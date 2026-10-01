package com.kanwarai.voiceprofessionalmessage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kanwarai.voiceprofessionalmessage.navigation.AppNavigation
import com.kanwarai.voiceprofessionalmessage.ui.theme.AiVoiceProfessionalMessageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiVoiceProfessionalMessageTheme {
                AppNavigation()
            }
        }
    }
}
