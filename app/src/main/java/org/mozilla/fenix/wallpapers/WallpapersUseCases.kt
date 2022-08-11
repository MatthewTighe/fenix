/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.StrictMode
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.Client
import mozilla.components.support.locale.LocaleManager
import org.mozilla.fenix.GleanMetrics.Wallpapers
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.perf.StrictModeManager
import org.mozilla.fenix.utils.Settings
import java.io.File
import java.util.Date

/**
 * Contains use cases related to the wallpaper feature.
 *
 * @param context Used for various file and configuration checks.
 * @param store Will receive dispatches of metadata updates like the currently selected wallpaper.
 * @param client Handles downloading wallpapers and their metadata.
 * @param strictMode Required for determining some device state like current locale and file paths.
 *
 * @property initialize Usecase for initializing wallpaper feature. Should usually be called early
 * in the app's lifetime to ensure that any potential long-running tasks can complete quickly.
 * @property loadBitmap Usecase for loading specific wallpaper bitmaps.
 * @property selectWallpaper Usecase for selecting a new wallpaper.
 */
class WallpapersUseCases(
    context: Context,
    store: AppStore,
    client: Client,
    strictMode: StrictModeManager
) {
    val initialize: InitializeWallpapersUseCase by lazy {
        // Required to even access context.filesDir property and to retrieve current locale
        val (fileManager, currentLocale) = strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
            val fileManager = WallpaperFileManager(context.filesDir)
            val currentLocale = LocaleManager.getCurrentLocale(context)?.toLanguageTag()
                ?: LocaleManager.getSystemDefault().toLanguageTag()
            fileManager to currentLocale
        }
        val downloader = WallpaperDownloader(context, client)
        val metadataFetcher = WallpaperMetadataFetcher(client)
        DefaultInitializeWallpaperUseCase(
            store = store,
            downloader = downloader,
            fileManager = fileManager,
            metadataFetcher = metadataFetcher,
            settings = context.settings(),
            currentLocale = currentLocale
        )
    }
    val loadBitmap: LoadBitmapUseCase by lazy { DefaultLoadBitmapUseCase(context) }
    val selectWallpaper: SelectWallpaperUseCase by lazy { DefaultSelectWallpaperUseCase(context.settings(), store) }

    /**
     * Contract for usecases that initialize the wallpaper feature.
     */
    interface InitializeWallpapersUseCase {
        /**
         * Start operations that should be down during initialization, like remote metadata
         * retrieval and determining the currently selected wallpaper.
         */
        suspend operator fun invoke()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal class DefaultInitializeWallpaperUseCase(
        private val store: AppStore,
        private val downloader: WallpaperDownloader,
        private val fileManager: WallpaperFileManager,
        private val metadataFetcher: WallpaperMetadataFetcher,
        private val settings: Settings,
        private val currentLocale: String,
    ) : InitializeWallpapersUseCase {

        /**
         * Downloads the currently available wallpaper metadata from a remote source.
         * Updates the [store] with that metadata and with the selected wallpaper found in storage.
         * Removes any unused promotional or time-limited assets from local storage.
         * Should usually be called early the app's lifetime to ensure that metadata and thumbnails
         * are available as soon as they are needed.
         */
        override suspend operator fun invoke() {
            // Quite a bit of code needs to be executed off the main thread in some of this setup.
            // This should be cleaned up as improvements are made to the storage, file management,
            // and download utilities.
            withContext(Dispatchers.IO) {
                val currentWallpaperName = settings.currentWallpaper
                val possibleWallpapers = metadataFetcher.downloadWallpaperList().filter {
                    !it.isExpired() && it.isAvailableInLocale()
                }
                val currentWallpaper = possibleWallpapers.find { it.id == currentWallpaperName }
                    ?: fileManager.lookupExpiredWallpaper(currentWallpaperName)
                    ?: Wallpaper.Default

                fileManager.clean(
                    currentWallpaper,
                    possibleWallpapers
                )
                downloadAllRemoteWallpapers(possibleWallpapers)

                store.dispatch(AppAction.WallpaperAction.UpdateAvailableWallpapers(possibleWallpapers))
                store.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(currentWallpaper))
            }
        }


        private suspend fun downloadAllRemoteWallpapers(allWallpapers: List<Wallpaper>) {
            for (wallpaper in allWallpapers) {
                downloader.downloadWallpaper(wallpaper)
            }
        }

        private fun Wallpaper.isExpired(): Boolean = when (this) {
            Wallpaper.Default -> false
            else -> {
                val expired = this.endDate?.let { Date().after(it) } ?: false
                expired && this.id != settings.currentWallpaper
            }
        }

        private fun Wallpaper.isAvailableInLocale(): Boolean =
            this.availableLocales?.contains(currentLocale) ?: false
    }

    /**
     * Contract for usecase for loading bitmaps related to a specific wallpaper.
     */
    interface LoadBitmapUseCase {
        /**
         * Load the bitmap for a [wallpaper], if available.
         *
         * @param wallpaper The wallpaper to load a bitmap for.
         */
        suspend operator fun invoke(wallpaper: Wallpaper): Bitmap?
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal class DefaultLoadBitmapUseCase(private val context: Context) : LoadBitmapUseCase {
        /**
         * Load the bitmap for a [wallpaper], if available.
         *
         * @param wallpaper The wallpaper to load a bitmap for.
         */
        override suspend operator fun invoke(wallpaper: Wallpaper): Bitmap? =
            loadWallpaperFromDisk(context, wallpaper)

        private suspend fun loadWallpaperFromDisk(
            context: Context,
            wallpaper: Wallpaper
        ): Bitmap? = Result.runCatching {
            val path = wallpaper.getLocalPathFromContext(context)
            withContext(Dispatchers.IO) {
                val file = File(context.filesDir, path)
                BitmapFactory.decodeStream(file.inputStream())
            }
        }.getOrNull()

        /**
         * Get the expected local path on disk for a wallpaper. This will differ depending
         * on orientation and app theme.
         */
        private fun Wallpaper.getLocalPathFromContext(context: Context): String {
            val orientation = if (context.isLandscape()) "landscape" else "portrait"
            val theme = if (context.isDark()) "dark" else "light"
            return Wallpaper.getBaseLocalPath(orientation, theme, id)
        }

        private fun Context.isLandscape(): Boolean {
            return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

        private fun Context.isDark(): Boolean {
            return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
    }

    /**
     * Contract for usecase of selecting a new wallpaper.
     */
    interface SelectWallpaperUseCase {
        /**
         * Select a new wallpaper.
         *
         * @param wallpaper The selected wallpaper.
         */
        operator fun invoke(wallpaper: Wallpaper)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal class DefaultSelectWallpaperUseCase(
        private val settings: Settings,
        private val store: AppStore,
    ) : SelectWallpaperUseCase {
        /**
         * Select a new wallpaper. Storage and the store will be updated appropriately.
         *
         * @param wallpaper The selected wallpaper.
         */
        override fun invoke(wallpaper: Wallpaper) {
            settings.currentWallpaper = wallpaper.id
            store.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(wallpaper))
            Wallpapers.wallpaperSelected.record(
                Wallpapers.WallpaperSelectedExtra(
                    name = wallpaper.id,
                    themeCollection = wallpaper.collectionId
                )
            )
        }
    }
}
