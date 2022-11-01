package org.mozilla.fenix.wallpapers

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class WallpaperJsonParser {
    fun parseAsWallpapers(json: String): List<Wallpaper> = JSONObject(json).getJSONArray("data").getJSONObject(0).toCollectionOfWallpapers()

    private fun JSONObject.toCollectionOfWallpapers(): List<Wallpaper> {
        val collectionId = getString("name")
        val heading = optStringOrNull("heading")
        val description = optStringOrNull("description")
        val availableLocales = optJSONArray("available-locales")?.getAvailableLocales()
        val availabilityRange = optJSONObject("available-dates")?.getAvailabilityRange()
        val learnMoreUrl = optStringOrNull("learn-more-url")
        val collection = Wallpaper.Collection(
            name = collectionId,
            heading = heading,
            description = description,
            availableLocales = availableLocales,
            startDate = availabilityRange?.first,
            endDate = availabilityRange?.second,
            learnMoreUrl = learnMoreUrl,
        )
        return getJSONArray("colorways").toWallpaperList(collection)
    }

    private fun JSONArray.getAvailableLocales(): List<String>? =
        (0 until length()).map { getString(it) }

    private fun JSONObject.getAvailabilityRange(): Pair<Date, Date>? {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return Result.runCatching {
            formatter.parse(getString("start"))!! to formatter.parse(getString("end"))!!
        }.getOrNull()
    }

    private fun JSONArray.toWallpaperList(collection: Wallpaper.Collection): List<Wallpaper> =
        (0 until length()).map { index ->
            with(getString(index)) {
                Wallpaper(
                    name = this,
                    textColor = 0,
                    cardColorLight = 0,
                    cardColorDark = 0,
                    collection = collection,
                    thumbnailFileState = Wallpaper.ImageFileState.Unavailable,
                    assetsFileState = Wallpaper.ImageFileState.Unavailable,
                )
            }
        }

    /**
     * Normally, if a field is specified in json as null, then optString will return it as "null". If
     * a field is missing completely, optString will return "". This will correctly return null in
     * both those cases so that optional properties are marked as missing.
     */
    private fun JSONObject.optStringOrNull(propName: String) = optString(propName).takeIf {
        it != "null" && it.isNotEmpty()
    }

    /**
     * The wallpaper metadata has 6 digit hex color codes for compatibility with iOS. Since Android
     * expects 8 digit ARBG values, we prepend FF for the "fully visible" version of the color
     * listed in the metadata.
     */
    private fun JSONObject.getArgbValueAsLong(propName: String): Long = "FF${getString(propName)}"
        .toLong(radix = 16)
}
