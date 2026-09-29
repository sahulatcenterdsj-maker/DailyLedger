# ProGuard rules for Daily Ledger
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
