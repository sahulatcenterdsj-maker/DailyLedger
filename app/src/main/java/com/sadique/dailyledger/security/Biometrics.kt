package com.sadique.dailyledger.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL

object Biometrics {
    /**
     * Returns the strongest authenticator combination this device supports and has enrolled,
     * or null when the device has no usable lock (so we never lock the user out of their own data).
     */
    fun pick(context: Context): Int? {
        val manager = BiometricManager.from(context)
        val candidates = listOf(
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL,
            BIOMETRIC_WEAK,
        )
        return candidates.firstOrNull { runCatching { manager.canAuthenticate(it) }.getOrNull() == BiometricManager.BIOMETRIC_SUCCESS }
    }
}
