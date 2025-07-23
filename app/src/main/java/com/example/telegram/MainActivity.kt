package com.example.telegram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.telegram.data.local.MyTelegramTheme
import com.example.telegram.domain.SettingsViewModel
import com.example.telegram.screens.App

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isDarkMode by settingsViewModel.darkModeFlow.collectAsState(initial = false)

            MyTelegramTheme(darkTheme = isDarkMode) {
                App() // Your root composable goes here
            }
        }
    }
}
