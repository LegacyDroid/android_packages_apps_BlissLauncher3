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
 * File:    bliss/src/foundation/e/bliss/backup/BackupRestoreHelper.java
 * Module:  bliss source-set  (foundation.e.bliss.backup)
 *
 * Tree (foundation/e/bliss/backup/):
 *   ├── BackupRestoreHelper.java            — Bliss-format backup façade  ← THIS FILE
 *   ├── ImportResult.java                   — DTO returned by Lawnchair importFromLawnchair
 *   ├── LawnchairImportHelper.java          — orchestrator for Lawnchair import
 *   ├── LawnchairImportTestReceiver.java    — debug-only adb-broadcast trigger
 *   ├── blissformat/                        — Bliss JSON schema adapters
 *   ├── prefs/                              — Lawnchair pref-key mappers and registry
 *   └── workspace/                          — launcher.db → BlissLauncher favorites table
 *
 * External parser module:
 *   - :bliss-backup-parser owns BlissBackupZip and Lawnchair parser classes.
 *
 * Purpose:
 *   Public façade for Bliss's own settings-and-layout backup format. This
 *   class stays app-side because it needs LauncherPrefs, Dagger access, and
 *   LayoutImportExportHelper. ZIP envelope I/O lives in
 *   :bliss-backup-parser; JSON schema conversion remains beside this helper
 *   because it depends on LauncherPrefs.
 *
 * Calls into:
 *   - foundation.e.bliss.backup.blissformat.BlissBackupZip
 *   - foundation.e.bliss.backup.blissformat.BlissPrefsSerializer
 *   - foundation.e.bliss.backup.blissformat.BlissPrefsDeserializer
 *
 * Consumed by:
 *   - com.android.launcher3.settings.ui.BackupFragment
 *   - com.android.launcher3.settings.controllers.backup.BackupCreateController
 *   - com.android.launcher3.settings.controllers.backup.BackupRestoreController
 */
package foundation.e.bliss.backup;

import android.content.Context;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.dagger.LauncherComponentProvider;
import com.android.launcher3.util.LayoutImportExportHelper;

import foundation.e.bliss.backup.blissformat.BlissBackupZip;
import foundation.e.bliss.backup.blissformat.BlissPrefsDeserializer;
import foundation.e.bliss.backup.blissformat.BlissPrefsSerializer;
import foundation.e.bliss.utils.Logger;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Thin façade orchestrating Bliss-format settings/layout backup and restore.
 */
public class BackupRestoreHelper {

    private static final String TAG = "BackupRestoreHelper";
    private static final Logger LOG = Logger.tag(TAG);

    public interface BackupCallback {
        void onComplete(boolean success);
    }

    /**
     * Writes a .bliss backup ZIP to {@code outputStream} and reports via
     * {@code callback}.
     */
    public static void createBackup(Context context, OutputStream outputStream, BackupCallback callback) {
        LauncherPrefs prefs = LauncherComponentProvider.get(context).getLauncherPrefs();

        String prefsJson = BlissPrefsSerializer.toJson(prefs);
        if (prefsJson == null) {
            callback.onComplete(false);
            return;
        }

        LayoutImportExportHelper.INSTANCE.exportModelDbAsXml(context, layoutXml -> {
            try {
                BlissBackupZip.write(outputStream, prefsJson, layoutXml);
                callback.onComplete(true);
            } catch (Exception e) {
                callback.onComplete(false);
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    /**
     * Reads a .bliss backup ZIP off {@code inputStream}; returns true on success.
     */
    public static boolean restoreFromBackup(Context context, InputStream inputStream) {
        BlissBackupZip.Bundle bundle;
        try {
            bundle = BlissBackupZip.read(inputStream);
        } catch (Exception e) {
            return false;
        }
        if (bundle.prefsJson == null)
            return false;

        LauncherPrefs prefs = LauncherComponentProvider.get(context).getLauncherPrefs();
        if (!BlissPrefsDeserializer.fromJson(bundle.prefsJson, prefs))
            return false;

        if (bundle.layoutXml != null && !bundle.layoutXml.isEmpty()) {
            try {
                LayoutImportExportHelper.INSTANCE.importModelFromXml(context, bundle.layoutXml);
            } catch (SecurityException e) {
                LOG.w("Layout import failed: WRITE_SECURE_SETTINGS not granted. "
                        + "Settings were restored but home screen layout could not be " + "imported on this build.", e);
                // Settings/preferences are already restored above — only the layout
                // import is skipped. The launcher will use its default layout on
                // next reload.
            }
        }
        return true;
    }
}
