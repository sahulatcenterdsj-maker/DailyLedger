buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Keep built-in Kotlin aligned with the Compose compiler plugin.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}

plugins {
    id("com.google.gms.google-services") version "4.5.0" apply false
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
}
