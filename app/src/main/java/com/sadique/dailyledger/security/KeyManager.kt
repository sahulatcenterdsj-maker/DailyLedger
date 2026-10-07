package com.sadique.dailyledger.security

import android.content.Context
import android.util.Base64
import com.sadique.dailyledger.auth.FirebaseRuntime
import com.sadique.dailyledger.auth.awaitResult
import com.sadique.dailyledger.sync.SnapshotCodec
import org.json.JSONObject
import javax.crypto.spec.SecretKeySpec

data class DekWrapResult(val dek: SecretKeySpec, val wrappedKey: String)

/** Account recovery is managed by the authenticated KMS backend, not by UID-derived keys. */
class KeyManager(private val context: Context, private val userId: String) {
    private val secrets = SecureSecretStore(context, "kms-dek-v3:$userId")
    private fun checkAccount() {
        check(FirebaseRuntime.auth(context).currentUser?.let { it.uid == userId && !it.isAnonymous } == true) { "Sign in to this account again." }
    }
    private fun cached(wrapped: String): SecretKeySpec? {
        val bytes = runCatching { secrets.loadBytes() }.getOrNull() ?: return null
        return try {
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            if (json.getString("fingerprint") != SnapshotCodec.hash(wrapped)) null
            else Base64.decode(json.getString("key"), Base64.NO_WRAP).let {
                require(it.size == 32)
                try { SecretKeySpec(it, "AES") } finally { it.fill(0) }
            }
        } finally { bytes.fill(0) }
    }
    private fun cache(key: SecretKeySpec, wrapped: String) {
        val bytes = JSONObject().put("fingerprint", SnapshotCodec.hash(wrapped))
            .put("key", Base64.encodeToString(key.encoded, Base64.NO_WRAP)).toString().toByteArray()
        try { secrets.saveBytes(bytes) } finally { bytes.fill(0) }
    }
    fun clearLocalCache() = secrets.clearBytes()
    suspend fun getExistingDek(wrapped: String, revision: String, format: Long): SecretKeySpec {
        checkAccount()
        cached(wrapped)?.let { return it }
        return retrieve(wrapped, revision, format).also { cache(it, wrapped) }
    }
    suspend fun refreshExistingDek(wrapped: String, revision: String, format: Long): SecretKeySpec {
        checkAccount(); clearLocalCache()
        return retrieve(wrapped, revision, format).also { cache(it, wrapped) }
    }
    private suspend fun retrieve(wrapped: String, revision: String, format: Long): SecretKeySpec {
        checkAccount()
        val fingerprint = SnapshotCodec.hash(wrapped)
        val result = FirebaseRuntime.functions(context).getHttpsCallable("unwrapBackupKey").call(mapOf(
            "expectedRevision" to revision, "wrappedFingerprint" to fingerprint, "formatVersion" to format
        )).awaitResult()
        checkAccount()
        val data = result.data as? Map<*, *> ?: error("Invalid backup key response.")
        check(data["revision"] == revision && data["wrappedFingerprint"] == fingerprint) { "Backup changed on another phone. Please retry." }
        val encoded = data["rawKey"] as? String ?: error("Invalid backup key response.")
        require(encoded.length == 44)
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        require(bytes.size == 32)
        return try { SecretKeySpec(bytes, "AES") } finally { bytes.fill(0) }
    }
    suspend fun createAndWrapNewDek(): DekWrapResult {
        checkAccount()
        val dek = BackupCrypto.generateDek()
        val result = FirebaseRuntime.functions(context).getHttpsCallable("wrapBackupKey")
            .call(mapOf("rawKey" to Base64.encodeToString(dek.encoded, Base64.NO_WRAP), "formatVersion" to 3))
            .awaitResult()
        checkAccount()
        val wrapped = (result.data as? Map<*, *>)?.get("wrappedKey") as? String ?: error("Invalid wrapped backup key response.")
        require(wrapped.length in 16..4096)
        cache(dek, wrapped)
        return DekWrapResult(dek, wrapped)
    }
}
