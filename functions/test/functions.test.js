"use strict";

const test = require("node:test");
const { createHash } = require("node:crypto");
const revision = "12345678-1234-1234-1234-123456789abc";
const requestData = () => ({expectedRevision: revision, formatVersion: 2, wrappedFingerprint: createHash("sha256").update(Buffer.from("wrapped").toString("base64")).digest("hex")});
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { createBackupKeyHandlers, aadFor, requireKmsKeyName } = require("../backupKeyHandlers");

function mockDeps({ exists = true, formatVersion = 2, wrappedKey = Buffer.from("wrapped").toString("base64") } = {}) {
  const calls = { encrypt: [], decrypt: [], paths: [] };
  const raw = Buffer.alloc(32, 7);
  const kmsClient = {
    async encrypt(request) {
      calls.encrypt.push({...request, plaintext: Buffer.from(request.plaintext)});
      return [{ ciphertext: Buffer.from("kms-ciphertext") }];
    },
    async decrypt(request) {
      calls.decrypt.push(request);
      return [{ plaintext: raw }];
    },
  };
  const firestore = {
    doc(docPath) {
      calls.paths.push(docPath);
      return {
        async get() {
          return {
            exists,
            data: () => ({ formatVersion, wrappedKey, revision }),
          };
        },
      };
    },
  };
  const handlers = createBackupKeyHandlers({
    kmsClient,
    firestore,
    kmsKeyNameProvider: () => "projects/demo/locations/global/keyRings/dailyledger/cryptoKeys/backup-kek",
  });
  return { handlers, calls, raw };
}

test("AAD binds authenticated uid and backup format", () => {
  assert.equal(aadFor("alice", 2).toString(), "alice:v2");
  assert.notEqual(aadFor("alice", 2).toString(), aadFor("bob", 2).toString());
});

test("wrap requires auth and exactly 32 raw key bytes", async () => {
  const { handlers } = mockDeps();
  await assert.rejects(() => handlers.wrapBackupKey({ auth: null, data: {} }), /authenticated/);
  await assert.rejects(
    () => handlers.wrapBackupKey({ auth: { uid: "alice" }, data: { rawKey: Buffer.alloc(31).toString("base64") } }),
    /256 bits/,
  );
});

test("wrap sends only caller-bound AAD and raw DEK to KMS", async () => {
  const { handlers, calls } = mockDeps();
  const raw = Buffer.alloc(32, 3);
  const result = await handlers.wrapBackupKey({ auth: { uid: "alice" }, data: { rawKey: raw.toString("base64") } });
  assert.ok(result.wrappedKey);
  assert.equal(calls.encrypt.length, 1);
  assert.equal(calls.encrypt[0].additionalAuthenticatedData.toString(), "alice:v2");
  assert.deepEqual(Buffer.from(calls.encrypt[0].plaintext), raw);
});

test("unwrap ignores client uid/key and reads authenticated user's server metadata", async () => {
  const { handlers, calls, raw } = mockDeps();
  const result = await handlers.unwrapBackupKey({
    auth: { uid: "alice" },
    data: { ...requestData(), userId: "victim", wrappedKey: Buffer.from("attacker").toString("base64") },
  });
  assert.deepEqual(calls.paths, ["users/alice/backups/latest"]);
  assert.equal(calls.decrypt[0].additionalAuthenticatedData.toString(), "alice:v2");
  assert.equal(result.rawKey, raw.toString("base64"));
});

test("unwrap fails closed for absent or legacy backup", async () => {
  await assert.rejects(() => mockDeps({ exists: false }).handlers.unwrapBackupKey({ auth: { uid: "alice" }, data: {} }), /No backup/);
  await assert.rejects(() => mockDeps({ formatVersion: 1 }).handlers.unwrapBackupKey({ auth: { uid: "alice" }, data: {} }), /encrypted format/);
});

test("KMS key configuration is validated", () => {
  assert.throws(() => requireKmsKeyName(() => ""), /KMS_KEY_NAME/);
  assert.throws(() => requireKmsKeyName(() => "not-a-kms-resource"), /KMS_KEY_NAME/);
  assert.equal(
    requireKmsKeyName(() => "projects/p/locations/global/keyRings/r/cryptoKeys/k"),
    "projects/p/locations/global/keyRings/r/cryptoKeys/k",
  );
});

test("runtime callable declarations enforce App Check", () => {
  const source = fs.readFileSync(path.join(__dirname, "..", "index.js"), "utf8");
  const matches = source.match(/enforceAppCheck:\s*true/g) || [];
  assert.equal(matches.length, 2);
  assert.doesNotMatch(source, /process\.env\.NODE_ENV/);
});

test("changed revision never unwraps or poisons a device cache", async () => {
 const {handlers,calls}=mockDeps();
 await assert.rejects(()=>handlers.unwrapBackupKey({auth:{uid:"alice"},data:{...requestData(),expectedRevision:"changed"}}),/Backup changed/);
 assert.equal(calls.decrypt.length,0);
});
test("wrong wrapped-key fingerprint is rejected before KMS", async () => {
 const {handlers,calls}=mockDeps();
 await assert.rejects(()=>handlers.unwrapBackupKey({auth:{uid:"alice"},data:{...requestData(),wrappedFingerprint:"0".repeat(64)}}),/Backup changed/);
 assert.equal(calls.decrypt.length,0);
});
test("v3 keys use distinct owner-bound KMS context", async () => {
 const {handlers,calls}=mockDeps();
 await handlers.wrapBackupKey({auth:{uid:"alice"},data:{rawKey:Buffer.alloc(32).toString("base64"),formatVersion:3}});
 assert.equal(calls.encrypt[0].additionalAuthenticatedData.toString(),"alice:v3");
});
test("anonymous auth cannot access recovery functions",async()=>{
 const {handlers,calls}=mockDeps();
 await assert.rejects(()=>handlers.unwrapBackupKey({auth:{uid:"a",token:{firebase:{sign_in_provider:"anonymous"}}},data:requestData()}),/authenticated/);
 assert.equal(calls.paths.length,0);
});
