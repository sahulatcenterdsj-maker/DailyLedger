package com.sadique.dailyledger.ai

import android.content.Context
import com.google.firebase.appcheck.FirebaseAppCheck
import com.sadique.dailyledger.auth.FirebaseRuntime
import com.sadique.dailyledger.auth.awaitResult
import com.sadique.dailyledger.data.CategoryCatalog
import kotlinx.coroutines.TimeoutCancellationException
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

class AiService(private val context: Context, private val owner: String) {
    private val preferences = AiPreferences(context, owner)
    private val runner = AiRequestRunner(
        checkAccess = ::checkAccess,
        prepare = {
            val user = FirebaseRuntime.auth(context).currentUser
                ?: throw AiException("Please sign in to use Gemini AI. [AI_SIGN_IN]")
            try {
                if (user.getIdToken(false).awaitResult().token.isNullOrBlank()) {
                    throw AiException("Please sign in again to use Gemini AI. [AI_SIGN_IN]")
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { throw AiFailures.describe(e, AiStage.SIGN_IN) }
            checkAccess()
            FirebaseAiSetup.initialize(context)
            try {
                FirebaseAppCheck.getInstance().getAppCheckToken(false).awaitResult()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { throw AiFailures.describe(e, AiStage.APP_CHECK) }
        },
        reserve = { preferences.reserve(it) },
        generate = { GeminiGateway.generate(it) },
    )

    private fun checkAccess() {
        val user = FirebaseRuntime.auth(context).currentUser
        if (user == null || user.uid != owner || user.isAnonymous) {
            throw AiException("This account is no longer signed in. Please sign in again. [AI_SIGN_IN]")
        }
        if (!preferences.enabled) throw AiException("Enable Gemini AI in Settings before sending a request. [AI_CONSENT]")
    }

    private suspend fun call(request: AiModelRequest): String = try {
        runner.execute(request)
    } catch (e: TimeoutCancellationException) {
        throw AiException("Gemini took too long to reply. Try again later. Nothing was saved. [AI_TIMEOUT]")
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) { throw AiFailures.describe(e) }

    suspend fun drafts(input: String): AiDraftResult {
        require(input.isNotBlank() && input.length <= AiProtocol.MAX_INPUT)
        val request = AiPrompts.drafts(input, LocalDate.now(), CategoryCatalog.load(context))
        return AiProtocol.decodeDrafts(call(request))
    }

    suspend fun insights(snapshot: SpendingSnapshot): List<SpendingTip> {
        val request = AiPrompts.insights(snapshot, CategoryCatalog.load(context))
        return AiProtocol.decodeInsights(call(request))
    }
}
