/*
 * Copyright (C) 2025 MURENA SAS
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
 * File:    bliss/src/foundation/e/bliss/backup/LawnchairImportHelper.java
 * Module:  bliss source-set  (foundation.e.bliss.backup)
 * Role:    REFACTORED
 *
 * Tree (foundation/e/bliss/backup/):
 *   ├── BackupRestoreHelper.java            — Bliss-format settings backup/restore
 *   ├── ImportResult.java                   — DTO returned by importFromLawnchair
 *   ├── LawnchairImportHelper.java          — orchestrator (this file)  ← THIS FILE
 *   ├── LawnchairImportTestReceiver.java    — debug-only adb-broadcast trigger
 *   ├── blissformat/                        — Bliss .bliss/.zip backup parsers
 *   ├── parser/                             — Lawnchair-format parsers (ZIP, XML, proto)
 *   ├── prefs/                              — pref-key mappers and registry
 *   └── workspace/                          — launcher.db → BlissLauncher favorites table
 *
 * Purpose:
 *   Orchestrates Lawnchair-backup import. Pre-Migration04 this file was
 *   1976 lines doing seven jobs at once; Migration04 phase 02 lifted each
 *   concern into the parser/, prefs/, and workspace/ packages. What
 *   remains here is a thin façade that sequences four delegate calls:
 *   ZIP read → XML+proto parse → pref-mapping → workspace import.
 *
 * Calls into:
 *   - foundation.e.bliss.backup.parser.LawnchairZip
 *   - foundation.e.bliss.backup.parser.SharedPrefsXmlParser
 *   - foundation.e.bliss.backup.parser.DataStoreProtoParser
 *   - foundation.e.bliss.backup.prefs.PrefMapperRegistry
 *   - foundation.e.bliss.backup.workspace.WorkspaceImporter
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.LawnchairImportTestReceiver
 *   - com.android.launcher3.settings.SettingsActivity (Lawnchair-import flow)
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §3, §8 phase 8
 */
package foundation.e.bliss.backup;

import android.content.Context;

import foundation.e.bliss.backup.parser.DataStoreProtoParser;
import foundation.e.bliss.backup.parser.LawnchairZip;
import foundation.e.bliss.backup.parser.SharedPrefsXmlParser;
import foundation.e.bliss.backup.prefs.PrefMapperRegistry;
import foundation.e.bliss.backup.workspace.WorkspaceImporter;
import foundation.e.bliss.utils.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Thin façade orchestrating the four-stage Lawnchair-backup import. */
public final class LawnchairImportHelper {

    private static final String TAG = "LawnchairImport";
    private static final Logger LOG = Logger.tag(TAG);

    private LawnchairImportHelper() {
    }

    /** Import settings + workspace from a .lawnchairbackup ZIP. */
    public static ImportResult importFromLawnchair(Context context, InputStream inputStream) {
        LawnchairZip.Bundle bundle;
        try {
            bundle = LawnchairZip.read(inputStream);
        } catch (Exception e) {
            LOG.w("Failed to read Lawnchair backup ZIP", e);
            return ImportResult.failure("Invalid backup file");
        }

        if (bundle.isEmpty())
            return ImportResult.failure("No Lawnchair data found in backup");

        LOG.i("Lawnchair import: prefs=" + (bundle.sharedPrefs != null) + " proto=" + (bundle.dataStore != null)
                + " db=" + (bundle.db != null));

        Map<String, Object> lawnchairPrefs = new HashMap<>();
        if (bundle.sharedPrefs != null)
            parsePrefs(bundle.sharedPrefs, lawnchairPrefs, /* xml */ true);
        if (bundle.dataStore != null)
            parsePrefs(bundle.dataStore, lawnchairPrefs, /* xml */ false);

        int imported = lawnchairPrefs.isEmpty() ? 0 : PrefMapperRegistry.applyAll(context, lawnchairPrefs);

        List<String> skipped = computeSkipped(lawnchairPrefs);
        if (!skipped.isEmpty()) {
            LOG.i("Lawnchair keys with no BlissLauncher mapping (" + skipped.size() + "): " + skipped);
        }

        boolean layoutImported = bundle.db != null && WorkspaceImporter.run(context, bundle.db);
        return summarise(imported, layoutImported, skipped);
    }

    private static void parsePrefs(byte[] data, Map<String, Object> dst, boolean xml) {
        try {
            if (xml)
                SharedPrefsXmlParser.parse(data, dst);
            else
                DataStoreProtoParser.parse(data, dst);
        } catch (Exception e) {
            LOG.w(xml ? "Failed to parse SharedPreferences XML" : "Failed to parse DataStore protobuf", e);
        }
    }

    /** Lawnchair keys present in {@code src} that this importer doesn't map. */
    private static List<String> computeSkipped(Map<String, Object> src) {
        Set<String> recognised = PrefMapperRegistry.recognisedKeys();
        Set<String> internal = PrefMapperRegistry.internalKeys();
        List<String> skipped = new ArrayList<>();
        for (String k : src.keySet()) {
            if (recognised.contains(k))
                continue;
            if (internal.contains(k))
                continue;
            if (k.startsWith("launcher.") || k.startsWith("migration_"))
                continue;
            skipped.add(k);
        }
        Collections.sort(skipped);
        return skipped;
    }

    private static ImportResult summarise(int imported, boolean layoutImported, List<String> skipped) {
        boolean success = imported > 0 || layoutImported;
        StringBuilder msg = new StringBuilder("Imported ");
        if (imported > 0)
            msg.append(imported).append(" settings");
        if (layoutImported) {
            if (imported > 0)
                msg.append(" and ");
            msg.append("home screen layout");
        }
        msg.append(" from Lawnchair");
        if (!skipped.isEmpty()) {
            msg.append(" (").append(skipped.size()).append(" Lawnchair-only setting")
                    .append(skipped.size() == 1 ? "" : "s").append(" not applicable to BlissLauncher — see logcat tag ")
                    .append(TAG).append(")");
        }
        return new ImportResult(success, imported, layoutImported, msg.toString());
    }
}
