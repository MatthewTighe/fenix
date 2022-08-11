/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Request
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.fenix.BuildConfig
import java.text.SimpleDateFormat
import java.util.*

class WallpaperMetadataFetcher(
    private val client: Client
) {
    private val metadataUrl = BuildConfig.WALLPAPER_URL.substringBefore("android") +
            "/metadata/v$currentJsonVersion/wallpapers.json"

    suspend fun downloadWallpaperList(): List<Wallpaper> = withContext(Dispatchers.IO) {
        Result.runCatching {
            val request = Request(url = metadataUrl, method = Request.Method.GET)
            val response = client.fetch(request)
            response.body.useBufferedReader {
                val json = it.readText()
                JSONObject(json).parseAsWallpapers()
            }
        }.getOrElse { listOf() }
    }

    private fun JSONObject.parseAsWallpapers(): List<Wallpaper> = with(getJSONArray("collections")) {
        (0 until length()).map { index ->
            getJSONObject(index).toCollectionOfWallpapers()
        }.flatten()
    }

    private fun JSONObject.toCollectionOfWallpapers(): List<Wallpaper> {
        val collectionId = getString("id")
        val availableLocales = optJSONArray("available-locales")?.getAvailableLocales()
        val availabilityRange = optJSONObject("availability-range")?.getAvailabilityRange()
        return getJSONArray("wallpapers").toWallpaperList(collectionId, availableLocales, availabilityRange)
    }

    private fun JSONArray.getAvailableLocales(): List<String>? =
            (0 until length()).map { getString(it) }

    private fun JSONObject.getAvailabilityRange(): Pair<Date, Date>? {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.parse(getString("start"))!! to formatter.parse(getString("end"))!!
    }

    private fun JSONArray.toWallpaperList(collectionId: String, availableLocales: List<String>?, availabilityRange: Pair<Date, Date>?): List<Wallpaper> =
        (0 until length()).map { index ->
            with(getJSONObject(index)) {
                Wallpaper(
                    id = getString("id"),
                    textColor = getString("text-color").toLong(radix = 16),
                    cardColor = getString("card-color").toLong(radix = 16),
                    collectionId = collectionId,
                    availableLocales = availableLocales ?: listOf(),
                    startDate = availabilityRange?.first,
                    endDate = availabilityRange?.second
                )
            }
        }

    companion object {
        internal const val currentJsonVersion = 1
    }
}