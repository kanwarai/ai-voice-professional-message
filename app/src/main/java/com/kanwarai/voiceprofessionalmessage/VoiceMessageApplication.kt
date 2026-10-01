package com.kanwarai.voiceprofessionalmessage

import android.app.Application
import com.kanwarai.voiceprofessionalmessage.audio.AndroidAudioRecorder
import com.kanwarai.voiceprofessionalmessage.audio.AudioRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VoiceMessageApplication : Application() {
    val audioRecorder: AudioRecorder by lazy { AndroidAudioRecorder(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            audioRecorder.cleanupStaleFiles()
        }
    }
}
