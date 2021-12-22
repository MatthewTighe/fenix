/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.FragmentWallpaperSettingsBinding
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.wallpapers.Wallpaper
import org.mozilla.fenix.wallpapers.WallpaperManager

class WallpaperSettingsFragment : Fragment() {
    private val binding by lazy {
        FragmentWallpaperSettingsBinding.inflate(layoutInflater)
    }

    private val wallpaperManager by lazy {
        requireComponents.wallpaperManager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FirefoxTheme {
                    WallpaperSettings(wallpaperManager)
                }
            }
        }
        return binding.root
    }
}

@Composable
fun WallpaperSettings(
    wallpaperManager: WallpaperManager
) {
    val currentlySelectedWallpaper = wallpaperManager.currentWallpaper.collectAsState()
    Column {
        Surface {
            WallpaperThumbnails(
                wallpapers = wallpaperManager.wallpapers.toList(),
                onSelectionChanged = { wallpaperManager.updateWallpaperSelection(it) },
                selectedWallpaper = currentlySelectedWallpaper.value
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallpaperThumbnails(
    wallpapers: List<Wallpaper>,
    onSelectionChanged: (Wallpaper) -> Unit,
    selectedWallpaper: Wallpaper
) {
    LazyVerticalGrid(
        cells = GridCells.Fixed(3),
        modifier = Modifier.padding(vertical = 30.dp, horizontal = 20.dp)
    ) {
        items(wallpapers) { wallpaper ->
            WallpaperThumbnailItem(
                wallpaper = wallpaper,
                onSelectionChanged = onSelectionChanged,
                isCurrentlySelected = selectedWallpaper == wallpaper
            )
        }
    }
}

@Composable
fun WallpaperThumbnailItem(
    wallpaper: Wallpaper,
    onSelectionChanged: (Wallpaper) -> Unit,
    isCurrentlySelected: Boolean
) {
    val thumbnailShape = RoundedCornerShape(8.dp)
    val border = if (isCurrentlySelected) {
        Modifier.border(
            BorderStroke(width = 2.dp, color = FirefoxTheme.colors.borderSelected),
            thumbnailShape
        )
    } else {
        Modifier.border(
            BorderStroke(width = 1.dp, color = FirefoxTheme.colors.borderDivider),
            thumbnailShape
        )
    }
    val background = if (wallpaper == Wallpaper.NONE) {
        Modifier.background(color = FirefoxTheme.colors.layer1)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .padding(4.dp)
            .clip(thumbnailShape)
            .then(background)
            .then(border)
            .clickable { onSelectionChanged(wallpaper) }
    ) {
        if (wallpaper != Wallpaper.NONE) {
            WallpaperImageThumbnail(
                wallpaper = wallpaper,
            )
        }
    }
}

@Composable
fun WallpaperImageThumbnail(
    wallpaper: Wallpaper,
) {
    val localizedContentDescription = LocalContext.current.resources.getString(
        R.string.content_description_wallpaper_name, wallpaper.name
    )
    Image(
        painterResource(id = wallpaper.resource),
        contentScale = ContentScale.FillBounds,
        contentDescription = localizedContentDescription,
        modifier = Modifier.fillMaxSize()
    )
}

@Preview
@Composable
fun WallpaperThumbnailsPreview() {
    WallpaperThumbnails(
        wallpapers = Wallpaper.values().toList(),
        onSelectionChanged = {},
        selectedWallpaper = Wallpaper.NONE
    )
}