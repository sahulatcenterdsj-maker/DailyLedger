package com.sadique.dailyledger.auth

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

// Debug is for privately registered test installations; release uses device attestation.
internal fun appCheckProvider(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
