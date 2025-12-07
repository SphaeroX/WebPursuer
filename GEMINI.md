# WebPursuer - Projektbeschreibung

## Übersicht
WebPursuer ist eine native Android-App, die es Benutzern ermöglicht, Webseiten auf Änderungen zu überwachen. Die App orientiert sich funktional an "Web Alert". Der Benutzer kann Webseiten in einem internen Browser aufrufen, Interaktionen (wie Logins oder Klicks) aufzeichnen und spezifische Bereiche einer Webseite zur Überwachung auswählen. Bei Änderungen in diesen Bereichen wird der Benutzer per Push-Benachrichtigung informiert.

## Kernfunktionen
1.  **Interner Browser & Recorder**:
    *   Navigation zu beliebigen URLs.
    *   Aufzeichnung von Benutzerinteraktionen (Makros), um zu spezifischen Inhalten zu gelangen (z.B. hinter einem Login).
2.  **Element-Selektion**:
    *   Visuelle Auswahl von DOM-Elementen auf der Webseite.
    *   Möglichkeit, die Auswahl zu verfeinern (Eltern/Kind-Elemente).
3.  **Überwachungs-Konfiguration**:
    *   Festlegen von Intervallen für die Überprüfung.
    *   Definition von Bedingungen für den Alarm (z.B. "Text enthält...", "Zahl größer als...").
4.  **Hintergrund-Dienst**:
    *   Periodische Überprüfung der Webseiten im Hintergrund (WorkManager).
    *   Ausführung der aufgezeichneten Makros headless.
5.  **Benachrichtigungen**:
    *   Push-Benachrichtigungen bei erkannten Änderungen.
    *   Verlauf der Änderungen (Diff-View).

## Technische Architektur
*   **Sprache**: Kotlin
*   **UI**: XML Views / Jetpack Compose Hybrid.
*   **Web-Engine**: Android WebView mit JavaScript Injection.
*   **Datenbank**: Room (SQLite).
*   **Background Processing**: Android WorkManager.

## Entwicklung & Debugging

### Build via Command Line
Um das Projekt ohne Android Studio zu bauen und zu debuggen, können folgende Befehle im Projektverzeichnis verwendet werden (vorausgesetzt Gradle ist installiert oder der Wrapper wird genutzt):

1.  **Gradle Wrapper generieren** (falls nicht vorhanden):
    ```powershell
    gradle wrapper
    ```
2.  **Debug Build erstellen**:
    ```powershell
    .\gradlew assembleDebug
    ```
    Dies erstellt die APK unter `app/build/outputs/apk/debug/app-debug.apk`.

3.  **Fehleranalyse**:
    Führen Sie den Build mit `--stacktrace` oder `--info` aus, um detaillierte Fehlerinformationen zu erhalten:
    ```powershell
    .\gradlew assembleDebug --stacktrace
    ```

### Wichtige Hinweise
*   Stellen Sie sicher, dass eine `local.properties` Datei im Root-Verzeichnis existiert, die auf Ihr Android SDK zeigt:
    ```properties
    sdk.dir=C:\\Users\\MGasc\\AppData\\Local\\Android\\Sdk
    ```
*   Java Development Kit (JDK) muss installiert und `JAVA_HOME` gesetzt sein (Android Studio bringt meist ein eigenes JDK mit).

### Troubleshooting
*   **"gradle" command not found**: Wenn der `gradle` Befehl nicht gefunden wird, suchen Sie nach einer lokalen Gradle-Installation (z.B. in `C:\Users\<User>\.gradle\wrapper\dists\...`) oder nutzen Sie den in Android Studio integrierten Gradle-Wrapper, indem Sie das Projekt dort einmalig öffnen.
*   **Android resource linking failed**: Dies deutet oft auf fehlende Ressourcen hin, die im `AndroidManifest.xml` referenziert werden (z.B. Icons oder XML-Regeln). Überprüfen Sie das Manifest auf Referenzen zu `@mipmap/...` oder `@xml/...`, die noch nicht existieren, und entfernen Sie diese vorerst.
*   **Build Logs**: Nutzen Sie `.\gradlew assembleDebug --stacktrace > build_log.txt 2>&1`, um die vollständigen Logs in eine Datei zu schreiben, falls die Konsole den Fehler abschneidet. Nach der Analyse der Logs können diese wieder gelöscht werden.
*   **Kotlin & Compose Versionen**: Achten Sie auf kompatible Versionen.
    *   Kotlin: 1.9.22 (benötigt für Compose Compiler 1.5.10)
    *   KSP Plugin: 1.9.22-1.0.17
    *   Compose Compiler: 1.5.10
    *   *Hinweis*: KSP Plugin sollte im Root `build.gradle.kts` definiert sein, um Versionskonflikte zu vermeiden.
*   **Compose in MainActivity**: Wenn `viewModel()` in einer Activity verwendet wird, muss dies innerhalb eines `@Composable` Kontexts geschehen (z.B. innerhalb von `setContent { ... }`), nicht direkt in `onCreate` oder Callbacks, die keinen Composable-Scope haben.
*   **Encoding Probleme**: Bei der Analyse von Logs mit `findstr` kann es zu Encoding-Problemen kommen (Unicode). Nutzen Sie stattdessen PowerShell's `Select-String` oder `type file.txt`.

> [!IMPORTANT]
> **Automatische Aktualisierung**: Diese Datei (`GEMINI.md`) soll bei wichtigen neuen Erkenntnissen, insbesondere bezüglich Build-Konfigurationen, Fehlern und deren Lösungen, automatisch aktualisiert werden, um zukünftiges Debugging zu erleichtern.
> Nachdem du das Dateien geändert hast, verschieh vor dem Abschluss immer die App zu bauen und zu testen. 