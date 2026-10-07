package com.sadique.dailyledger.auth

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.sadique.dailyledger.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

object FirebaseRuntime {
    private var appCheckInitialized = false

    fun configured(context: Context): Boolean = BuildConfig.FIREBASE_CONFIGURED &&
        (FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null)

    @Synchronized fun initAppCheck(context: Context) {
        if (!configured(context) || appCheckInitialized) return
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        appCheckInitialized = true
    }

    fun auth(context: Context): FirebaseAuth {
        check(configured(context)) { "Online sign-in setup is pending for this app." }
        return FirebaseAuth.getInstance()
    }

    fun firestore(context: Context): FirebaseFirestore {
        check(configured(context)) { "Cloud backup setup is pending for this app." }
        return FirebaseFirestore.getInstance()
    }

    private val cacheLock = Mutex()
    private var cacheReady = false
    suspend fun prepareBackup(context: Context) = cacheLock.withLock {
        if (!cacheReady) {
            val db = firestore(context)
            db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
            db.clearPersistence().awaitResult()
            cacheReady = true
        }
    }

    fun storage(context: Context): FirebaseStorage {
        check(configured(context)) { "Firebase Storage setup is pending for this app." }
        return FirebaseStorage.getInstance()
    }

    fun functions(context: Context): FirebaseFunctions {
        check(configured(context)) { "Firebase Functions setup is pending for this app." }
        return FirebaseFunctions.getInstance()
    }

    fun googleClientId(context: Context): String {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id != 0) context.getString(id) else BuildConfig.GOOGLE_WEB_CLIENT_ID
    }
}

/** Bounded wait on IO; no extra coroutine adapter dependency is needed. */
suspend fun <T> Task<T>.awaitResult(): T = withContext(Dispatchers.IO) {
    try { Tasks.await(this@awaitResult, 40, TimeUnit.SECONDS) }
    catch (e: ExecutionException) { throw (e.cause ?: e) }
}
