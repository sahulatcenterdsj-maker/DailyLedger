package com.sadique.dailyledger

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import kotlin.coroutines.cancellation.CancellationException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadique.dailyledger.auth.AccountManager
import com.sadique.dailyledger.auth.friendlyAuthError
import com.sadique.dailyledger.data.SettingsStore
import com.sadique.dailyledger.security.Biometrics
import com.sadique.dailyledger.ui.DailyLedgerApp
import com.sadique.dailyledger.ui.DailyLedgerTheme
import com.sadique.dailyledger.ui.screens.LockedScreen
import com.sadique.dailyledger.ui.screens.LoginScreen
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val notifyPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            val settings = remember { SettingsStore(applicationContext) }
            val account = remember { AccountManager(this) }
            var sessionReady by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                try { account.restoreSession() }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { settings.clearUser() }
                finally { sessionReady = true }
            }
            // null until DataStore has produced its first snapshot -> avoids flashing the login/unlocked UI.
            val loaded by settings.all.collectAsStateWithLifecycle(null)
            val current = loaded
            var unlocked by remember { mutableStateOf(false) }
            LaunchedEffect(current != null) {
                if (current != null) unlocked = !current.biometric
            }

            DailyLedgerTheme(current?.theme ?: "SYSTEM") {
                Surface(Modifier.fillMaxSize()) {
                    when {
                        current == null || !sessionReady -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        current.user == null -> AuthGate()
                        current.biometric && !unlocked -> Locked { unlocked = true }
                        else -> DailyLedgerApp(
                            user = current.user,
                            settings = settings,
                            theme = current.theme,
                            biometric = current.biometric,
                            driveEnabled = current.driveEnabled,
                            lastSync = current.lastSync,
                            cloudEnabled = current.cloudEnabled,
                            lastCloudSync = current.lastCloudSync,
                            cloudStatus = current.cloudStatus,
                            onLogout = {},
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun AuthGate() {
        val scope = rememberCoroutineScope()
        val account = remember { AccountManager(this) }
        var error by remember { mutableStateOf<String?>(null) }
        var message by remember { mutableStateOf<String?>(null) }
        var busy by remember { mutableStateOf(false) }
        fun submit(action: suspend () -> Unit) {
            if (busy) return
            busy = true; error = null; message = null
            scope.launch {
                try { action() }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { error = friendlyAuthError(e) }
                finally { busy = false }
            }
        }
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            LoginScreen(
                configured = account.configured,
                googleConfigured = account.googleConfigured,
                busy = busy, error = error, message = message,
                onGoogle = { submit { account.google() } },
                onSignIn = { email, password -> submit { account.signIn(email, password) } },
                onSignUp = { name, email, password -> submit { account.signUp(name, email, password) } },
                onResetPassword = { email -> submit {
                    account.resetPassword(email)
                    message = "If this email has an account, a reset link will be sent. Check your inbox and spam folder."
                } },
                onClearMessage = { error = null; message = null },
            )
        }
    }

    @Composable
    private fun Locked(onUnlock: () -> Unit) {
        val authenticators = remember { Biometrics.pick(this) }
        val prompt = remember {
            BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onUnlock()
                    }
                },
            )
        }
        val start: () -> Unit = {
            if (authenticators == null) {
                // No screen lock / biometrics enrolled on this device: never trap the user out of their data.
                onUnlock()
            } else {
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Daily Ledger")
                    .setSubtitle("Use biometrics or your device credential")
                    .setAllowedAuthenticators(authenticators)
                    .build()
                prompt.authenticate(info)
            }
        }
        LaunchedEffect(Unit) { start() }
        Box(Modifier.fillMaxSize().safeDrawingPadding()) { LockedScreen(start) }
    }
}
