import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { ref, uploadBytes, getBytes, deleteObject } from 'firebase/storage';

const env = await initializeTestEnvironment({
  projectId: 'demo-daily-ledger',
  storage: { rules: await readFile(new URL('../storage.rules', import.meta.url), 'utf8') },
});
const alice = env.authenticatedContext('alice').storage();
const bob = env.authenticatedContext('bob').storage();
const guest = env.unauthenticatedContext().storage();
const revision = '12345678-1234-1234-1234-123456789abc';
const path = `users/alice/backups/${revision}.enc`;
const bytes = new Uint8Array(32);
const metadata = { contentType: 'application/octet-stream', customMetadata: { ownerId: 'alice', revision } };

try {
  await assertFails(uploadBytes(ref(guest, path), bytes, metadata));
  await assertFails(uploadBytes(ref(bob, path), bytes, metadata));
  await assertFails(uploadBytes(ref(alice, path), bytes, { ...metadata, customMetadata: { ownerId: 'bob', revision } }));
  await assertFails(uploadBytes(ref(alice, path), bytes, { ...metadata, contentType: 'text/plain' }));
  await assertFails(uploadBytes(ref(alice, path), bytes, { ...metadata, customMetadata: { ownerId: 'alice', revision: 'wrong' } }));
  await assertFails(uploadBytes(ref(alice, 'users/alice/backups/plain.txt'), bytes, metadata));
  await assertSucceeds(uploadBytes(ref(alice, path), bytes, metadata));
  await assertSucceeds(getBytes(ref(alice, path)));
  await assertFails(getBytes(ref(bob, path)));
  // Existing encrypted objects are immutable; each backup revision uses a fresh path.
  await assertFails(uploadBytes(ref(alice, path), new Uint8Array([9]), metadata));
  await assertSucceeds(deleteObject(ref(alice, path)));
  console.log('Storage backup rule checks passed.');
} finally {
  await env.cleanup();
}
