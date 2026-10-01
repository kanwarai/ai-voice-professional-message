package com.kanwarai.voiceprofessionalmessage.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.kanwarai.voiceprofessionalmessage.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableThemeMode = MutableStateFlow(
        savedStateHandle.get<String>(THEME_MODE_KEY)
            ?.let { savedValue -> ThemeMode.entries.find { it.name == savedValue } }
            ?: ThemeMode.System,
    )
    val themeMode: StateFlow<ThemeMode> = mutableThemeMode.asStateFlow()

    fun selectThemeMode(themeMode: ThemeMode) {
        mutableThemeMode.value = themeMode
        savedStateHandle[THEME_MODE_KEY] = themeMode.name
    }

    private companion object {
        const val THEME_MODE_KEY = "theme_mode"
    }
}
