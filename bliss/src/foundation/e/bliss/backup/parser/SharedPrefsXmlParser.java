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
 * File:    bliss/src/foundation/e/bliss/backup/parser/SharedPrefsXmlParser.java
 * Module:  bliss source-set  (foundation.e.bliss.backup.parser)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/parser/):
 *   ├── DataStoreProtoParser.java     — parses preferences.preferences_pb (Jetpack DataStore)
 *   ├── LawnchairZip.java             — reads .lawnchairbackup ZIP into a Bundle
 *   ├── ProtobufVarint.java           — bare varint/string helpers (pkg-private)
 *   └── SharedPrefsXmlParser.java     — parses com.android.launcher3.prefs.xml  ← THIS FILE
 *
 * Purpose:
 *   Parses the Lawnchair SharedPreferences XML entry into a Map<String,Object>
 *   keyed by pref name. Booleans/ints/longs/floats become their boxed Java
 *   types; <string> children of a <set> become a HashSet<String>. The parser
 *   is a thin wrapper around android.util.Xml#newPullParser, which means it
 *   must run under Robolectric (or on-device) — pure JVM is not enough.
 *
 * Consumed by:
 *   - foundation.e.bliss.backup.LawnchairImportHelper  — orchestrator
 *
 * Plan reference: Plans/Migration04/02-importer-decomposition.md §4 (lifted from lines 315–380)
 */
package foundation.e.bliss.backup.parser;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Parses Lawnchair's SharedPreferences XML format into a typed Map. */
public final class SharedPrefsXmlParser {

    private SharedPrefsXmlParser() {
    }

    /**
     * Parses {@code data} as a SharedPreferences XML document and copies each named
     * entry into {@code prefs}. Tag types recognised: {@code boolean,
     * int, long, float, string, set} (the standard SharedPreferences set).
     */
    public static void parse(byte[] data, Map<String, Object> prefs) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new ByteArrayInputStream(data), "UTF-8");

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                String name = parser.getAttributeValue(null, "name");
                if (name == null) {
                    eventType = parser.next();
                    continue;
                }
                switch (tag) {
                    case "boolean" :
                        String boolVal = parser.getAttributeValue(null, "value");
                        if (boolVal != null) {
                            prefs.put(name, Boolean.parseBoolean(boolVal));
                        }
                        break;
                    case "int" :
                        String intVal = parser.getAttributeValue(null, "value");
                        if (intVal != null) {
                            prefs.put(name, Integer.parseInt(intVal));
                        }
                        break;
                    case "long" :
                        String longVal = parser.getAttributeValue(null, "value");
                        if (longVal != null) {
                            prefs.put(name, Long.parseLong(longVal));
                        }
                        break;
                    case "float" :
                        String floatVal = parser.getAttributeValue(null, "value");
                        if (floatVal != null) {
                            prefs.put(name, Float.parseFloat(floatVal));
                        }
                        break;
                    case "string" :
                        String strVal = parser.nextText();
                        prefs.put(name, strVal);
                        break;
                    case "set" :
                        Set<String> set = new HashSet<>();
                        while (parser.next() != XmlPullParser.END_TAG || !"set".equals(parser.getName())) {
                            if (parser.getEventType() == XmlPullParser.START_TAG && "string".equals(parser.getName())) {
                                set.add(parser.nextText());
                            }
                        }
                        prefs.put(name, set);
                        break;
                }
            }
            eventType = parser.next();
        }
    }
}
