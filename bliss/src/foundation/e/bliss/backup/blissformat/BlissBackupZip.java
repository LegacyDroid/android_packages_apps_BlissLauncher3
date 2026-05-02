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
 * File:    bliss/src/foundation/e/bliss/backup/blissformat/BlissBackupZip.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.blissformat)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/blissformat/):
 *   ├── BlissBackupZip.java            — ZIP envelope read/write (this file)  ← THIS FILE
 *   ├── BlissPrefsSerializer.java      — LauncherPrefs → prefs.json
 *   └── BlissPrefsDeserializer.java    — prefs.json → LauncherPrefs (+ gesture validation)
 *
 * Purpose:
 *   First-stage decoder/encoder for the .bliss backup ZIP envelope. Wraps
 *   the two entries the helper cares about — prefs.json and layout.xml —
 *   with the 1 MB per-entry safety limit on read. Decoupled from the JSON
 *   schema (BlissPrefsSerializer / BlissPrefsDeserializer own that) so the
 *   envelope and the contents can evolve independently.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.BackupRestoreHelper  — façade orchestrator
 *
 * Plan reference: Plans/Migration05/02-backup-restore-decomp.md (speculative)
 */
package foundation.e.bliss.backup.blissformat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Reads and writes the .bliss backup ZIP envelope. */
public final class BlissBackupZip {

    public static final String PREFS_ENTRY = "prefs.json";
    public static final String LAYOUT_ENTRY = "layout.xml";
    private static final long MAX_ZIP_ENTRY_SIZE = 1024L * 1024L; // 1 MB

    private BlissBackupZip() {
    }

    /** The two byte-string fields the importer cares about; either may be null. */
    public static final class Bundle {
        public final String prefsJson;
        public final String layoutXml;

        Bundle(String prefsJson, String layoutXml) {
            this.prefsJson = prefsJson;
            this.layoutXml = layoutXml;
        }
    }

    /** Writes the prefs.json (required) and optional layout.xml into the stream. */
    public static void write(OutputStream out, String prefsJson, String layoutXml) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out);
        zip.putNextEntry(new ZipEntry(PREFS_ENTRY));
        zip.write(prefsJson.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
        if (layoutXml != null && !layoutXml.isEmpty()) {
            zip.putNextEntry(new ZipEntry(LAYOUT_ENTRY));
            zip.write(layoutXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        zip.close();
    }

    /** Reads the .bliss backup ZIP off the given stream into a Bundle. */
    public static Bundle read(InputStream in) throws IOException {
        String prefsJson = null;
        String layoutXml = null;
        try (ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (!PREFS_ENTRY.equals(name) && !LAYOUT_ENTRY.equals(name)) {
                    zip.closeEntry();
                    continue;
                }
                String content = readEntry(zip);
                if (PREFS_ENTRY.equals(name)) {
                    prefsJson = content;
                } else {
                    layoutXml = content;
                }
                zip.closeEntry();
            }
        }
        return new Bundle(prefsJson, layoutXml);
    }

    private static String readEntry(ZipInputStream zip) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        long totalRead = 0;
        int len;
        while ((len = zip.read(buffer)) > 0) {
            totalRead += len;
            if (totalRead > MAX_ZIP_ENTRY_SIZE) {
                throw new IOException("ZIP entry exceeds size limit");
            }
            baos.write(buffer, 0, len);
        }
        return baos.toString(StandardCharsets.UTF_8.name());
    }
}
