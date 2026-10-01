package com.kanwarai.voiceprofessionalmessage.presentation

import androidx.lifecycle.SavedStateHandle
import com.kanwarai.voiceprofessionalmessage.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AppViewModelTest {
    @Test
    fun themeDefaultsToSystem() {
        val viewModel = AppViewModel(SavedStateHandle())

        assertEquals(ThemeMode.System, viewModel.themeMode.value)
    }

    @Test
    fun themeSelectionUpdatesAndRestoresFromSavedState() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = AppViewModel(savedStateHandle)

        viewModel.selectThemeMode(ThemeMode.Dark)
        val restoredViewModel = AppViewModel(savedStateHandle)

        assertEquals(ThemeMode.Dark, restoredViewModel.themeMode.value)
    }
}
