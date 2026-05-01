/*
 * Copyright (C) 2026 MURENA SAS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
/*
 * File:    bliss/src/foundation/e/bliss/backup/ImportResult.java
 * Module:  bliss source-set  (foundation.e.bliss.backup)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/):
 *   ├── BackupRestoreHelper.java            — Bliss-format settings backup/restore
 *   ├── ImportResult.java                   — DTO returned by importFromLawnchair  ← THIS FILE
 *   ├── LawnchairImportHelper.java          — orchestrator for Lawnchair import
 *   ├── LawnchairImportTestReceiver.java    — debug-only adb-broadcast trigger
 *   ├── blissformat/                        — Bliss .bliss/.zip backup parsers
 *   ├── parser/                             — Lawnchair-format parsers (ZIP, XML, proto)
 *   ├── prefs/                              — pref-key mappers and registry
 *   └── workspace/                          — launcher.db → BlissLauncher favorites table
 *
 * Purpose:
 *   Lifted out of LawnchairImportHelper (Migration04 phase 02) so the
 *   importer's public surface lives in a small, named DTO instead of a
 *   nested static class. Callers (LawnchairImportTestReceiver, the SAF
 *   import flow in SettingsActivity) consume only this type — no other
 *   importer internals leak.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.LawnchairImportHelper   — return type of importFromLawnchair
 *   - foundation.e.bliss.backup.LawnchairImportTestReceiver — log + forceReload after success
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §3, §8 phase 1
 */
package foundation.e.bliss.backup;

/** Result of an import operation. */
public final class ImportResult {
    public final boolean success;
    public final int settingsImported;
    public final boolean layoutImported;
    public final String message;

    public ImportResult(boolean success, int settingsImported, boolean layoutImported, String message) {
        this.success = success;
        this.settingsImported = settingsImported;
        this.layoutImported = layoutImported;
        this.message = message;
    }

    public static ImportResult failure(String message) {
        return new ImportResult(false, 0, false, message);
    }
}
