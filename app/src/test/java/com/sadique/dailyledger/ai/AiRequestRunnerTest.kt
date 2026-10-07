package com.sadique.dailyledger.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

class AiRequestRunnerTest {
    private val request = AiModelRequest(AiTask.AUTOFILL, "rules", "private input")

    @Test fun noConsentStopsBeforeAttestationQuotaOrDataSend() = runBlocking {
        val runner = AiRequestRunner(
            { throw AiException("consent required") },
            { fail("attestation attempted") },
            { fail("quota consumed"); false },
            { fail("private input sent"); "" },
        )
        assertEquals("consent required", runCatching { runner.execute(request) }.exceptionOrNull()?.message)
    }

    @Test fun accountSwitchDuringVerificationNeverSendsData() = runBlocking {
        var owner = "first"
        val runner = AiRequestRunner(
            { if (owner != "first") throw AiException("account changed") },
            { owner = "second" },
            { fail("quota consumed"); false },
            { fail("private input sent"); "" },
        )
        assertEquals("account changed", runCatching { runner.execute(request) }.exceptionOrNull()?.message)
    }

    @Test fun revokedConsentDiscardsPendingResult() = runBlocking {
        var allowed = true
        val runner = AiRequestRunner(
            { if (!allowed) throw AiException("consent revoked") }, {}, { true },
            { allowed = false; "{\"transactions\":[]}" },
        )
        assertEquals("consent revoked", runCatching { runner.execute(request) }.exceptionOrNull()?.message)
    }

    @Test fun failedAppCheckDoesNotConsumeQuotaOrSendPrompt() = runBlocking {
        val runner = AiRequestRunner({}, { throw AiException("App Check failed") },
            { fail("quota consumed"); false }, { fail("prompt sent"); "" })
        assertEquals("App Check failed", runCatching { runner.execute(request) }.exceptionOrNull()?.message)
    }

    @Test fun quotaRejectsBeforeGeneratingAndCancellationPropagates() = runBlocking {
        val limited = AiRequestRunner({}, {}, { false }, { fail("provider called"); "" })
        assertTrue(runCatching { limited.execute(request) }.exceptionOrNull()?.message.orEmpty().contains("AI_DEVICE_LIMIT"))
        val cancelled = AiRequestRunner({}, {}, { true }, { throw CancellationException("cancelled") })
        assertTrue(runCatching { cancelled.execute(request) }.exceptionOrNull() is CancellationException)
    }

    @Test fun emptyOrOversizedResponsesCannotBecomeDrafts() = runBlocking {
        for (response in listOf("", " ", "x".repeat(65_537))) {
            val runner = AiRequestRunner({}, {}, { true }, { response })
            assertTrue(runCatching { runner.execute(request) }.exceptionOrNull()?.message.orEmpty().contains("AI_RESPONSE"))
        }
    }

    @Test fun messagesDistinguishFailuresWithoutExposingPrivateDetails() {
        val privateText = "PRIVATE user input token"
        assertTrue(AiFailures.describe(IOException(privateText)).message.orEmpty().contains("AI_NETWORK"))
        assertTrue(AiFailures.describe(SocketTimeoutException(privateText)).message.orEmpty().contains("AI_TIMEOUT"))
        assertTrue(AiFailures.describe(IllegalStateException(privateText), AiStage.APP_CHECK).message.orEmpty().contains("AI_APP_CHECK"))
        for (stage in AiStage.entries) assertFalse(AiFailures.describe(Exception(privateText), stage).message.orEmpty().contains(privateText))
    }
}
