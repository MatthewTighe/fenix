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
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.FragmentWallpaperSettingsBinding
import org.mozilla.fenix.theme.FirefoxTheme

class WallpaperSettingsFragment : Fragment() {
    private val binding by lazy {
        FragmentWallpaperSettingsBinding.inflate(layoutInflater)
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
                    WallpaperSettings(wallpapers = Wallpaper.values().toList())
                }
            }
        }
        return binding.root
    }
}

enum class Wallpaper(val drawable: Int) {
    NONE(R.attr.homeBackground),
    FIRST(R.drawable.wallpaper_1),
    SECOND(R.drawable.wallpaper_2),
    THIRD(R.drawable.wallpaper_1);
}

@Composable
fun WallpaperSettings(wallpapers: List<Wallpaper>) {
    var currentlySelectedWallpaper by remember { mutableStateOf(wallpapers[0]) }
    Column {
        Surface {
            WallpaperThumbnails(
                wallpapers = wallpapers,
                onSelectionChanged = { currentlySelectedWallpaper = it },
                selectedWallpaper = currentlySelectedWallpaper
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            var wallpapersEnabled by remember { mutableStateOf(false) }
            Text("Logo wallpaper switch")
            Switch(checked = wallpapersEnabled, onCheckedChange = { wallpapersEnabled = it })
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
    Image(
        painterResource(id = wallpaper.drawable),
        contentScale = ContentScale.FillBounds,
        contentDescription = wallpaper.name,
    )
}

@Preview
@Composable
fun WallpaperSettingsPreview() {
    WallpaperSettings(wallpapers = Wallpaper.values().toList())
}