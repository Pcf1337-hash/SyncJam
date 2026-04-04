# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools proguard-android.txt file.

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }

# Keep data classes for Kotlin serialization
-keep @kotlinx.serialization.Serializable class * { *; }

# Hilt
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }

# LiveKit
-keep class io.livekit.** { *; }

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Suppress missing java.lang.management classes (not available on Android)
-dontwarn java.lang.management.**
-dontwarn javax.management.**
