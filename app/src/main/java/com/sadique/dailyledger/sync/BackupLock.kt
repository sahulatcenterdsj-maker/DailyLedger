package com.sadique.dailyledger.sync

import kotlinx.coroutines.sync.Mutex

/** Serialize local edits, account switches and complete backup/restore operations. */
object BackupLock { val mutex = Mutex() }
