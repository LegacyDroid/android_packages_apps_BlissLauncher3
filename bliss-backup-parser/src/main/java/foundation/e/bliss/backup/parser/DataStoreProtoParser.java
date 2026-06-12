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
 * File:    bliss-backup-parser/src/main/java/foundation/e/bliss/backup/parser/DataStoreProtoParser.java
 * Module:  :bliss-backup-parser
 *
 * Tree (foundation/e/bliss/backup/parser/):
 *   ├── DataStoreProtoParser.java     — parses preferences.preferences_pb  ← THIS FILE
 *   ├── LawnchairZip.java             — reads .lawnchairbackup ZIP into a Bundle
 *   ├── ProtobufVarint.java           — bare varint/string helpers (pkg-private)
 *   └── SharedPrefsXmlParser.java     — parses com.android.launcher3.prefs.xml
 *
 * Purpose:
 *   Decodes a Jetpack DataStore Preferences protobuf payload into a typed
 *   Map<String,Object>. Wire format (PreferencesProto):
 *     message PreferencesProto { map<string, Value> preferences = 1; }
 *     message Value { oneof { bool=1; float=2; int32=3; int64=4;
 *                             string=5; double=6; StringSet=7; } }
 *   Pure-JVM (no Android types) — covered by DataStoreProtoParserTest in
 *   the app test source-set until parser tests are moved with their fixtures.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.LawnchairImportHelper  — orchestrator
 *
 * Calls into:
 *   - foundation.e.bliss.backup.parser.ProtobufVarint  — wire helpers
 */
package foundation.e.bliss.backup.parser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Parses a Jetpack DataStore Preferences protobuf payload into a typed Map. */
public final class DataStoreProtoParser {

    private DataStoreProtoParser() {
    }

    /**
     * Parses {@code data} as a PreferencesProto and copies each (key, value) pair
     * into {@code prefs}. Malformed entries stop the parse silently — partial maps
     * are kept (matches the pre-Migration04 behaviour).
     */
    public static void parse(byte[] data, Map<String, Object> prefs) {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        try {
            while (buf.hasRemaining()) {
                int tag = ProtobufVarint.readVarint32(buf);
                int fieldNumber = tag >>> 3;
                int wireType = tag & 0x7;

                if (fieldNumber == 1 && wireType == 2) {
                    // Map entry (length-delimited sub-message)
                    int length = ProtobufVarint.readVarint32(buf);
                    int limit = buf.position() + length;
                    parseMapEntry(buf, limit, prefs);
                    buf.position(limit);
                } else {
                    ProtobufVarint.skipField(buf, wireType);
                }
            }
        } catch (Exception e) {
            // Malformed — keep what we got. Caller logs.
        }
    }

    private static void parseMapEntry(ByteBuffer buf, int limit, Map<String, Object> prefs) {
        String key = null;
        Object value = null;
        while (buf.position() < limit) {
            int tag = ProtobufVarint.readVarint32(buf);
            int fieldNumber = tag >>> 3;
            int wireType = tag & 0x7;
            if (fieldNumber == 1 && wireType == 2) {
                // Key (string)
                key = ProtobufVarint.readString(buf);
            } else if (fieldNumber == 2 && wireType == 2) {
                // Value (embedded message)
                int valueLen = ProtobufVarint.readVarint32(buf);
                int valueLimit = buf.position() + valueLen;
                value = parseValue(buf, valueLimit);
                buf.position(valueLimit);
            } else {
                ProtobufVarint.skipField(buf, wireType);
            }
        }
        if (key != null && value != null) {
            prefs.put(key, value);
        }
    }

    private static Object parseValue(ByteBuffer buf, int limit) {
        if (buf.position() >= limit)
            return null;
        int tag = ProtobufVarint.readVarint32(buf);
        int fieldNumber = tag >>> 3;
        // wireType tag bits are kept for parity with the original switch but
        // not currently used outside StringSet — preserved verbatim.
        switch (fieldNumber) {
            case 1 : // boolean (varint)
                return ProtobufVarint.readVarint32(buf) != 0;
            case 2 : // float (fixed32)
                return buf.getFloat();
            case 3 : // int32 (varint)
                return ProtobufVarint.readVarint32(buf);
            case 4 : // int64 (varint)
                return ProtobufVarint.readVarint64(buf);
            case 5 : // string (length-delimited)
                return ProtobufVarint.readString(buf);
            case 6 : // double (fixed64)
                return buf.getDouble();
            case 7 : // StringSet (length-delimited sub-message)
                int setLen = ProtobufVarint.readVarint32(buf);
                int setLimit = buf.position() + setLen;
                Set<String> set = new HashSet<>();
                while (buf.position() < setLimit) {
                    int innerTag = ProtobufVarint.readVarint32(buf);
                    if ((innerTag >>> 3) == 1 && (innerTag & 0x7) == 2) {
                        set.add(ProtobufVarint.readString(buf));
                    } else {
                        ProtobufVarint.skipField(buf, innerTag & 0x7);
                    }
                }
                return set;
            default :
                return null;
        }
    }
}
