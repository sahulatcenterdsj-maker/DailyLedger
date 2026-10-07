import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, deleteDoc, serverTimestamp } from 'firebase/firestore';

const env = await initializeTestEnvironment({
  projectId: 'demo-daily-ledger',
  firestore: { rules: await readFile(new URL('../firestore.rules', import.meta.url), 'utf8') },
});
const alice = env.authenticatedContext('alice').firestore();
const bob = env.authenticatedContext('bob').firestore();
const guest = env.unauthenticatedContext().firestore();
const path = 'users/alice/backups/latest';
const revision = '12345678-1234-1234-1234-123456789abc';

const v1Snapshot = () => ({
  formatVersion: 1,
  revision,
  payload: 'compressed-snapshot-placeholder',
  contentHash: 'a'.repeat(64),
  updatedAt: serverTimestamp(),
});

const v2Snapshot = () => ({
  formatVersion: 3,
  revision,
  storagePath: `users/alice/backups/${revision}.enc`,
  wrappedKey: 'd3JhcHBlZC1rZXktcGxhY2Vob2xkZXI=',
  contentHash: 'a'.repeat(64),
  plainHash: 'b'.repeat(64),
  iv: 'MTIzNDU2Nzg5MDEy',
  updatedAt: serverTimestamp(),
});

try {
  await env.clearFirestore();
  await assertFails(setDoc(doc(guest, path), v2Snapshot()));
  await assertSucceeds(setDoc(doc(alice, path), v2Snapshot()));
  await assertSucceeds(getDoc(doc(alice, path)));
  await assertFails(getDoc(doc(bob, path)));
  await assertFails(getDoc(doc(guest, path)));
  await assertFails(setDoc(doc(bob, path), v2Snapshot()));
  await assertFails(setDoc(doc(alice, path), { ...v2Snapshot(), storagePath: `users/bob/backups/${revision}.enc` }));
  await assertFails(setDoc(doc(alice, path), { ...v2Snapshot(), plainHash: 'not-a-hash' }));
  await assertFails(setDoc(doc(alice, path), { ...v2Snapshot(), unexpected: 'field' }));
  await assertFails(setDoc(doc(alice, path), v1Snapshot()));
  await assertFails(setDoc(doc(alice, path), { ...v2Snapshot(), formatVersion: 2 }));
  await assertFails(setDoc(doc(alice, path), { ...v2Snapshot(), iv: 'a'.repeat(32) }));
  await assertFails(setDoc(doc(alice, path), { ...v2Snapshot(), storagePath: `users/alice/backups/00000000-1234-1234-1234-123456789abc.enc` }));
  await env.withSecurityRulesDisabled(async context => setDoc(doc(context.firestore(), path), v1Snapshot()));
  await assertSucceeds(getDoc(doc(alice,path)));
  await assertFails(getDoc(doc(bob,path)));
  await assertFails(setDoc(doc(alice, path), { ...v1Snapshot(), payload: '' }));
  await assertSucceeds(deleteDoc(doc(alice, path)));
  console.log('Firestore backup rule checks passed.');
} finally {
  await env.cleanup();
}
