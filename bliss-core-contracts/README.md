# :bliss-core-contracts

Owns tiny interfaces shared between extracted Bliss modules and the root app.

Owned packages:
- `foundation.e.bliss.core.logging`
- `foundation.e.bliss.core.prefs`
- `foundation.e.bliss.core.runtime`

Public API:
- `BlissLogger`
- `BlissLoggerFactory`
- `DebugFlags`
- `StringPreferenceReader`
- `StringPreferenceWriter`

Allowed dependencies:
- Kotlin standard library
- JDK types

Forbidden dependencies:
- Android framework UI classes
- `com.android.launcher3.*`
- `LauncherPrefs`
- Network, database, or filesystem implementations

App-root adapters and consumers:
- `foundation.e.bliss.utils.Logger`
- `foundation.e.bliss.adapters.LauncherDebugFlags`
- `foundation.e.bliss.adapters.LauncherPrefsStringAdapter`
- Extracted modules that need logging/debug/preference contracts

Validation:
```bash
ANDROID_HOME=/data/android-sdk ./gradlew :bliss-core-contracts:assembleDebug
```
