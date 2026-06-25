<div align="center">
  <h1>📥 YTSave</h1>
  <p><b>A modern, lightning-fast Android app for downloading and stitching YouTube Video & Audio.</b></p>
</div>

<br/>

## 🌟 Overview
YTSave is a premium, open-source Android application built to seamlessly download media from YouTube. Whether you need the highest quality stitched video, or just an audio extraction for your playlist, YTSave delivers with a beautifully crafted, dark-mode native interface.

## ✨ Features
- **🎥 High-Quality Stitching:** Automatically merges high-resolution video streams with the best audio streams using embedded FFmpeg.
- **⚡ Concurrent Downloads:** Fire off multiple downloads at once. Our robust background workers will handle the queue without dropping a sweat.
- **🎧 Format Selection:** Choose between `Stitched (Video + Audio)`, `Video Only`, or `Audio Only`.
- **🔄 Auto-Updater:** Never check for updates manually again. YTSave pings GitHub directly and prompts you to update whenever a new release drops.
- **🎨 Modern UI:** Built 100% in Jetpack Compose featuring dynamic components, seamless transitions, and a premium aesthetic.

## 🚀 How to Install
Since YTSave downloads media directly from YouTube, it is not available on the Google Play Store. 
1. Navigate to the [Releases Page](https://github.com/unkemptArc99/yt-save/releases/latest).
2. Download the latest `YTSave-x.x.x.apk` file.
3. Open the downloaded file on your Android device (you may need to allow "Install from Unknown Sources").
4. Enjoy!

## 🛠️ Tech Stack
- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Architecture:** MVVM + Clean Architecture
- **Dependency Injection:** Dagger Hilt
- **Background Tasks:** Android WorkManager
- **Core Engine:** `yt-dlp` powered by [Chaquopy](https://chaquo.com/chaquopy/) (Python for Android)
- **Processing:** FFmpegKit

## 🤝 Contributing
Feel free to open an issue or submit a pull request if you have ideas on how to improve the app!
