package com.sadique.dailyledger.sync;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Compact JSON fits a Firestore document. Bounds also protect restore from oversized input. */
public final class SnapshotCodec {
    public static final int MAX_ENCODED = 900_000;
    private static final int MAX_PLAIN = 16 * 1024 * 1024;
    private SnapshotCodec() {}
    public static String encode(String json) throws IOException {
        byte[] raw = json.getBytes(StandardCharsets.UTF_8);
        if (raw.length > MAX_PLAIN) throw new IOException("Backup is too large. Use Google Drive or export your data.");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) { gzip.write(raw); }
        String encoded = Base64.getEncoder().encodeToString(out.toByteArray());
        if (encoded.length() > MAX_ENCODED) throw new IOException("Backup is too large for account storage. Use Google Drive.");
        return encoded;
    }
    public static String decode(String encoded) throws IOException {
        if (encoded == null || encoded.length() > MAX_ENCODED) throw new IOException("Invalid backup size");
        final byte[] compressed;
        try { compressed = Base64.getDecoder().decode(encoded); }
        catch (IllegalArgumentException e) { throw new IOException("Invalid backup encoding", e); }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed));
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = gzip.read(buffer)) != -1) {
                if (out.size() + count > MAX_PLAIN) throw new IOException("Backup exceeds the restore size limit");
                out.write(buffer, 0, count);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
    public static String hash(String json) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format("%02x", b & 0xff));
            return out.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
