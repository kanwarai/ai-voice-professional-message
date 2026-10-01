package com.kanwarai.voiceprofessionalmessage.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val messageType: MessageType = MessageType.Message,
    val tone: MessageTone = MessageTone.Professional,
    val notice: String? = null,
)

enum class MessageType(val label: String) {
    Message("Message"),
    Email("Email"),
}

enum class MessageTone(val label: String) {
    Professional("Professional"),
    Friendly("Friendly"),
    Concise("Concise"),
}

class HomeViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        HomeUiState(
            messageType = restoredEnum(MESSAGE_TYPE_KEY, MessageType.Message),
            tone = restoredEnum(TONE_KEY, MessageTone.Professional),
        ),
    )
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    fun selectMessageType(messageType: MessageType) {
        mutableState.value = mutableState.value.copy(messageType = messageType)
        savedStateHandle[MESSAGE_TYPE_KEY] = messageType.name
    }

    fun selectTone(tone: MessageTone) {
        mutableState.value = mutableState.value.copy(tone = tone)
        savedStateHandle[TONE_KEY] = tone.name
    }

    fun onRecordSelected() {
        mutableState.value = mutableState.value.copy(
            notice = "Voice recording will be added in Phase 3.",
        )
    }

    fun onNoticeShown() {
        mutableState.value = mutableState.value.copy(notice = null)
    }

    private inline fun <reified T : Enum<T>> restoredEnum(key: String, default: T): T {
        val savedValue = savedStateHandle.get<String>(key) ?: return default
        return enumValues<T>().find { it.name == savedValue } ?: default
    }

    private companion object {
        const val MESSAGE_TYPE_KEY = "message_type"
        const val TONE_KEY = "tone"
    }
}
