package com.kanwarai.voiceprofessionalmessage.presentation

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeViewModelTest {
    @Test
    fun defaultsMatchProductSpecification() {
        val viewModel = HomeViewModel(SavedStateHandle())

        assertEquals(MessageType.Message, viewModel.state.value.messageType)
        assertEquals(MessageTone.Professional, viewModel.state.value.tone)
    }

    @Test
    fun selectionsUpdateStateAndRestoreFromSavedState() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = HomeViewModel(savedStateHandle)

        viewModel.selectMessageType(MessageType.Email)
        viewModel.selectTone(MessageTone.Friendly)
        val restoredViewModel = HomeViewModel(savedStateHandle)

        assertEquals(MessageType.Email, restoredViewModel.state.value.messageType)
        assertEquals(MessageTone.Friendly, restoredViewModel.state.value.tone)
    }

    @Test
    fun recordSelectionExplainsUnavailableFeatureWithoutChangingSelections() {
        val viewModel = HomeViewModel(SavedStateHandle())
        viewModel.selectTone(MessageTone.Concise)

        viewModel.onRecordSelected()

        assertEquals(
            "Voice recording will be added in Phase 3.",
            viewModel.state.value.notice,
        )
        assertEquals(MessageTone.Concise, viewModel.state.value.tone)
    }

    @Test
    fun shownNoticeIsConsumed() {
        val viewModel = HomeViewModel(SavedStateHandle())
        viewModel.onRecordSelected()

        viewModel.onNoticeShown()

        assertNull(viewModel.state.value.notice)
    }
}
