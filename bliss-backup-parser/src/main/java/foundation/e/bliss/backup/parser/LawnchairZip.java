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
 * File:    bliss-backup-parser/src/main/java/foundation/e/bliss/backup/parser/LawnchairZip.java
 * Module:  :bliss-backup-parser
 *
 * Tree (foundation/e/bliss/backup/parser/):
 *   ├── DataStoreProtoParser.java     — parses preferences.preferences_pb (Jetpack DataStore)
 *   ├── LawnchairZip.java             — reads .lawnchairbackup ZIP into a Bundle  ← THIS FILE
 *   ├── ProtobufVarint.java           — bare varint/string helpers (pkg-private)
 *   └── SharedPrefsXmlParser.java     — parses com.android.launcher3.prefs.xml
 *
 * Purpose:
 *   First-stage decoder for a Lawnchair backup. Cracks open the ZIP and
 *   pulls out the three entries the importer cares about — the SharedPrefs
 *   XML, the DataStore protobuf, and the launcher.db SQLite blob. Other
 *   ZIP entries (info.pb, screenshot.png, wallpaper.png) are silently
 *   ignored. The 5 MB per-entry safety limit is enforced here.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.LawnchairImportHelper  — orchestrator
 */
package foundation.e.bliss.backup.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Reads the entries the Lawnchair importer cares about out of a backup ZIP. */
public final class LawnchairZip {

    private static final String SHARED_PREFS_ENTRY = "com.android.launcher3.prefs.xml";
    private static final String DATASTORE_ENTRY = "preferences.preferences_pb";
    private static final String DB_ENTRY = "launcher.db";
    private static final long MAX_ENTRY_SIZE = 5L * 1024 * 1024; // 5 MB (db can be larger)

    private LawnchairZip() {
    }

    /** The three byte-arrays the importer cares about; any may be null. */
    public static final class Bundle {
        public final byte[] sharedPrefs; // com.android.launcher3.prefs.xml
        public final byte[] dataStore; // preferences.preferences_pb
        public final byte[] db; // launcher.db

        Bundle(byte[] sharedPrefs, byte[] dataStore, byte[] db) {
            this.sharedPrefs = sharedPrefs;
            this.dataStore = dataStore;
            this.db = db;
        }

        public boolean isEmpty() {
            return sharedPrefs == null && dataStore == null && db == null;
        }
    }

    /** Reads the .lawnchairbackup ZIP off the given stream into a Bundle. */
    public static Bundle read(InputStream in) throws IOException {
        byte[] sharedPrefsData = null;
        byte[] datastoreData = null;
        byte[] dbData = null;
        try (ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (SHARED_PREFS_ENTRY.equals(name)) {
                    sharedPrefsData = readEntry(zip);
                } else if (DATASTORE_ENTRY.equals(name)) {
                    datastoreData = readEntry(zip);
                } else if (DB_ENTRY.equals(name)) {
                    dbData = readEntry(zip);
                }
                zip.closeEntry();
            }
        }
        return new Bundle(sharedPrefsData, datastoreData, dbData);
    }

    private static byte[] readEntry(ZipInputStream zip) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        long totalRead = 0;
        int len;
        while ((len = zip.read(buffer)) > 0) {
            totalRead += len;
            if (totalRead > MAX_ENTRY_SIZE) {
                throw new IOException("ZIP entry exceeds size limit");
            }
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }
}
