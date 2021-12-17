package org.mozilla.fenix.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Text
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
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
                    Text("hey")
                }
            }
        }
        return binding.root
    }
}