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
 * File:    bliss-backup-parser/src/main/java/foundation/e/bliss/backup/parser/ProtobufVarint.java
 * Module:  :bliss-backup-parser
 *
 * Tree (foundation/e/bliss/backup/parser/):
 *   ├── DataStoreProtoParser.java     — parses preferences.preferences_pb (Jetpack DataStore)
 *   ├── LawnchairZip.java             — reads .lawnchairbackup ZIP into a Bundle
 *   ├── ProtobufVarint.java           — bare varint/string helpers (pkg-private)  ← THIS FILE
 *   └── SharedPrefsXmlParser.java     — parses com.android.launcher3.prefs.xml
 *
 * Purpose:
 *   Stateless protobuf wire-format helpers. Bare varint/string/skip; no
 *   Lawnchair semantics. Lifted from LawnchairImportHelper so the protobuf
 *   plumbing is independently testable and reusable. Package-private —
 *   only DataStoreProtoParser inside the same package consumes these.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.parser.DataStoreProtoParser
 */
package foundation.e.bliss.backup.parser;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** Stateless protobuf wire-format helpers (varint / string / skip). */
final class ProtobufVarint {

    private ProtobufVarint() {
    }

    static int readVarint32(ByteBuffer buf) {
        int result = 0;
        int shift = 0;
        while (buf.hasRemaining()) {
            byte b = buf.get();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0)
                break;
            shift += 7;
        }
        return result;
    }

    static long readVarint64(ByteBuffer buf) {
        long result = 0;
        int shift = 0;
        while (buf.hasRemaining()) {
            byte b = buf.get();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0)
                break;
            shift += 7;
        }
        return result;
    }

    static String readString(ByteBuffer buf) {
        int len = readVarint32(buf);
        byte[] bytes = new byte[len];
        buf.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static void skipField(ByteBuffer buf, int wireType) {
        switch (wireType) {
            case 0 : // varint
                readVarint64(buf);
                break;
            case 1 : // fixed64
                buf.position(buf.position() + 8);
                break;
            case 2 : // length-delimited
                int len = readVarint32(buf);
                buf.position(buf.position() + len);
                break;
            case 5 : // fixed32
                buf.position(buf.position() + 4);
                break;
            default :
                // Wire types 3 (start-group) and 4 (end-group) are deprecated and
                // never emitted by Jetpack DataStore; anything else is a malformed
                // protobuf message that we cannot safely skip.
                throw new IllegalStateException("unexpected protobuf wire type: " + wireType);
        }
    }
}
