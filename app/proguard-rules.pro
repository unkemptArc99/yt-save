# Keep domain models for Kotlin Serialization and UI reflection
-keep class com.ytsave.app.domain.model.** { *; }

# WorkManager rules
-keep class androidx.work.** { *; }

# Commons Compress instantiates ZIP extra-field handlers through reflection.
-keep,allowobfuscation class org.apache.commons.compress.archivers.zip.** implements org.apache.commons.compress.archivers.zip.ZipExtraField {
    public <init>();
}

# Serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
