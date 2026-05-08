# :bliss-folders-data

Owns Room persistence for drawer folders.

Owned packages:
- `foundation.e.bliss.folders.db`

Public API:
- `DrawerFolderDao`
- `DrawerFolderDatabase`
- `DrawerFolderEntity`
- `DrawerFolderItemEntity`
- `DrawerFolderWithItems`

Allowed dependencies:
- AndroidX Room runtime/KTX/compiler
- Kotlin standard library
- Android database/runtime APIs required by Room

Forbidden dependencies:
- `com.android.launcher3.*`
- Launcher model classes such as `AppInfo`, `FolderInfo`, `WorkspaceItemInfo`
- Launcher UI classes
- Preference or backup orchestration

App-root adapters and consumers:
- `foundation.e.bliss.folders.DrawerFolderService` maps persisted rows to launcher model objects.
- `foundation.e.bliss.folders.ui.DrawerFolderManageActivity` manages user edits through the service.
- `foundation.e.bliss.allapps.BlissAlphabeticalAppsList` renders folder-backed drawer rows through app-owned adapters.

Validation:
```bash
ANDROID_HOME=/data/android-sdk ./gradlew :bliss-folders-data:assembleDebug
```
