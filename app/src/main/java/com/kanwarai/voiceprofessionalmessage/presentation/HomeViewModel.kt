package com.kanwarai.voiceprofessionalmessage.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val notice: String? = null,
)

class HomeViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    fun onRecordSelected() {
        mutableState.value = HomeUiState(
            notice = "Voice recording will be added in Phase 3.",
        )
    }

    fun onNoticeShown() {
        mutableState.value = mutableState.value.copy(notice = null)
    }
}
