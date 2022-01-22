/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import mozilla.components.support.base.log.logger.Logger
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.lang.Exception

class WallpapersAssetsStorage(private val context: Context) : WallpaperStorage {
    val logger = Logger("WallpapersAssetsStorage")
    private val wallpapersDirectory = "wallpapers"

    @Suppress("TooGenericExceptionCaught")
    override fun loadAvailableMetaData(): List<Wallpaper> {
        val assetsManager = context.assets
        return try {
            assetsManager.readArray("$wallpapersDirectory/wallpapers.json").toWallpapers()
        } catch (e: Exception) {
            logger.error("Unable to load wallpaper", e)
            emptyList()
        }
    }

    override fun loadBitmap(wallpaper: Wallpaper): Bitmap {
        val path = context.getWallpaperName(wallpaper)
        val file = File(context.filesDir, path)
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    override fun saveAsBitmap(wallpaper: Wallpaper) {
        if (wallpaper == WallpaperManager.defaultWallpaper) return
        val portraitOutput = context.openFileOutput(wallpaper.portraitName, Context.MODE_PRIVATE)
        context.assets.open(wallpaper.portraitPath).copyTo(portraitOutput)
        val landscapeOutput = context.openFileOutput(wallpaper.landscapeName, Context.MODE_PRIVATE)
        context.assets.open(wallpaper.landscapePath).copyTo(landscapeOutput)
    }

    private fun JSONArray.toWallpapers(): List<Wallpaper> {
        return (0 until this.length()).mapNotNull { index ->
            this.getJSONObject(index).toWallpaper()
        }
    }

    private fun JSONObject.toWallpaper(): Wallpaper? {
        return try {
            Wallpaper(
                name = getString("name"),
                portraitPath = getString("portrait"),
                landscapePath = getString("landscape"),
                isDark = getBoolean("isDark")
            )
        } catch (e: JSONException) {
            logger.error("unable to parse json for wallpaper $this", e)
            null
        }
    }

    private fun AssetManager.readArray(fileName: String) = JSONArray(
        open(fileName).bufferedReader().use {
            it.readText()
        }
    )

    private fun Context.getWallpaperName(wallpaper: Wallpaper) =
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            wallpaper.landscapeName
        } else {
            wallpaper.portraitName
        }

    private val Wallpaper.landscapeName get() = "landscape-${this.name}"
    private val Wallpaper.portraitName get() = "portrait-${this.name}"
}
