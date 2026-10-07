package com.sadique.dailyledger.security;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Versioned AES-256-GCM envelope. UID is authenticated context, never a secret/key. */
public final class EnvelopeCrypto {
    private static final byte[] MAGIC = {'D','L','E',2};
    private static final SecureRandom RANDOM = new SecureRandom();
    private EnvelopeCrypto() {}
    public static byte[] randomKey() { byte[] key = new byte[32]; RANDOM.nextBytes(key); return key; }
    public static byte[] seal(byte[] key, byte[] plain, String owner, String purpose) throws GeneralSecurityException {
        checkKey(key);
        byte[] iv = new byte[12]; RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        cipher.updateAAD(aad(owner, purpose));
        byte[] encrypted = cipher.doFinal(plain);
        byte[] out = new byte[16 + encrypted.length];
        System.arraycopy(MAGIC,0,out,0,4); System.arraycopy(iv,0,out,4,12);
        System.arraycopy(encrypted,0,out,16,encrypted.length);
        return out;
    }
    public static byte[] open(byte[] key, byte[] blob, String owner, String purpose) throws GeneralSecurityException {
        checkKey(key);
        if (blob.length < 32 || !Arrays.equals(MAGIC, Arrays.copyOf(blob,4)))
            throw new GeneralSecurityException("Invalid encrypted backup.");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key,"AES"), new GCMParameterSpec(128,blob,4,12));
        cipher.updateAAD(aad(owner,purpose));
        return cipher.doFinal(blob,16,blob.length-16);
    }
    /** RFC 5869 HKDF-SHA256; independent keys for wrapping, payloads and content fingerprints. */
    public static byte[] derive(byte[] input, byte[] salt, String info) throws GeneralSecurityException {
        checkKey(input);
        if (salt.length != 32) throw new GeneralSecurityException("Invalid key salt.");
        Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(salt,"HmacSHA256"));
        byte[] prk = mac.doFinal(input);
        try {
            mac.init(new SecretKeySpec(prk,"HmacSHA256"));
            mac.update(info.getBytes(StandardCharsets.UTF_8)); mac.update((byte)1);
            return mac.doFinal();
        } finally { Arrays.fill(prk,(byte)0); }
    }
    public static String fingerprint(byte[] key, String owner, String plain) throws GeneralSecurityException {
        byte[] derived = derive(key, new byte[32], "DailyLedger/fingerprint/v2/" + owner);
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(derived,"HmacSHA256"));
            StringBuilder out = new StringBuilder();
            for (byte b : mac.doFinal(plain.getBytes(StandardCharsets.UTF_8))) out.append(String.format("%02x",b & 255));
            return out.toString();
        } finally { Arrays.fill(derived,(byte)0); }
    }
    private static void checkKey(byte[] key) throws GeneralSecurityException {
        if (key.length != 32) throw new GeneralSecurityException("Invalid encryption key.");
    }
    private static byte[] aad(String owner,String purpose) throws GeneralSecurityException {
        if (owner.isEmpty() || owner.length()>128 || purpose.isEmpty() || purpose.length()>4096)
            throw new GeneralSecurityException("Invalid encryption context.");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.write(MAGIC); out.writeUTF(owner); out.writeUTF(purpose); out.flush(); return bytes.toByteArray();
        } catch (java.io.IOException e) { throw new GeneralSecurityException(e); }
    }
}
