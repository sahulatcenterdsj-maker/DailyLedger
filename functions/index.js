"use strict";

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineString } = require("firebase-functions/params");
const admin = require("firebase-admin");
const { KeyManagementServiceClient } = require("@google-cloud/kms");
const { createBackupKeyHandlers } = require("./backupKeyHandlers");

if (!admin.apps.length) admin.initializeApp();

// Parameterized configuration makes deploy fail/prompt instead of silently starting without a KEK.
const KMS_KEY_NAME = defineString("KMS_KEY_NAME");
const kmsClient = new KeyManagementServiceClient();

const handlers = createBackupKeyHandlers({
  kmsClient,
  firestore: admin.firestore(),
  kmsKeyNameProvider: () => KMS_KEY_NAME.value(),
  reserveRequest: async (uid) => {
    const db = admin.firestore();
    const ref = db.collection("backupKeyUsage").doc(uid);
    const day = new Date().toISOString().slice(0, 10);
    await db.runTransaction(async (tx) => {
      const record = (await tx.get(ref)).data() || {};
      const count = record.day === day ? Number(record.count || 0) : 0;
      if (count >= 30) throw new HttpsError("resource-exhausted", "Daily backup key request limit reached. Try tomorrow.");
      tx.set(ref, { day, count: count + 1 });
    });
  },
});

// Auth is enforced again inside the handlers. App Check is enforced by the callable runtime.
exports.wrapBackupKey = onCall(
  { enforceAppCheck: true, timeoutSeconds: 30, memory: "256MiB", maxInstances: 3 },
  handlers.wrapBackupKey,
);

exports.unwrapBackupKey = onCall(
  { enforceAppCheck: true, timeoutSeconds: 30, memory: "256MiB", maxInstances: 3 },
  handlers.unwrapBackupKey,
);
