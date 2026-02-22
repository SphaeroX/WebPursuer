# AGENTS.md

Coding agent instructions for the WebPursuer Android project.

## Project Overview

WebPursuer is a native Android app for monitoring websites for changes. Users can browse websites, record interactions, select elements to monitor, and receive notifications when changes are detected.

**Tech Stack:**
- Language: Kotlin
- UI: Jetpack Compose (Material3) with XML Views hybrid
- Database: Room (SQLite)
- Background Processing: WorkManager
- Network: Retrofit with OkHttp
- Async: Kotlin Coroutines & Flow

## Build Commands

```powershell
# Debug build
.\gradlew assembleDebug

# Debug build (quiet mode)
.\gradlew assembleDebug -q

# Release build
.\gradlew assembleRelease

# Clean build
.\gradlew clean assembleDebug

# Build with detailed error output
.\gradlew assembleDebug --stacktrace

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Test Commands

```powershell
# Run all unit tests
.\gradlew test

# Run a single test class
.\gradlew test --tests "com.murmli.webpursuer.util.UrlUtilsTest"

# Run a single test method
.\gradlew test --tests "com.murmli.webpursuer.util.UrlUtilsTest.extractUrlFromText_findsUrl"

# Run with detailed output
.\gradlew test --info

# Run Android instrumented tests (requires connected device/emulator)
.\gradlew connectedAndroidTest
```

Test locations:
- Unit tests: `app/src/test/java/com/murmli/webpursuer/`
- Instrumented tests: `app/src/androidTest/java/com/murmli/webpursuer/`

## Lint Commands

```powershell
# Run Android lint
.\gradlew lint

# Run lint for debug variant
.\gradlew lintDebug

# View lint report (after running lint)
# Report: app/build/reports/lint-results-debug.html
```

## Code Style Guidelines

### Package Structure

```
com.murmli.webpursuer/
├── data/           # Database entities, DAOs, repositories
├── ui/             # Composables, ViewModels, activities
├── network/        # API services, Retrofit interfaces
├── worker/         # WorkManager workers
├── util/           # Utility classes
└── utils/          # Additional utilities (note: both exist)
```

### Imports

Order imports as follows:
1. Android framework imports (`android.*`)
2. AndroidX imports (`androidx.*`)
3. Kotlin imports (`kotlin.*`, `kotlinx.*`)
4. Third-party libraries (`okhttp3.*`, `retrofit2.*`, etc.)
5. Project imports (`com.murmli.webpursuer.*`)

Use fully qualified names for rarely used classes to avoid import clutter.

### Formatting

- Indentation: 4 spaces (no tabs)
- Maximum line length: 100 characters
- Blank lines between logical sections
- Opening brace on same line for functions/classes
- No trailing whitespace

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `WebChecker`, `MonitorViewModel` |
| Functions | camelCase | `checkMonitor()`, `loadContent()` |
| Variables | camelCase | `contentHash`, `changePercentage` |
| Constants | SCREAMING_SNAKE_CASE | `MAX_RETRIES` |
| Data classes | PascalCase | `Monitor`, `CheckLog` |
| Composables | PascalCase | `HomeScreen()`, `MonitorItem()` |
| State variables | prefixed with verb | `showDialog`, `isLoading` |
| Flow/StateFlow | descriptive nouns | `monitors`, `apiKey` |

### Types

- Use `val` over `var` whenever possible
- Prefer nullable types with `?` over platform types
- Use `String?` for optional strings, not empty string defaults
- Use `Long` for timestamps (milliseconds since epoch)
- Use `Double` for percentages and measurements
- Entity IDs use `Int` with auto-generation

### Data Classes (Room Entities)

```kotlin
@Entity(tableName = "monitors")
data class Monitor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val name: String,
    val enabled: Boolean = true,
    @ColumnInfo(defaultValue = "0") val useAiInterpreter: Boolean = false
)
```

### ViewModels

```kotlin
class MonitorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val monitorDao = database.monitorDao()
    
    val monitors: StateFlow<List<Monitor>> =
        monitorDao.getAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    fun updateMonitor(monitor: Monitor) {
        viewModelScope.launch { monitorDao.update(monitor) }
    }
}
```

### Composables

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorItem(
    monitor: Monitor,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = monitor.name, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
```

### Error Handling

- Use try-catch blocks for network operations and parsing
- Log errors using `android.util.Log.e()` or the `LogRepository`
- Return safe default values on error (e.g., empty string, false, null)
- Store error information in `CheckLog` or `AppLog` for debugging

```kotlin
try {
    val response = api.getCompletion(request)
    return response.choices.firstOrNull()?.message?.content ?: ""
} catch (e: Exception) {
    e.printStackTrace()
    logRepository?.logError("TAG", "Operation failed: ${e.message}")
    return ""
}
```

### Coroutines

- Use `viewModelScope` for ViewModel operations
- Use `Dispatchers.IO` for database and network operations
- Use `Dispatchers.Main` for WebView operations
- Use `suspendCancellableCoroutine` for callback-to-coroutine conversion

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    val data = repository.loadData()
    // Update UI on Main
    withContext(Dispatchers.Main) {
        updateUi(data)
    }
}
```

### Comments

Do NOT add comments to code unless explicitly requested by the user. The code should be self-documenting through clear naming and structure.

## Version Requirements

| Component | Version |
|-----------|---------|
| Kotlin | 1.9.22 |
| Compose Compiler | 1.5.10 |
| KSP Plugin | 1.9.22-1.0.17 |
| Room | 2.6.1 |
| WorkManager | 2.9.0 |
| Compose BOM | 2024.02.00 |
| minSdk | 26 |
| targetSdk | 35 |

## Testing Guidelines

- Use JUnit 4 for unit tests (`testImplementation("junit:junit:4.13.2")`)
- Use Espresso for UI tests (`androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")`)
- Test file naming: `<ClassName>Test.kt`
- Place tests in same package structure as source files

```kotlin
class UrlUtilsTest {
    @Test
    fun extractUrlFromText_findsUrl() {
        val text = "Check this out https://example.com it is cool"
        val url = UrlUtils.extractUrlFromText(text)
        assertEquals("https://example.com", url)
    }
}
```

## Important Files

| File | Purpose |
|------|---------|
| `build.gradle.kts` (root) | Project-level Gradle config |
| `app/build.gradle.kts` | App module config, dependencies |
| `settings.gradle.kts` | Repository and module config |
| `AndroidManifest.xml` | App permissions, components |
| `AppDatabase.kt` | Room database singleton |
| `GEMINI.md` | Additional development notes |

## Troubleshooting

- **Build fails with "gradle command not found"**: Use `.\gradlew` (Gradle wrapper) instead of `gradle`
- **Resource linking failed**: Check `AndroidManifest.xml` for missing `@mipmap/` or `@xml/` references
- **Compose compiler errors**: Ensure Kotlin version matches Compose compiler compatibility
- **Database migration issues**: The project uses `fallbackToDestructiveMigration()` for development

## Pre-commit Checklist

1. Run `.\gradlew assembleDebug` to verify build
2. Run `.\gradlew test` to verify tests pass
3. Check for any lint warnings: `.\gradlew lintDebug`
