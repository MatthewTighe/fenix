package org.mozilla.fenix.wallpapers

import android.util.Log
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.isSuccess
import mozilla.components.support.ktx.android.org.json.mapNotNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.IllegalStateException

internal class RemoteSettingsDownloader(
    private val rsClient: RemoteSettingsClient,
    private val client: Client,
    private val storageRootDirectory: File,
) {
    fun download(wallpaper: Wallpaper, fileType: Wallpaper.ImageType): Wallpaper.ImageFileState {
        val attachmentLocation = JSONObject(rsClient.get())
            .getJSONArray("data")
            .mapNotNull(JSONArray::toListOfObjects) {
                it.takeIf {
                    Log.i("tighe", it.getString("platform"))
                    Log.i("tighe", it.getString("name"))
                    it.getString("platform").lowercase() == "android" &&
                            it.getString("name").lowercase() == wallpaper.name.lowercase()
                }
            }
            .find {
                it.getString("type").lowercase() == fileType.name.lowercase()
            }?.getJSONObject("attachment")?.getString("location") ?: run {
                Log.i("tighe", "location not found")
                return Wallpaper.ImageFileState.Error
            }
        val request = Request(
            url = "${rsClient.attachmentsUrl()}/$attachmentLocation",
            method = Request.Method.GET,
        )
        val localFile = File(storageRootDirectory,
            Wallpaper.getLocalPath(wallpaper.name, fileType))
        val response = client.fetch(request)
        if (!response.isSuccess) {
            throw IllegalStateException()
        }
        localFile.parentFile?.mkdirs()
        response.body.useStream { input ->
            input.copyTo(localFile.outputStream())
        }
        return Wallpaper.ImageFileState.Downloaded
    }
}

private fun JSONArray.toListOfObjects(idx: Int): JSONObject =
    this.getJSONObject(idx)

