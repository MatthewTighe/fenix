/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.appstate

import org.mozilla.fenix.components.AppStore

/**
 * Reducer for [AppStore].
 */
internal object AppStoreReducer {
    fun reduce(state: AppState, action: AppAction): AppState = when (action) {
        is AppAction.InitAction -> state
        is AppAction.UpdateInactiveExpanded ->
            state.copy(inactiveTabsExpanded = action.expanded)
        is AppAction.SwitchToNextWallpaper ->
            state.copy(wallpaper = state.wallpaper.nextWallpaper)
        is AppAction.UpdateWallpaper ->
            state.copy(wallpaper = action.wallpaper)
    }
}
