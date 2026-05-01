<!--
  Plan reference: Plans/Migration04/08-observability-and-testing.md §3.4 / §5
  Fixture binaries do not carry the Form-A header (they are non-source); this
  README documents their provenance so a future maintainer can regenerate them
  byte-for-byte from a known input.
-->

# Test fixtures

| File | How produced | Consumed by |
|------|---------------|-------------|
| `synthetic.lawnchairbackup` | `./gradlew buildSyntheticBackup` — runs `foundation.e.bliss.backup.fixtures.SyntheticBackupBuilder.main`, which writes a ZIP containing `com.android.launcher3.prefs.xml` (the canonical `SYNTHETIC_PREFS` map serialised as Android SharedPreferences XML) and `preferences.preferences_pb` (the same map serialised as a Jetpack DataStore Preferences protobuf). No `launcher.db` entry — the importer treats a missing DB as "no layout import", which is exactly what the prefs-side golden test asserts on (see `synthetic_prefs.txt`). | `LawnchairImporterGoldenTest` (now active under `ShadowLauncherComponentProvider`) and indirectly the parser tests which build the byte payloads in-memory rather than re-reading this ZIP. |
| `synthetic_prefs.txt` | Captured one-shot from `LawnchairImporterGoldenTest` with `CAPTURE_MODE = true` — the canonical alphabetised `key=value` dump of the BlissLauncher SharedPreferences after the importer has run against `synthetic.lawnchairbackup`. Each line maps to one mapper-registry rule that fired. Re-capture by flipping the flag, running the test once, and committing the regenerated file. | `LawnchairImporterGoldenTest.importMatchesGolden` — `assertEquals(golden, actual)`. |

## Regenerating

```
ANDROID_HOME=/data/MurenaOS/android-sdk ./gradlew buildSyntheticBackup
```

The output is reproducible because `SYNTHETIC_PREFS` is a `linkedMapOf` — iteration order is preserved across runs. If you need to add a new entry, append it to `SYNTHETIC_PREFS` (do NOT reorder), regenerate the binary, and update the relevant goldens.

## Out of scope (deferred to a future migration wave)

`synthetic_favorites.csv` is intentionally absent. Activating the workspace-import half of the golden test under Robolectric requires shadowing `LauncherWidgetHolder.newInstance`, `UserCache.INSTANCE`, `LauncherAppState.getInstance`, and `Executors.MAIN_EXECUTOR` (the `submit().get()` pattern in `PostImportLayoutFix.apply` deadlocks under PAUSED looper mode). Each of those is its own Dagger entry-point that the current test classpath cannot cleanly fake. See `LawnchairImporterGoldenTest`'s file header for the full blocker list and `Plans/Migration05/04-importer-golden-activation.md §3.2` for the documented decision.
