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
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# ── Kotlin Serialization ──────────────────────────────────────────────────────
-keepattributes RuntimeVisibleAnnotations
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class kotlinx.serialization.** { *; }

# ── Kotlin & Coroutines ───────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepnames class kotlinx.coroutines.**
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Coil ──────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
