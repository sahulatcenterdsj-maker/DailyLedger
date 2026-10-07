"use strict";

const { createHash } = require("node:crypto");
let HttpsError;
try {
  ({ HttpsError } = require("firebase-functions/v2/https"));
} catch (_) {
  // Keeps the pure handler module unit-testable before npm install. Production Functions always
  // provide firebase-functions and therefore use the real callable HttpsError implementation.
  HttpsError = class HttpsError extends Error {
    constructor(code, message) {
      super(message);
      this.name = "HttpsError";
      this.code = code;
    }
  };
}

const KMS_NAME = /^projects\/[^/]+\/locations\/[^/]+\/keyRings\/[^/]+\/cryptoKeys\/[^/]+$/;

function requireAuth(request) {
  const uid = request && request.auth && request.auth.uid;
  if (!uid || typeof uid !== "string" || uid.length > 128 || uid.includes("/") || request.auth.token?.firebase?.sign_in_provider === "anonymous") {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }
  return uid;
}

function requireKmsKeyName(provider) {
  const value = provider && provider();
  if (!value || typeof value !== "string" || !KMS_NAME.test(value.trim())) {
    throw new HttpsError("failed-precondition", "KMS_KEY_NAME is not configured correctly on the server.");
  }
  return value.trim();
}

function decodeBase64(value, label) {
  if (!value || typeof value !== "string" || value.length > 8192) {
    throw new HttpsError("invalid-argument", `${label} must be a base64 string.`);
  }
  const compact = value.trim();
  // Buffer.from is lenient, so reject non-base64 characters and malformed padding first.
  if (!/^[A-Za-z0-9+/]+={0,2}$/.test(compact) || compact.length % 4 !== 0) {
    throw new HttpsError("invalid-argument", `${label} is not valid base64.`);
  }
  return Buffer.from(compact, "base64");
}

function aadFor(uid, formatVersion) {
  return Buffer.from(`${uid}:v${formatVersion}`, "utf8");
}

function createBackupKeyHandlers({ kmsClient, firestore, kmsKeyNameProvider, reserveRequest = async () => {} }) {
  if (!kmsClient || !firestore || !kmsKeyNameProvider) {
    throw new Error("Missing handler dependencies");
  }

  async function wrapBackupKey(request) {
    const uid = requireAuth(request);
    const rawKey = decodeBase64(request.data && request.data.rawKey, "rawKey");
    if (rawKey.length !== 32) {
      throw new HttpsError("invalid-argument", "Data Encryption Key must be exactly 256 bits.");
    }

    const version = request.data?.formatVersion ?? 2;
    if (![2, 3].includes(version)) throw new HttpsError("invalid-argument", "Unsupported backup format.");
    const keyName = requireKmsKeyName(kmsKeyNameProvider);
    await reserveRequest(uid);
    try {
      const [response] = await kmsClient.encrypt({
        name: keyName,
        plaintext: rawKey,
        additionalAuthenticatedData: aadFor(uid, version),
      });
      if (!response || !response.ciphertext) throw new Error("KMS returned no ciphertext");
      return { wrappedKey: Buffer.from(response.ciphertext).toString("base64") };
    } catch (error) {
      if (error instanceof HttpsError) throw error;
      console.error("KMS wrap failed", error && error.code ? error.code : "unknown");
      throw new HttpsError("internal", "Failed to wrap backup key securely.");
    } finally { rawKey.fill(0); }
  }

  async function unwrapBackupKey(request) {
    const uid = requireAuth(request);
    const docPath = `users/${uid}/backups/latest`;
    const snapshot = await firestore.doc(docPath).get();
    if (!snapshot.exists) {
      throw new HttpsError("not-found", "No backup record found for this account.");
    }

    const data = snapshot.data() || {};
    const formatVersion = data.formatVersion || 1;
    if (![2, 3].includes(formatVersion) || !data.wrappedKey) {
      throw new HttpsError("failed-precondition", "Backup is not in encrypted format.");
    }

    const fingerprint = createHash("sha256").update(data.wrappedKey).digest("hex");
    const expected = request.data || {};
    if (expected.expectedRevision !== data.revision || expected.wrappedFingerprint !== fingerprint || expected.formatVersion !== formatVersion) {
      throw new HttpsError("aborted", "Backup changed. Read the latest revision and retry.");
    }
    const wrappedKey = decodeBase64(data.wrappedKey, "wrappedKey");
    await reserveRequest(uid);
    const keyName = requireKmsKeyName(kmsKeyNameProvider);
    try {
      const [response] = await kmsClient.decrypt({
        name: keyName,
        ciphertext: wrappedKey,
        additionalAuthenticatedData: aadFor(uid, formatVersion),
      });
      const plaintext = response && response.plaintext ? Buffer.from(response.plaintext) : Buffer.alloc(0);
      if (plaintext.length !== 32) throw new Error("KMS returned an invalid DEK length");
      try { return { rawKey: plaintext.toString("base64"), revision: data.revision, wrappedFingerprint: fingerprint }; }
      finally { plaintext.fill(0); }
    } catch (error) {
      if (error instanceof HttpsError) throw error;
      console.error("KMS unwrap failed", error && error.code ? error.code : "unknown");
      throw new HttpsError("internal", "Failed to unwrap backup key.");
    }
  }

  return { wrapBackupKey, unwrapBackupKey };
}

module.exports = {
  aadFor,
  createBackupKeyHandlers,
  decodeBase64,
  requireKmsKeyName,
};
