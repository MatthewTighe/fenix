/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.mozilla.fenix.utils.Settings

/**
 * Provides access to available wallpapers and manages their states.
 */
class WallpaperManager(private val settings: Settings) {
    val wallpapers = Wallpaper.values()

    private var _currentWallpaper = MutableStateFlow(Wallpaper.valueOf(settings.currentWallpaper))
    var currentWallpaper: StateFlow<Wallpaper> = _currentWallpaper

    fun updateWallpaperSelection(wallpaper: Wallpaper) {
        settings.currentWallpaper = wallpaper.name
        _currentWallpaper.value = wallpaper
    }

    fun switchToNextWallpaper() {
        val current = _currentWallpaper.value
        val wallpapers = Wallpaper.values()
        val nextIndex = wallpapers.indexOf(current) + 1
        val nextWallpaper = if (nextIndex >= wallpapers.size) {
            wallpapers.first()
        } else {
            wallpapers[nextIndex]
        }
        updateWallpaperSelection(nextWallpaper)
    }
}
