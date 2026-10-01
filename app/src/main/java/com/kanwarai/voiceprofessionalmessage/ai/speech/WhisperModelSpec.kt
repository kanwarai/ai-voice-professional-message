package com.kanwarai.voiceprofessionalmessage.ai.speech

import android.content.Context
import java.io.File

object WhisperModelSpec {
    const val FILE_NAME = "ggml-tiny.en.bin"
    const val BYTE_COUNT = 77_704_715L
    const val SHA256 = "0d686a2a6a22b02da2ef3101d4c86e68461363a623c58f27f81b1b2d36b42317"
    const val HUGGING_FACE_REVISION = "5359861c739e955e79d9a303bcbc70fb988958b1"

    fun privateFile(context: Context): File =
        File(File(context.noBackupFilesDir, "models"), FILE_NAME)
}
