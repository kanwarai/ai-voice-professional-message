package com.kanwarai.voiceprofessionalmessage.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalMessageActionsTest {
    @Test fun copyAndSharePayloadContainsOnlyVisibleMessage() {
        assertEquals(
            ExternalMessagePayload("Final edited message", "text/plain"),
            externalMessagePayload("Final edited message"),
        )
    }

    @Test fun blankMessageHasNoExternalPayload() {
        assertNull(externalMessagePayload("  "))
    }
}
