package org.mozilla.fenix.wallpapers

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.Response
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.wallpapers.WallpaperMetadataFetcher.Companion.currentJsonVersion
import java.text.SimpleDateFormat
import java.util.*

@RunWith(AndroidJUnit4::class)
class WallpaperMetadataFetcherTest {

    private val expectedRequest = Request(
        url = BuildConfig.WALLPAPER_URL.substringBefore("android") +
            "/metadata/v$currentJsonVersion/wallpapers.json",
        method = Request.Method.GET
    )
    private val mockResponse = mockk<Response>()
    private val mockClient = mockk<Client> {
        every { fetch(expectedRequest) } returns mockResponse
    }

    private lateinit var metadataFetcher: WallpaperMetadataFetcher

    @Before
    fun setup() {
        metadataFetcher = WallpaperMetadataFetcher(mockClient)
    }

    @Test
    fun `GIVEN wallpaper metadata WHEN parsed THEN wallpapers have correct ids, text and card colors`() = runTest {
        val json = """
            {
                "last-updated-date": "2022-01-01",
                "collections": [
                    {
                        "id": "firefox",
                        "available-locales": null,
                        "availability-range": null,
                        "wallpapers": [
                            {
                                "id": "beachVibes",
                                "text-color": "FFFBFBFE",
                                "card-color": "FF15141A"
                            },
                            {
                                "id": "sunrise",
                                "text-color": "FF15141A",
                                "card-color": "FFFBFBFE"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        every { mockResponse.body } returns Response.Body(json.byteInputStream())

        val wallpapers = metadataFetcher.downloadWallpaperList()

        with (wallpapers[0]) {
            assertEquals(0xFFFBFBFE, textColor)
            assertEquals(0xFF15141A, cardColor)
        }
        with (wallpapers[1]) {
            assertEquals(0xFF15141A, textColor)
            assertEquals(0xFFFBFBFE, cardColor)
        }
    }

    @Test
    fun `GIVEN wallpaper metadata is missing an id WHEN parsed THEN parsing fails`() = runTest {
        val json = """
            {
                "last-updated-date": "2022-01-01",
                "collections": [
                    {
                        "id": "firefox",
                        "available-locales": null,
                        "availability-range": null,
                        "wallpapers": [
                            {
                                "text-color": "FFFBFBFE",
                                "card-color": "FF15141A"
                            },
                            {
                                "id": "sunrise",
                                "text-color": "FF15141A",
                                "card-color": "FFFBFBFE"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        every { mockResponse.body } returns Response.Body(json.byteInputStream())

        val wallpapers = metadataFetcher.downloadWallpaperList()

        assertTrue(wallpapers.isEmpty())
    }

    @Test
    fun `GIVEN wallpaper metadata is missing a text color WHEN parsed THEN parsing fails`() = runTest {
        val json = """
            {
                "last-updated-date": "2022-01-01",
                "collections": [
                    {
                        "id": "firefox",
                        "available-locales": null,
                        "availability-range": null,
                        "wallpapers": [
                            {
                                "id": "beachVibes",
                                "card-color": "FF15141A"
                            },
                            {
                                "id": "sunrise",
                                "text-color": "FF15141A",
                                "card-color": "FFFBFBFE"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        every { mockResponse.body } returns Response.Body(json.byteInputStream())

        val wallpapers = metadataFetcher.downloadWallpaperList()

        assertTrue(wallpapers.isEmpty())
    }

    @Test
    fun `GIVEN wallpaper metadata is missing a card color WHEN parsed THEN parsing fails`() = runTest {
        val json = """
            {
                "last-updated-date": "2022-01-01",
                "collections": [
                    {
                        "id": "firefox",
                        "available-locales": null,
                        "availability-range": null,
                        "wallpapers": [
                            {
                                "id": "beachVibes",
                                "text-color": "FFFBFBFE",
                            },
                            {
                                "id": "sunrise",
                                "text-color": "FF15141A",
                                "card-color": "FFFBFBFE"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        every { mockResponse.body } returns Response.Body(json.byteInputStream())

        val wallpapers = metadataFetcher.downloadWallpaperList()

        assertTrue(wallpapers.isEmpty())
    }

    @Test
    fun `GIVEN collection with specified locales WHEN parsed THEN wallpapers includes locales`() = runTest {
        val locales = listOf("en-US", "es-US", "en-CA", "fr-CA")
        val json = """
            {
                "last-updated-date": "2022-01-01",
                "collections": [
                    {
                        "id": "firefox",
                        "available-locales": ["en-US", "es-US", "en-CA", "fr-CA"],
                        "availability-range": null,
                        "wallpapers": [
                            {
                                "id": "beachVibes",
                                "text-color": "FFFBFBFE",
                                "card-color": "FF15141A"
                            },
                            {
                                "id": "sunrise",
                                "text-color": "FF15141A",
                                "card-color": "FFFBFBFE"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        every { mockResponse.body } returns Response.Body(json.byteInputStream())

        val wallpapers = metadataFetcher.downloadWallpaperList()

        assertTrue(wallpapers.all {
            it.availableLocales == locales
        })
    }

    @Test
    fun `GIVEN collection with specified date range WHEN parsed THEN wallpapers includes dates`() = runTest {
        val calendar = Calendar.getInstance()
        val startDate = calendar.run {
            set(2022, Calendar.JUNE, 27)
            time
        }
        val endDate = calendar.run {
            set(2022, Calendar.SEPTEMBER, 30)
            time
        }
        val json = """
            {
                "last-updated-date": "2022-01-01",
                "collections": [
                    {
                        "id": "firefox",
                        "available-locales": null,
                        "availability-range": {
                            "start": "2022-06-27",
                            "end": "2022-09-30"
                        },
                        "wallpapers": [
                            {
                                "id": "beachVibes",
                                "text-color": "FFFBFBFE",
                                "card-color": "FF15141A"
                            },
                            {
                                "id": "sunrise",
                                "text-color": "FF15141A",
                                "card-color": "FFFBFBFE"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        every { mockResponse.body } returns Response.Body(json.byteInputStream())

        val wallpapers = metadataFetcher.downloadWallpaperList()

        assertTrue(wallpapers.all {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            formatter.format(startDate) == formatter.format(it.startDate!!) &&
                    formatter.format(endDate) == formatter.format(it.endDate!!)
        })
    }
}