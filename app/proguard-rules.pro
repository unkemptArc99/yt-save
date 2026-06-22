# Keep domain models for Kotlin Serialization and UI reflection
-keep class com.ytsave.app.domain.model.** { *; }

# Chaquopy specific rules (if necessary, though Chaquopy handles most of its own)
-keep class com.chaquo.python.** { *; }

# FFmpegKit rules
-keep class com.arthenica.ffmpegkit.** { *; }

# WorkManager rules
-keep class androidx.work.** { *; }

# Serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
