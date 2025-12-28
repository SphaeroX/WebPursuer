# WebPursuer

## Overview
WebPursuer is a native Android application that allows users to monitor websites for changes. Users can browse websites using an internal browser, record interactions (like logins or clicks), and select specific areas of a webpage to monitor. If changes are detected in these areas, the user is notified via push notification.


## Demo
[![WebPursuer Demo](http://img.youtube.com/vi/xNKxBWUUmSA/0.jpg)](https://youtu.be/xNKxBWUUmSA)

## Key Features
1.  **Internal Browser & Recorder**:
    *   Navigate to any URL.
    *   Record user interactions (macros) to reach specific content (e.g., behind a login).
2.  **Element Selection**:
    *   Visually select DOM elements on the webpage.
    *   Option to refine the selection (parent/child elements).
3.  **Monitoring Configuration**:
    *   Define check intervals.
    *   Define alarm conditions (e.g., "Text contains...", "Number greater than...").
4.  **Background Service**:
    *   Periodic background checks of websites (using WorkManager).
    *   Headless execution of recorded macros.
5.  **Notifications**:
    *   Push notifications upon detected changes.
    *   Change history (Diff-View).
6.  **LLM Integration**:
    *   Integration with OpenRouter API.
    *   Generates comprehensive reports and summaries of detected changes.

## Technical Architecture
*   **Language**: Kotlin
*   **UI**: XML Views / Jetpack Compose Hybrid.
*   **Web-Engine**: Android WebView with JavaScript Injection.
*   **Database**: Room (SQLite).
*   **Background Processing**: Android WorkManager.

## Development & Debugging

### Build via Command Line
To build and debug the project without Android Studio, use the following commands in the project directory (assuming Gradle is installed or the wrapper is used):

1.  **Generate Gradle Wrapper** (if missing):
    ```powershell
    gradle wrapper
    ```
2.  **Create Debug Build**:
    ```powershell
    .\gradlew assembleDebug
    ```
    This creates the APK at `app/build/outputs/apk/debug/app-debug.apk`.

3.  **Error Analysis**:
    Run the build with `--stacktrace` or `--info` to get detailed error information:
    ```powershell
    .\gradlew assembleDebug --stacktrace
    ```

### Important Notes
*   Ensure a `local.properties` file exists in the root directory pointing to your Android SDK:
    ```properties
    sdk.dir=C:\\Users\\MGasc\\AppData\\Local\\Android\\Sdk
    ```
*   Java Development Kit (JDK) must be installed and `JAVA_HOME` set (Android Studio usually provides its own JDK).

### Troubleshooting
*   **"gradle" command not found**: If the `gradle` command is not found, look for a local Gradle installation (e.g., in `C:\Users\<User>\.gradle\wrapper\dists\...`) or use the Gradle Wrapper integrated in Android Studio by opening the project there once.
*   **Android resource linking failed**: This often indicates missing resources referenced in `AndroidManifest.xml` (e.g., icons or XML rules). Check the Manifest for references to `@mipmap/...` or `@xml/...` that do not exist yet and remove them temporarily.
*   **Build Logs**: Use `.\gradlew assembleDebug --stacktrace > build_log.txt 2>&1` to write the full logs to a file if the console cuts off the error. After analyzing the logs, they can be deleted.
*   **Kotlin & Compose Versions**: Pay attention to compatible versions.
    *   Kotlin: 1.9.22 (required for Compose Compiler 1.5.10)
    *   KSP Plugin: 1.9.22-1.0.17
    *   Compose Compiler: 1.5.10
    *   *Note*: KSP Plugin should be defined in the root `build.gradle.kts` to avoid version conflicts.
*   **Compose in MainActivity**: If `viewModel()` is used in an Activity, it must be done within a `@Composable` context (e.g., inside `setContent { ... }`), not directly in `onCreate` or callbacks that lack a Composable scope.
*   **Encoding Issues**: When analyzing logs with `findstr`, encoding issues (Unicode) may occur. Use PowerShell's `Select-String` or `type file.txt` instead.
