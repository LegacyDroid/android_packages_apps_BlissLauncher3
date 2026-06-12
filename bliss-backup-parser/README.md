# :bliss-backup-parser

Owns backup archive and primitive preference-file parsing only.

Owned packages:
- `foundation.e.bliss.backup.parser`
- `foundation.e.bliss.backup.blissformat`

Public API:
- `LawnchairZip`
- `SharedPrefsXmlParser`
- `DataStoreProtoParser`
- `BlissBackupZip`

Allowed dependencies:
- Java/Kotlin standard library
- Android XML utility used by `SharedPrefsXmlParser`

Forbidden dependencies:
- `com.android.launcher3.*`
- `LauncherPrefs`
- `LauncherSettings`
- `LauncherAppState`
- `LauncherComponentProvider`
- Launcher database writes or model mutation

App-root adapters and consumers:
- `foundation.e.bliss.backup.LawnchairImportHelper` orchestrates Lawnchair import and persistence.
- `foundation.e.bliss.backup.BackupRestoreHelper` orchestrates Bliss backup restore and launcher layout import.

Validation:
```bash
ANDROID_HOME=/data/android-sdk ./gradlew :bliss-backup-parser:assembleDebug
```
