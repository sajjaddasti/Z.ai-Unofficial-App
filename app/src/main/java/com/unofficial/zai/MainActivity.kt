package com.unofficial.zai

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unofficial.zai.ui.navigation.AppNavHost
import com.unofficial.zai.ui.theme.ThemeMode
import com.unofficial.zai.ui.theme.ThemePrefs
import com.unofficial.zai.ui.theme.ZaiTheme
import com.unofficial.zai.ui.web.WebViewModel

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val activity = this

        setContent {
            var themeMode by remember { mutableStateOf(ThemePrefs.read(activity)) }
            val darkTheme = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            val view = LocalView.current
            SideEffect {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            ZaiTheme(darkTheme = darkTheme) {
                val vm: WebViewModel = viewModel()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(
                        vm = vm,
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            ThemePrefs.write(activity, mode)
                        }
                    )
                }
            }
        }
    }
}
