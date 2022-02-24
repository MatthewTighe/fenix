package org.mozilla.fenix.wallpapers

import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor

const val nightlyUrl = "https://app.adjust.com/z7hvyfj?campaign=wallpaper&adgroup=websitemodal&creative=android&redirect=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Dorg.mozilla.fenix&redirect_macos=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Dorg.mozilla.fenix&deep_link=fenix-nightly%3A%2F%2Fsettings_wallpapers"
const val betaUrl = "https://app.adjust.com/lqbz7i2?campaign=wallpaper&adgroup=websitemodal&creative=android&redirect=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Dorg.mozilla.firefox_beta&redirect_macos=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Dorg.mozilla.firefox_beta&deep_link=fenix-beta%3A%2F%2Fsettings_wallpapers"
const val releaseUrl = "https://app.adjust.com/t13wxna?campaign=wallpaper&adgroup=websitemodal&creative=android&redirect=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Dorg.mozilla.firefox&redirect_macos=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Dorg.mozilla.firefox&deep_link=fenix%3A%2F%2Fsettings_wallpapers"

class AdjustLinkInterceptor(
    private val navToWallpaperSettings: () -> Unit,
) : RequestInterceptor {
    override fun onLoadRequest(
        engineSession: EngineSession,
        uri: String,
        lastUri: String?,
        hasUserGesture: Boolean,
        isSameDomain: Boolean,
        isRedirect: Boolean,
        isDirectNavigation: Boolean,
        isSubframeRequest: Boolean
    ): RequestInterceptor.InterceptionResponse? {
        return if (uri in listOf(nightlyUrl, betaUrl, releaseUrl)) {
            navToWallpaperSettings()
            RequestInterceptor.InterceptionResponse.Deny
        }
        else null
//       return if (uri == interceptUrl) {
//            RequestInterceptor.InterceptionResponse.AppIntent(
//                Intent.parseUri("fenix://settings_wallpapers", 0),
//                "fenix://settings_wallpapers",
//            )
//        } else null
    }
}