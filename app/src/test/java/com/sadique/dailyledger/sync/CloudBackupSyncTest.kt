package com.sadique.dailyledger.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeoutException
import kotlin.coroutines.cancellation.CancellationException

class CloudBackupSyncTest {
    @Test fun timeoutAfterServerCommitKeepsTheReferencedObject() = runBlocking {
        var metadata = "old"; val objects = mutableSetOf("old", "new")
        try {
            BackupCommitProtocol.commit({ metadata = "new"; throw TimeoutException("Reply lost") }, { objects.remove("new") })
            fail("Timeout expected")
        } catch (_: TimeoutException) { }
        assertTrue(objects.contains(metadata))
        assertTrue(objects.contains("old"))
    }
    @Test fun cancelledCommitNeverDeletesAnUploadWithUnknownOutcome() = runBlocking {
        var deleted = false
        try { BackupCommitProtocol.commit({ throw CancellationException() }, { deleted = true }); fail("Cancellation expected") }
        catch (_: CancellationException) { }
        assertFalse(deleted)
    }
    @Test fun explicitRevisionConflictCleansOnlyTheRejectedUpload() = runBlocking {
        val objects = mutableSetOf("winning-backup", "rejected-upload")
        try { BackupCommitProtocol.commit({ throw BackupConflictException() }, { objects.remove("rejected-upload") }); fail("Conflict expected") }
        catch (_: BackupConflictException) { }
        assertEquals(setOf("winning-backup"), objects)
    }
    @Test fun cleanupFailureDoesNotMaskARevisionConflict() = runBlocking {
        try { BackupCommitProtocol.commit({ throw BackupConflictException() }, { throw java.io.IOException("Offline") }); fail("Conflict expected") }
        catch (e: BackupConflictException) { assertEquals(1, e.suppressed.size) }
    }
    @Test fun confirmedCommitKeepsItsNewObject() = runBlocking {
        var committed = false; var deleted = false
        BackupCommitProtocol.commit({ committed = true }, { deleted = true })
        assertTrue(committed); assertFalse(deleted)
    }
}
