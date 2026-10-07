package com.sadique.dailyledger.ai
import android.content.Context
import com.sadique.dailyledger.auth.FirebaseRuntime
internal object FirebaseAiSetup { fun initialize(context: Context) = FirebaseRuntime.initAppCheck(context) }
