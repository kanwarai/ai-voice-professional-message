package com.kanwarai.voiceprofessionalmessage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kanwarai.voiceprofessionalmessage.navigation.AppNavigation
import com.kanwarai.voiceprofessionalmessage.presentation.AppViewModel
import com.kanwarai.voiceprofessionalmessage.ui.theme.AiVoiceProfessionalMessageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = viewModel()
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()

            AiVoiceProfessionalMessageTheme(themeMode = themeMode) {
                AppNavigation(
                    themeMode = themeMode,
                    onThemeModeSelected = appViewModel::selectThemeMode,
                )
            }
        }
    }
}
