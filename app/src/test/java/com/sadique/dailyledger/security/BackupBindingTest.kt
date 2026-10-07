package com.sadique.dailyledger.security
import org.junit.Assert.*
import org.junit.Test
import javax.crypto.AEADBadTagException
class BackupBindingTest {
 @Test fun ciphertextCannotMoveBetweenAccountsOrRevisions(){
  val key=BackupCrypto.generateDek();val bytes="private ledger".toByteArray();val aad=BackupCrypto.snapshotAad("alice","rev1")
  val encrypted=BackupCrypto.encryptPayload(bytes,key,aad)
  assertArrayEquals(bytes,BackupCrypto.decryptPayload(encrypted.ciphertext,encrypted.iv,key,aad))
  for(other in listOf(BackupCrypto.snapshotAad("bob","rev1"),BackupCrypto.snapshotAad("alice","rev2"))){
   try{BackupCrypto.decryptPayload(encrypted.ciphertext,encrypted.iv,key,other);fail("Context swap accepted")}catch(_:AEADBadTagException){}
  }
 }
 @Test fun fingerprintDoesNotExposeAnUnkeyedPlaintextHash(){
  val a=BackupCrypto.generateDek();val b=BackupCrypto.generateDek()
  val one=BackupCrypto.plainFingerprint("salary:100",a,"alice")
  assertEquals(one,BackupCrypto.plainFingerprint("salary:100",a,"alice"))
  assertNotEquals(one,BackupCrypto.plainFingerprint("salary:100",b,"alice"))
  assertNotEquals(one,BackupCrypto.plainFingerprint("salary:100",a,"bob"))
  assertNotEquals(one,BackupCrypto.sha256Hex("salary:100".toByteArray()))
 }
}
