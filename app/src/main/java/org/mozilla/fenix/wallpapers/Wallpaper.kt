/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import android.view.View
import mozilla.components.support.ktx.android.content.getColorFromAttr
import org.mozilla.fenix.R

/**
 * A enum that represents the available wallpapers and their states.
 */
enum class Wallpaper(val resource: Int) {
    NONE(R.attr.homeBackground),
    FIRST(R.drawable.wallpaper_1),
    SECOND(R.drawable.wallpaper_2),
    THIRD(R.drawable.wallpaper_1);

    fun applyToView(view: View) {
        if (this == NONE) {
            view.setBackgroundColor(view.context.getColorFromAttr(resource))
        } else {
            view.setBackgroundResource(resource)
        }
    }
}
