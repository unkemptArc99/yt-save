# Keep domain models for Kotlin Serialization and UI reflection
-keep class com.ytsave.app.domain.model.** { *; }

# WorkManager rules
-keep class androidx.work.** { *; }

# Serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
