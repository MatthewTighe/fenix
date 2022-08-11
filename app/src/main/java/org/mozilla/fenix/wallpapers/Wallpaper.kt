/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import androidx.annotation.DrawableRes
import java.util.Calendar
import java.util.Date

/**
 * Type hierarchy defining the various wallpapers that are available as home screen backgrounds.
 * @property id The name of the wallpaper.
 */
data class Wallpaper(
    val id: String,
    val collectionId: String,
    val textColor: Long,
    val cardColor: Long,
    val availableLocales: List<String>?,
    val startDate: Date?,
    val endDate: Date?
) {

    companion object {
        val Default = Wallpaper(
            id = "default",
            collectionId = "default",
            textColor = 0,
            cardColor = 0,
            availableLocales = null,
            startDate = null,
            endDate = null,
        )

        /**
         * Defines the standard path at which a wallpaper resource is kept on disk.
         *
         * @param orientation One of landscape/portrait.
         * @param theme One of dark/light.
         * @param name The name of the wallpaper.
         */
        fun getBaseLocalPath(orientation: String, theme: String, name: String): String =
            "wallpapers/$orientation/$theme/$name.png"
    }
}
