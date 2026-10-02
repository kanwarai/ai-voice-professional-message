package com.kanwarai.voiceprofessionalmessage.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

internal data class ExternalMessagePayload(val text: String, val mimeType: String = "text/plain")

internal fun externalMessagePayload(message: String): ExternalMessagePayload? =
    message.takeIf(String::isNotBlank)?.let(::ExternalMessagePayload)

internal fun Context.copyMessage(payload: ExternalMessagePayload): Boolean = try {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Professional message", payload.text))
    true
} catch (_: RuntimeException) {
    false
}

internal fun Context.openShareSheet(payload: ExternalMessagePayload): Boolean = try {
    val sendIntent = Intent(Intent.ACTION_SEND)
        .setType(payload.mimeType)
        .putExtra(Intent.EXTRA_TEXT, payload.text)
    startActivity(Intent.createChooser(sendIntent, "Share message"))
    true
} catch (_: RuntimeException) {
    false
}
