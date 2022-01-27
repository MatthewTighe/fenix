/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import android.graphics.Bitmap

/**
 * Represents a storage to store [Wallpaper]s.
 */
interface WallpaperStorage {
    /**
     * Returns all [Wallpaper] from the storage.
     */
    fun loadMetaData(): List<Wallpaper>

    fun loadBitmap(wallpaper: Wallpaper): Bitmap

    fun saveAsBitmap(wallpaper: Wallpaper)

    fun deleteWallpaper(wallpaper: Wallpaper)
}
