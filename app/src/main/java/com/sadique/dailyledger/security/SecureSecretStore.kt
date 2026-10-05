package com.sadique.dailyledger.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Drive passphrases are isolated by account and encrypted using Android Keystore. */
class SecureSecretStore(context: Context, ownerId: String) {
    private val prefs = context.getSharedPreferences("secure_secrets", Context.MODE_PRIVATE)
    private val suffix = MessageDigest.getInstance("SHA-256").digest(ownerId.toByteArray())
        .joinToString("") { "%02x".format(it) }
    private val alias = "daily_ledger_keystore"
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return gen.generateKey()
    }
    fun savePassphrase(value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(value.toByteArray())
        prefs.edit().putString("iv:$suffix", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("data:$suffix", Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }
    private fun load(ivKey: String, dataKey: String): String? = runCatching {
        val iv = prefs.getString(ivKey, null) ?: return null
        val data = prefs.getString(dataKey, null) ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        }
        String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
    }.getOrNull()
    fun loadPassphrase(): String? = load("iv:$suffix", "data:$suffix")
    fun hasPassphrase() = loadPassphrase() != null
    fun migrateLegacy() {
        if (!hasPassphrase()) load("iv", "data")?.let { savePassphrase(it) }
        prefs.edit().remove("iv").remove("data").apply()
    }
    fun clear() { prefs.edit().remove("iv:$suffix").remove("data:$suffix").apply() }
}
