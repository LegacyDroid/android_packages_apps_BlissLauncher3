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
 * File:    bliss-backup-parser/src/main/java/foundation/e/bliss/backup/parser/SharedPrefsXmlParser.java
 * Module:  :bliss-backup-parser
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
 */
package foundation.e.bliss.backup.parser;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Parses Lawnchair's SharedPreferences XML format into a typed Map. */
public final class SharedPrefsXmlParser {

    private static final String ATTR_NAME = "name";
    private static final String ATTR_VALUE = "value";
    private static final String TAG_BOOLEAN = "boolean";
    private static final String TAG_INT = "int";
    private static final String TAG_LONG = "long";
    private static final String TAG_FLOAT = "float";
    private static final String TAG_STRING = "string";
    private static final String TAG_SET = "set";

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
                handleStartTag(parser, prefs);
            }
            eventType = parser.next();
        }
    }

    /**
     * Reads one START_TAG; if it carries a {@code name=} attribute, dispatches by
     * tag.
     */
    private static void handleStartTag(XmlPullParser parser, Map<String, Object> prefs) throws Exception {
        String name = parser.getAttributeValue(null, ATTR_NAME);
        if (name == null) {
            return;
        }
        dispatchTag(parser, parser.getName(), name, prefs);
    }

    /** Routes a named pref entry to its type-specific reader. */
    private static void dispatchTag(XmlPullParser parser, String tag, String name, Map<String, Object> prefs)
            throws Exception {
        switch (tag) {
            case TAG_BOOLEAN :
                putIfPresent(parser, prefs, name, Boolean::parseBoolean);
                break;
            case TAG_INT :
                putIfPresent(parser, prefs, name, Integer::parseInt);
                break;
            case TAG_LONG :
                putIfPresent(parser, prefs, name, Long::parseLong);
                break;
            case TAG_FLOAT :
                putIfPresent(parser, prefs, name, Float::parseFloat);
                break;
            case TAG_STRING :
                prefs.put(name, parser.nextText());
                break;
            case TAG_SET :
                prefs.put(name, readStringSet(parser));
                break;
            default :
                // Unknown SharedPreferences tag; ignore for forward-compatibility.
                break;
        }
    }

    /**
     * Reads the {@code value=} attribute, parses it via {@code parse}, and stores
     * it under {@code name}.
     */
    private static <T> void putIfPresent(XmlPullParser parser, Map<String, Object> prefs, String name,
            Function<String, T> parse) {
        String raw = parser.getAttributeValue(null, ATTR_VALUE);
        if (raw != null) {
            prefs.put(name, parse.apply(raw));
        }
    }

    /**
     * Consumes the body of a {@code <set>} element, collecting nested
     * {@code <string>} children.
     */
    private static Set<String> readStringSet(XmlPullParser parser) throws Exception {
        Set<String> set = new HashSet<>();
        while (parser.next() != XmlPullParser.END_TAG || !TAG_SET.equals(parser.getName())) {
            if (parser.getEventType() == XmlPullParser.START_TAG && TAG_STRING.equals(parser.getName())) {
                set.add(parser.nextText());
            }
        }
        return set;
    }
}
