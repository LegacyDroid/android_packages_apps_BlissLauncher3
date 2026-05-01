/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/backup/DataStoreProtoParserTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.backup)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/):
 *   ├── DataStoreProtoParserTest.kt    — proto-format unit test (pure JVM)  ← THIS FILE
 *   ├── LawnchairImporterGoldenTest.kt — end-to-end importer golden test (Robolectric, currently @Ignore)
 *   ├── SharedPrefsXmlParserTest.kt    — shared-prefs XML parser unit test
 *   └── fixtures/
 *       └── SyntheticBackupBuilder.kt  — produces the .lawnchairbackup ZIP byte payload
 *
 * Purpose:
 *   Targeted test of LawnchairImportHelper.parseDataStoreProto. Builds a
 *   small DataStore Preferences protobuf in memory via SyntheticBackupBuilder
 *   and asserts each value round-trips through the importer's parser. Pure
 *   JVM (no Robolectric) — the parser is byte-level varint munging with no
 *   Android dependencies.
 *
 *   Phase 02 (importer decomposition) will move parseDataStoreProto onto a
 *   dedicated parser class with package-visibility; this test will then drop
 *   the reflection invocation. Until then, reflection is the cleanest way to
 *   exercise a private static method without refactoring the production file.
 *
 * Plan reference: Plans/Migration04/08-observability-and-testing.md §3.6
 */
package foundation.e.bliss.backup

import foundation.e.bliss.backup.fixtures.SyntheticBackupBuilder
import foundation.e.bliss.backup.parser.DataStoreProtoParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreProtoParserTest {

    @Test
    fun parsesSingleVarintInt() {
        // Synthetic proto with one entry: { "answer": 42 (int32) }.
        val proto = SyntheticBackupBuilder.buildDataStoreProto(mapOf("answer" to 42))
        val parsed = invokeParser(proto)
        assertEquals(1, parsed.size)
        assertEquals(42, parsed["answer"])
    }

    @Test
    fun roundTripsMixedValueTypes() {
        val proto =
            SyntheticBackupBuilder.buildDataStoreProto(SyntheticBackupBuilder.SYNTHETIC_PREFS)
        val parsed = invokeParser(proto)

        assertEquals(SyntheticBackupBuilder.SYNTHETIC_PREFS.size, parsed.size)
        assertEquals(true, parsed["show_icon_labels_on_home_screen"])
        assertEquals(1.0f, parsed["pref_iconSizeFactor"] as Float, 0.0001f)
        assertEquals(4, parsed["pref_workspaceColumns"])
        assertEquals(123456789012L, parsed["long_value"])
        assertEquals("{\"type\":\"openAppDrawer\"}", parsed["double_tap_gesture_handler"])
        @Suppress("UNCHECKED_CAST") val set = parsed["hidden-app-set"] as Set<String>
        assertTrue(set.contains("com.example.app1"))
        assertTrue(set.contains("com.example.app2"))
    }

    private fun invokeParser(proto: ByteArray): Map<String, Any?> {
        val result = LinkedHashMap<String, Any>()
        DataStoreProtoParser.parse(proto, result)
        return result
    }
}
