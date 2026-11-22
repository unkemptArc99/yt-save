import java.net.URI
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

val ytDlpVersion = "2026.10.26"
val ytDlpSha256 = "e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3"
abstract class DownloadYtDlp : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val sha256: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun download() {
        val releaseVersion = version.get()
        val connection = URI("https://github.com/yt-dlp/yt-dlp/releases/download/$releaseVersion/yt-dlp")
            .toURL().openConnection().apply {
                connectTimeout = 30_000
                readTimeout = 30_000
            }
        val content = connection.getInputStream().use { it.readBytes() }
        val digest = MessageDigest.getInstance("SHA-256").digest(content)
            .joinToString("") { "%02x".format(it) }
        check(digest == sha256.get()) { "Checksum mismatch for yt-dlp $releaseVersion" }
        outputDirectory.file("yt-dlp").get().asFile.apply {
            parentFile.mkdirs()
            writeBytes(content)
        }
    }
}

android {
    namespace = "com.ytsave.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ytsave.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.0.5"
        buildConfigField("String", "YT_DLP_VERSION", "\"$ytDlpVersion\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging.jniLibs.useLegacyPackaging = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

base {
    archivesName.set("YTSave-${android.defaultConfig.versionName}")
}

androidComponents {
    onVariants { variant ->
        val downloadYtDlp = tasks.register<DownloadYtDlp>(
            "download${variant.name.replaceFirstChar { it.uppercaseChar() }}YtDlp"
        ) {
            version.set(ytDlpVersion)
            sha256.set(ytDlpSha256)
        }
        variant.sources.assets?.addGeneratedSourceDirectory(downloadYtDlp, DownloadYtDlp::outputDirectory)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
    
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    
    // Room
    val roomVersion = "2.7.2"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-compiler:2.59.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.0")
    
    val ytDlpAndroid = "0.18.1"
    implementation("io.github.junkfood02.youtubedl-android:library:$ytDlpAndroid")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:$ytDlpAndroid")
    
    // Coil
    implementation("io.coil-kt:coil-compose:2.6.0")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
