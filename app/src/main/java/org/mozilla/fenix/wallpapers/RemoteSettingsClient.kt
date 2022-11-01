package org.mozilla.fenix.wallpapers

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import android.util.Log
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.isSuccess
import java.io.IOException

/**
 * Helper class to make it easier to interact with Kinto
 *
 * @property httpClient http client to use
 * @property baseUrl Kinto server url
 * @property bucketName name of the bucket to fetch
 * @property collectionName name of the collection to fetch
 * @property headers headers to provide along with the request
 */
internal class RemoteSettingsClient(
    val httpClient: Client,
    private val baseUrl: String = "http://10.0.2.2:8888",
    var bucketName: String,
    var collectionName: String = "",
    private val headers: Map<String, String>? = null
) {
    /**
     * Returns all records from the collection
     *
     * @return Kinto response with all records
     */
    fun get(): String {
        return fetch(recordsUrl())
    }

    /**
     * Performs a diff, given the last_modified time
     *
     * @param lastModified last modified time as a UNIX timestamp
     *
     * @return Kinto diff response
     */
    fun diff(lastModified: Long): String {
        return fetch("${recordsUrl()}?_since=$lastModified")
    }

    /**
     * Gets the collection associated metadata
     *
     * @return collection metadata
     */
    fun getMetadata(): String {
        return fetch(collectionUrl())
    }

    @Suppress("TooGenericExceptionCaught", "ThrowsCount")
    internal fun fetch(url: String): String {
        try {
            val headers = MutableHeaders().also {
                headers?.forEach { (k, v) -> it.append(k, v) }
            }

            val request = Request(url, headers = headers, useCaches = false)
            val response = httpClient.fetch(request)
            if (!response.isSuccess) {
                Log.i("tighe", "${response.status}")
                throw java.lang.IllegalStateException()
            }
            return response.body.string()
        } catch (e: IOException) {
            Log.i("tighe", "${e.message}")
            throw e
        } catch (e: ArrayIndexOutOfBoundsException) {
            // On some devices we are seeing an ArrayIndexOutOfBoundsException being thrown
            // somewhere inside AOSP/okhttp.
            // See: https://github.com/mozilla-mobile/android-components/issues/964
            Log.i("tighe", "${e.message}")
            throw e
        }
    }

    fun recordsUrl() = "${collectionUrl()}/records"
    fun collectionUrl() = "$baseUrl/v1/buckets/$bucketName/collections/$collectionName"
    fun attachmentsUrl() = "$baseUrl/attachments"
}
