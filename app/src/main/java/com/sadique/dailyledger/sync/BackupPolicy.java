package com.sadique.dailyledger.sync;

import java.util.Objects;

/** A stale or newly installed device must never overwrite an existing different backup. */
public final class BackupPolicy {
    private BackupPolicy() {}
    public enum Decision { UPLOAD, RESTORE, UNCHANGED, CONFLICT }
    public static Decision decide(boolean initialized, boolean hasLocalRecords,
            String knownRevision, String remoteRevision, String localHash, String remoteHash) {
        if (remoteRevision == null) {
            return !initialized || knownRevision == null || knownRevision.isEmpty()
                    ? Decision.UPLOAD : Decision.CONFLICT;
        }
        if (Objects.equals(localHash, remoteHash)) return Decision.UNCHANGED;
        if (!initialized) return hasLocalRecords ? Decision.CONFLICT : Decision.RESTORE;
        return Objects.equals(knownRevision, remoteRevision) ? Decision.UPLOAD : Decision.CONFLICT;
    }
}
