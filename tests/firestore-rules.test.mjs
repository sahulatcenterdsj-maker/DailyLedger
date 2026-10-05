import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, serverTimestamp } from 'firebase/firestore';

const env = await initializeTestEnvironment({
  projectId: 'demo-daily-ledger',
  firestore: { rules: await readFile(new URL('../firestore.rules', import.meta.url), 'utf8') },
});
const alice = env.authenticatedContext('alice').firestore();
const bob = env.authenticatedContext('bob').firestore();
const guest = env.unauthenticatedContext().firestore();
const path = 'users/alice/backups/latest';
const snapshot = () => ({
  formatVersion: 1, revision: '12345678-1234-1234-1234-123456789abc',
  payload: 'compressed-snapshot-placeholder', contentHash: 'a'.repeat(64), updatedAt: serverTimestamp(),
});
try {
  await env.clearFirestore();
  await assertFails(setDoc(doc(guest, path), snapshot()));
  await assertSucceeds(setDoc(doc(alice, path), snapshot()));
  await assertSucceeds(getDoc(doc(alice, path)));
  await assertFails(getDoc(doc(bob, path)));
  await assertFails(getDoc(doc(guest, path)));
  await assertFails(setDoc(doc(bob, path), snapshot()));
  await assertFails(setDoc(doc(alice, path), { ...snapshot(), payload: 'x'.repeat(900001) }));
  await assertFails(setDoc(doc(alice, path), { ...snapshot(), unexpected: 'field' }));
  await assertSucceeds(setDoc(doc(alice, path), snapshot()));
  console.log('9 Firestore access-rule checks passed.');
} finally {
  await env.cleanup();
}
