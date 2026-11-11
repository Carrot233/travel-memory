package com.carrot233.travelmemory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carrot233.travelmemory.data.ThemeMode
import com.carrot233.travelmemory.ui.TravelApp
import com.carrot233.travelmemory.ui.theme.TravelMemoryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings = (application as TravelMemoryApp).container.settings
            val mode by settings.themeMode.collectAsStateWithLifecycle(ThemeMode.System)
            val dark = when (mode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            TravelMemoryTheme(darkTheme = dark) {
                TravelApp()
            }
        }
    }
}
