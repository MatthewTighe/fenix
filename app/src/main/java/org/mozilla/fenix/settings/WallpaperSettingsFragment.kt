/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
                    WallpaperSettings(wallpapers = Wallpaper.values().toList().dropLast(1))
                }
            }
        }
        return binding.root
    }
}

enum class Wallpaper(val drawable: Int) {
    FIRST(R.drawable.wallpaper_1),
    SECOND(R.drawable.wallpaper_2),
    NONE(R.attr.homeBackground);
}

@Composable
fun WallpaperSettings(wallpapers: List<Wallpaper>) {
    var currentlySelectedWallpaper by remember { mutableStateOf(wallpapers[0]) }
    Surface {
        Column {
            for (wallpaperRow in wallpapers.chunked(3)) {
                Row{
                    for (wallpaper in wallpaperRow) {
                        val thumbnailShape = RoundedCornerShape(8.dp)
                        val border = if (currentlySelectedWallpaper == wallpaper) {
                            Modifier.border(BorderStroke(2.dp, FirefoxTheme.colors.borderSelected), thumbnailShape)
                        } else {
                            Modifier
                        }
                        Box(
                            modifier = Modifier
                                .height(102.dp)
                                .width(92.dp)
                                .padding(4.dp)
                                .clip(thumbnailShape)
                                .then(border)
                        ) {
                            Image(
                                painterResource(id = wallpaper.drawable),
                                contentScale = ContentScale.FillBounds,
                                contentDescription = wallpaper.name,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth()
                                    .clickable {
                                        currentlySelectedWallpaper = wallpaper
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun WallpaperSettingsPreview() {
    WallpaperSettings(wallpapers = Wallpaper.values().toList().dropLast(1))
}