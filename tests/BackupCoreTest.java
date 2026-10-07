import com.sadique.dailyledger.sync.BackupPolicy;
import com.sadique.dailyledger.sync.SnapshotCodec;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

public class BackupCoreTest {
    static int checks;
    static void equal(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        checks++;
    }
    interface Checked { void run() throws Exception; }
    static void rejects(Checked action, String label) throws Exception {
        try { action.run(); } catch (java.io.IOException expected) { checks++; return; }
        throw new AssertionError(label + ": expected rejection");
    }
    static void policy(BackupPolicy.Decision expected, boolean ready, boolean hasData,
            String known, String remote, String localHash, String remoteHash, String label) {
        equal(expected, BackupPolicy.decide(ready, hasData, known, remote, localHash, remoteHash), label);
    }
    public static void main(String[] args) throws Exception {
        policy(BackupPolicy.Decision.UPLOAD, false, false, "", null, "empty", null, "new empty account");
        policy(BackupPolicy.Decision.UPLOAD, false, true, "", null, "local", null, "first backup of local data");
        policy(BackupPolicy.Decision.RESTORE, false, false, "", "server-v1", "empty", "remote", "fresh phone detects remote backup first");
        policy(BackupPolicy.Decision.CONFLICT, false, true, "", "server-v1", "local", "remote", "never discard unsynced local records");
        policy(BackupPolicy.Decision.UPLOAD, true, true, "v1", "v1", "edited", "original", "normal edit backup");
        policy(BackupPolicy.Decision.UPLOAD, true, false, "v1", "v1", "empty", "original", "intentional delete-all remains deleted");
        policy(BackupPolicy.Decision.CONFLICT, true, true, "v1", "v2", "local", "remote", "stale phone cannot overwrite newer backup");
        policy(BackupPolicy.Decision.CONFLICT, true, false, "v1", "v2", "empty", "remote", "empty stale phone cannot overwrite");
        policy(BackupPolicy.Decision.UNCHANGED, true, true, "v1", "v2", "same", "same", "retry after commit acknowledgement lost");
        policy(BackupPolicy.Decision.UNCHANGED, false, true, "", "v2", "same", "same", "same snapshot on first check");
        policy(BackupPolicy.Decision.CONFLICT, true, true, "v1", null, "local", null, "deleted remote requires explicit recovery");
        policy(BackupPolicy.Decision.UPLOAD, true, true, "", null, "local", null, "initialized account with no remote yet");

        String unicode = "{\"note\":\"کرایہ • Kameti • PKR 12,345.67\",\"amountMinor\":1234567}";
        equal(unicode, SnapshotCodec.decode(SnapshotCodec.encode(unicode)), "UTF-8 finance snapshot round trip");
        equal("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", SnapshotCodec.hash("abc"), "SHA-256 integrity hash");
        rejects(() -> SnapshotCodec.decode("not base64%%%"), "bad encoding");
        rejects(() -> SnapshotCodec.decode("A".repeat(SnapshotCodec.MAX_ENCODED + 1)), "document bound");
        rejects(() -> SnapshotCodec.encode("x".repeat(16 * 1024 * 1024 + 1)), "raw size bound");
        byte[] damaged = Base64.getDecoder().decode(SnapshotCodec.encode(unicode));
        damaged[damaged.length - 5] ^= 1;
        rejects(() -> SnapshotCodec.decode(Base64.getEncoder().encodeToString(damaged)), "corrupt gzip payload");
        ByteArrayOutputStream bomb = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bomb)) { gzip.write(new byte[17 * 1024 * 1024]); }
        rejects(() -> SnapshotCodec.decode(Base64.getEncoder().encodeToString(bomb.toByteArray())), "decompression bound");
        System.out.println(checks + " backup core checks passed.");
    }
}
