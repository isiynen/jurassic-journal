# ── Annotations (required by most libraries) ──────────────────────────────────
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keep @androidx.room.TypeConverter class *
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}

# ── Hilt ──────────────────────────────────────────────────────────────────────
# Hilt, Coil and kotlinx-coroutines ship consumer ProGuard rules with their
# AARs — no whole-package keeps needed here.
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# ── Kotlin Serialization ──────────────────────────────────────────────────────
# Keep only the app's own @Serializable DTOs (profile export models); the
# runtime library carries its own consumer rules.
-keepattributes RuntimeVisibleAnnotations
-keep @kotlinx.serialization.Serializable class com.sufficienteffort.jurassicjournal.** { *; }
-keepclassmembers class com.sufficienteffort.jurassicjournal.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Kotlin ────────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
