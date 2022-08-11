package org.mozilla.fenix.wallpapers

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mozilla.components.service.glean.testing.GleanTestRule
import mozilla.components.support.test.libstate.ext.waitUntilIdle
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.Wallpapers
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.utils.Settings
import java.util.Calendar
import java.util.Date

@RunWith(AndroidJUnit4::class)
class WallpapersUseCasesTest {

    @get:Rule
    val gleanTestRule = GleanTestRule(testContext)

    // initialize this once, so it can be shared throughout tests
    private val baseFakeDate = Date()
    private val fakeCalendar = Calendar.getInstance()

    private val appStore = AppStore()
    private val mockSettings = mockk<Settings>()
    private val mockDownloader = mockk<WallpaperDownloader>(relaxed = true)
    private val mockMetadataFetcher = mockk<WallpaperMetadataFetcher>()
    private val mockFileManager = mockk<WallpaperFileManager> {
        every { clean(any(), any()) } just runs
    }

    @Test
    fun `GIVEN wallpapers that expired WHEN invoking initialize use case THEN expired wallpapers are filtered out and cleaned up`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        val fakeExpiredRemoteWallpapers = listOf("expired").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.BEFORE, name)
        }
        val possibleWallpapers = fakeRemoteWallpapers + fakeExpiredRemoteWallpapers
        every { mockSettings.currentWallpaper } returns ""
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns possibleWallpapers
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            "en-US",
        ).invoke()

        val expectedFilteredWallpaper = fakeExpiredRemoteWallpapers[0]
        appStore.waitUntilIdle()
        assertFalse(appStore.state.wallpaperState.availableWallpapers.contains(expectedFilteredWallpaper))
        verify { mockFileManager.clean(Wallpaper.Default, any()) }
    }

    @Test
    fun `GIVEN wallpapers that expired and an expired one is selected WHEN invoking initialize use case THEN selected wallpaper is not filtered out`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        val expiredWallpaper = makeFakeRemoteWallpaper(TimeRelation.BEFORE, "expired")
        val possibleWallpapers = fakeRemoteWallpapers + listOf(expiredWallpaper)
        every { mockSettings.currentWallpaper } returns expiredWallpaper.id
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns possibleWallpapers

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            "en-US",
        ).invoke()

        appStore.waitUntilIdle()
        assertTrue(appStore.state.wallpaperState.availableWallpapers.contains(expiredWallpaper))
        assertEquals(expiredWallpaper, appStore.state.wallpaperState.currentWallpaper)
    }

    @Test
    fun `GIVEN wallpapers that are in promotions outside of locale WHEN invoking initialize use case THEN promotional wallpapers are filtered out`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        val locale = "en-CA"
        every { mockSettings.currentWallpaper } returns ""
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns fakeRemoteWallpapers

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            locale,
        ).invoke()

        appStore.waitUntilIdle()
        assertTrue(appStore.state.wallpaperState.availableWallpapers.isEmpty())
    }

    @Test
    fun `GIVEN available wallpapers WHEN invoking initialize use case THEN available wallpapers downloaded`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        every { mockSettings.currentWallpaper } returns ""
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns fakeRemoteWallpapers

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            "en-US",
        ).invoke()

        for (fakeRemoteWallpaper in fakeRemoteWallpapers) {
            coVerify { mockDownloader.downloadWallpaper(fakeRemoteWallpaper) }
        }
    }

    @Test
    fun `GIVEN a wallpaper has not been selected WHEN invoking initialize use case THEN store contains default`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        every { mockSettings.currentWallpaper } returns ""
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns fakeRemoteWallpapers

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            "en-US",
        ).invoke()

        appStore.waitUntilIdle()
        assertTrue(appStore.state.wallpaperState.currentWallpaper == Wallpaper.Default)
    }

    @Test
    fun `GIVEN a wallpaper is selected and there are available wallpapers WHEN invoking initialize use case THEN these are dispatched to the store`() = runTest {
        val selectedWallpaper = makeFakeRemoteWallpaper(TimeRelation.LATER, "selected")
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        val possibleWallpapers = listOf(selectedWallpaper) + fakeRemoteWallpapers
        every { mockSettings.currentWallpaper } returns selectedWallpaper.id
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns possibleWallpapers

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            "en-US",
        ).invoke()

        appStore.waitUntilIdle()
        assertEquals(selectedWallpaper, appStore.state.wallpaperState.currentWallpaper)
        assertEquals(possibleWallpapers, appStore.state.wallpaperState.availableWallpapers)
    }

    @Test
    fun `WHEN selected wallpaper usecase invoked THEN storage updated and store receives dispatch`() {
        val selectedWallpaper = makeFakeRemoteWallpaper(TimeRelation.LATER, "selected")
        val slot = slot<String>()
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        every { mockSettings.currentWallpaper } returns ""
        every { mockSettings.currentWallpaper = capture(slot) } just runs

        WallpapersUseCases.DefaultSelectWallpaperUseCase(
            mockSettings,
            appStore
        ).invoke(selectedWallpaper)

        appStore.waitUntilIdle()
        assertEquals(selectedWallpaper.id, slot.captured)
        assertEquals(selectedWallpaper, appStore.state.wallpaperState.currentWallpaper)
        assertEquals(selectedWallpaper.id, Wallpapers.wallpaperSelected.testGetValue()?.first()?.extra?.get("name")!!)
    }

    private enum class TimeRelation {
        BEFORE,
        NOW,
        LATER,
    }

    /**
     * [timeRelation] should specify a time relative to the time the tests are run
     */
    private fun makeFakeRemoteWallpaper(
        timeRelation: TimeRelation,
        name: String = "name",
        isInPromo: Boolean = true
    ): Wallpaper {
        fakeCalendar.time = baseFakeDate
        when (timeRelation) {
            TimeRelation.BEFORE -> fakeCalendar.add(Calendar.DATE, -5)
            TimeRelation.NOW -> Unit
            TimeRelation.LATER -> fakeCalendar.add(Calendar.DATE, 5)
        }
        val relativeTime = fakeCalendar.time
        return if (isInPromo) {
            generateWallpaper(id = name, endDate = relativeTime)
        } else {
            generateWallpaper(id = name, endDate = relativeTime)
        }
    }

    private fun generateWallpaper(
        id: String = "",
        collectionId: String = "",
        textColor: Long = 0,
        cardCard: Long = 0,
        availableLocales: List<String>? = listOf("en-US"),
        startDate: Date? = null,
        endDate: Date? = null,
    ) = Wallpaper(id, collectionId, textColor, cardCard, availableLocales, startDate, endDate)
}
