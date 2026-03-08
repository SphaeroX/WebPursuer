# WebPursuer - Project Context

## Project Overview
WebPursuer is a native Android application designed to monitor websites for changes. It allows users to:
*   Navigate websites via an internal browser.
*   Record user interactions (macros) to access content behind logins or specific paths.
*   Visually select specific DOM elements for monitoring.
*   Configure check intervals and alarm conditions.
*   Receive push notifications when changes are detected.
*   Generate AI-powered reports and summaries using the OpenRouter API.

## Core Technologies
*   **Language**: Kotlin (1.9.22)
*   **UI Framework**: Jetpack Compose (Hybrid with XML Views)
*   **Database**: Room (SQLite)
*   **Networking**: Retrofit (for OpenRouter API), Jsoup (for HTML parsing)
*   **Background Processing**: Android WorkManager
*   **Web-Engine**: Android WebView with JavaScript injection for recorder and selector functionality.

## Project Structure
*   `app/src/main/java/com/murmli/webpursuer/`: Main source code.
    *   `data/`: Room database entities, DAOs, and repositories.
    *   `network/`: API definitions for OpenRouter.
    *   `ui/`: Compose screens, ViewModels, and Activities.
    *   `worker/`: WorkManager workers for background checks (`WebCheckWorker`, `ReportWorker`).
    *   `util/`: Utility classes for URL handling and networking.
*   `app/src/main/assets/`: Contains `recorder.js` for WebView interaction recording.
*   `app/src/main/res/`: Android resources (layout, drawable, values, xml).

## Building and Running
*   **Environment**: Requires Android SDK (compileSdk 35) and JDK 17 (recommended for Gradle 8+).
*   **Build via Wrapper**: 
    ```powershell
    .\gradlew assembleDebug
    ```
*   **Testing**: 
    ```powershell
    .\gradlew test
    ```
*   **Deployment**: APK is located at `app/build/outputs/apk/debug/app-debug.apk` after a successful debug build.

## Development Conventions
*   **Versioning**: 
    *   Always increment `versionCode` by **1** for every change/build.
    *   Always increment `versionName` by **0.001** (e.g., 1.113 -> 1.114).
*   **Architecture**: MVVM (Model-View-ViewModel) is used.
*   **Jetpack Compose**: Primary UI framework. ViewModels are injected into Composable screens.
*   **Database**: Room is used for persistence. Changes to models require Room migrations if already deployed.
*   **Package Name**: `com.murmli.webpursuer` (Note: the physical directory structure currently uses `com/example/webpursuer/` in `app/src/main/java`, but the code uses the correct package declaration).
*   **Macros**: User interactions are recorded as JSON-based `Interaction` objects and replayed in a headless WebView.

## AI Integration
*   The project uses OpenRouter to access various LLMs.
*   API keys should be managed securely (not committed to source control).
*   The `aiInterpreterInstruction` in `Monitor` entity allows customizing the AI prompt per monitor.
