package com.sadique.dailyledger.sync

/** Never remove a potentially committed object after a timeout or cancellation. */
internal object BackupCommitProtocol {
    suspend fun commit(writeMetadata: suspend () -> Unit, removeRejectedUpload: suspend () -> Unit) {
        try { writeMetadata() }
        catch (e: BackupConflictException) {
            try { removeRejectedUpload() } catch (cleanup: Exception) { e.addSuppressed(cleanup) }
            throw e
        }
    }
}
