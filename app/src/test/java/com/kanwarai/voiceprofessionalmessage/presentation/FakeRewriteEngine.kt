package com.kanwarai.voiceprofessionalmessage.presentation

import com.kanwarai.voiceprofessionalmessage.ai.rewrite.RewriteEngine
import com.kanwarai.voiceprofessionalmessage.ai.rewrite.RewriteMetrics
import com.kanwarai.voiceprofessionalmessage.ai.rewrite.RewriteRequest
import com.kanwarai.voiceprofessionalmessage.ai.rewrite.RewriteResult

class FakeRewriteEngine : RewriteEngine {
    var result: RewriteResult = RewriteResult.Success("Polished message", RewriteMetrics(20, 30, 40, 12))
    var calls = 0
    var cancelCalls = 0
    var releaseCalls = 0
    var request: RewriteRequest? = null
    override suspend fun rewrite(request: RewriteRequest): RewriteResult {
        calls++; this.request = request; return result
    }
    override fun cancel() { cancelCalls++ }
    override fun release() { releaseCalls++ }
}
