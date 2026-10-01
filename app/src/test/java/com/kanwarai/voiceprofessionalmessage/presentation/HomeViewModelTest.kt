package com.kanwarai.voiceprofessionalmessage.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeViewModelTest {
    @Test
    fun recordSelectionExplainsUnavailableFeature() {
        val viewModel = HomeViewModel()

        viewModel.onRecordSelected()

        assertEquals(
            "Voice recording will be added in Phase 3.",
            viewModel.state.value.notice,
        )
    }

    @Test
    fun shownNoticeIsConsumed() {
        val viewModel = HomeViewModel()
        viewModel.onRecordSelected()

        viewModel.onNoticeShown()

        assertNull(viewModel.state.value.notice)
    }
}
