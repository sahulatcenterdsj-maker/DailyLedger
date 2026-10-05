package com.sadique.dailyledger.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    private val magic="DLB1".toByteArray()
    fun encrypt(plain: ByteArray, passphrase: CharArray): ByteArray {
        val salt=ByteArray(16).also{SecureRandom().nextBytes(it)}; val iv=ByteArray(12).also{SecureRandom().nextBytes(it)}
        val key=derive(passphrase,salt); val c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key,GCMParameterSpec(128,iv))
        return magic + salt + iv + c.doFinal(plain)
    }
    fun decrypt(blob: ByteArray, passphrase: CharArray): ByteArray {
        require(blob.size>32 && blob.copyOfRange(0,4).contentEquals(magic)){"Invalid backup"}
        val salt=blob.copyOfRange(4,20); val iv=blob.copyOfRange(20,32); val data=blob.copyOfRange(32,blob.size)
        val c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE,derive(passphrase,salt),GCMParameterSpec(128,iv)); return c.doFinal(data)
    }
    private fun derive(p:CharArray,s:ByteArray):SecretKeySpec { val spec=PBEKeySpec(p,s,180_000,256); return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,"AES") }
}
