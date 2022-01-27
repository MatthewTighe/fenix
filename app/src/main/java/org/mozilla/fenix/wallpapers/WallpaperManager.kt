/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.content.getColorFromAttr
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.asActivity
import org.mozilla.fenix.utils.Settings

/**
 * Provides access to available wallpapers and manages their states.
 */
class WallpaperManager(
    private val settings: Settings,
    private val wallpaperStorage: WallpaperStorage
) {
    val logger = Logger("WallpaperManager")
    var availableWallpapers: List<Wallpaper> = loadWallpapers()
        private set

    var currentWallpaper: Wallpaper = getCurrentWallpaperFromSettings()
        set(value) {
            val previousWallpaper = field
            wallpaperStorage.deleteWallpaper(previousWallpaper)
            settings.currentWallpaper = value.name
            wallpaperStorage.saveAsBitmap(value)
            field = value
        }

    /**
     * Apply [currentWallpaper] to the [wallpaperContainer].
     */
    fun applyCurrentWallpaperToView(wallpaperContainer: View) {
        val wallpaper = currentWallpaper
        applyWallpaperToView(wallpaperContainer, wallpaper)
    }

    private fun applyWallpaperToView(wallpaperContainer: View, wallpaper: Wallpaper) {
        val context = wallpaperContainer.context
        if (wallpaper == defaultWallpaper) {
            wallpaperContainer.setBackgroundColor(context.getColorFromAttr(DEFAULT_RESOURCE))
            logger.info("Wallpaper update to default background")
        } else {
            val bitmap = if (wallpaper in availableWallpapers) {
                loadWallpaperFromAssets(wallpaper, context)
            } else {
                wallpaperStorage.loadBitmap(wallpaper)
            }
            wallpaperContainer.background = BitmapDrawable(context.resources, bitmap)
            logger.info("Wallpaper update to ${wallpaper.name}")
        }

        adjustTheme(wallpaperContainer.context)
    }

    private fun adjustTheme(context: Context) {
        val mode = if (currentWallpaper != defaultWallpaper) {
            if (currentWallpaper.isDark) {
                updateThemePreference(useDarkTheme = true)
                logger.info("theme changed to useDarkTheme")
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                logger.info("theme changed to useLightTheme")
                updateThemePreference(useLightTheme = true)
                AppCompatDelegate.MODE_NIGHT_NO
            }
        } else {
            // For the default wallpaper, there is not need to adjust the theme,
            // as we want to allow users decide which theme they want to have.
            // The default wallpaper adapts to whichever theme the user has.
            return
        }

        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
            logger.info("theme updated activity recreated")
            context.asActivity()?.recreate()
        }
    }

    private fun updateThemePreference(
        useDarkTheme: Boolean = false,
        useLightTheme: Boolean = false
    ) {
        settings.shouldUseDarkTheme = useDarkTheme
        settings.shouldUseLightTheme = useLightTheme
        settings.shouldFollowDeviceTheme = false
    }

    /**
     * Switch the current wallpaper to the next in [availableWallpapers] and apply it to
     * [wallpaperContainer].
     */
    fun applyNextWallpaperToView(wallpaperContainer: View) {
        val values = availableWallpapers
        val index = values.indexOf(currentWallpaper) + 1

        val nextWallpaper = if (index >= values.size) {
            values.first()
        } else {
            values[index]
        }
        currentWallpaper = nextWallpaper
        applyWallpaperToView(wallpaperContainer, nextWallpaper)
    }

    private fun getCurrentWallpaperFromSettings(): Wallpaper {
        val currentWallpaper = settings.currentWallpaper
        return if (currentWallpaper.isEmpty()) {
            defaultWallpaper
        } else {
            availableWallpapers.find { it.name == currentWallpaper } ?: defaultWallpaper
        }
    }

    fun loadWallpaperFromAssets(wallpaper: Wallpaper, context: Context): Bitmap {
        val path = if (isLandscape(context)) {
            wallpaper.landscapePath
        } else {
            wallpaper.portraitPath
        }
        return context.assets.open(path).use {
            BitmapFactory.decodeStream(it)
        }
    }

    private fun isLandscape(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private fun loadWallpapers(): List<Wallpaper> {
        val wallpapersFromStorage = wallpaperStorage.loadMetaData()
        return if (wallpapersFromStorage.isNotEmpty()) {
            listOf(defaultWallpaper) + wallpapersFromStorage
        } else {
            listOf(defaultWallpaper)
        }
    }

    companion object {
        const val DEFAULT_RESOURCE = R.attr.homeBackground
        val defaultWallpaper = Wallpaper(
            name = "default_wallpaper",
            portraitPath = "",
            landscapePath = "",
            isDark = false
        )
    }
}
