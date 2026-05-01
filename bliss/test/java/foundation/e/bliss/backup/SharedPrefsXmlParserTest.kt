/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/backup/SharedPrefsXmlParserTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.backup)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/):
 *   ├── DataStoreProtoParserTest.kt    — proto-format unit test (pure JVM)
 *   ├── LawnchairImporterGoldenTest.kt — end-to-end importer golden test (Robolectric, currently @Ignore)
 *   ├── SharedPrefsXmlParserTest.kt    — shared-prefs XML parser unit test  ← THIS FILE
 *   └── fixtures/
 *       └── SyntheticBackupBuilder.kt  — produces the .lawnchairbackup ZIP byte payload
 *
 * Purpose:
 *   Targeted test of LawnchairImportHelper.parseSharedPrefsXml. The parser
 *   uses android.util.Xml.newPullParser() so the test runs under Robolectric
 *   (the only Android-API call in the parser). Builds a small SharedPrefs
 *   XML in memory via SyntheticBackupBuilder and asserts each value type
 *   parses to the expected JVM type/value.
 *
 *   Phase 02 (importer decomposition) will pull parseSharedPrefsXml onto a
 *   dedicated parser class; this test will drop the reflection invocation.
 *
 * Plan reference: Plans/Migration04/08-observability-and-testing.md §3.6
 */
package foundation.e.bliss.backup

import foundation.e.bliss.backup.fixtures.SyntheticBackupBuilder
import foundation.e.bliss.backup.parser.SharedPrefsXmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Uses a vanilla [android.app.Application] (NOT com.android.launcher3.LauncherApplication) because
 * the production app's onCreate calls into QuickstepProcessInitializer → com.android.internal.
 * protolog.ProtoLog, a hidden-system-server class that the framework.jar shipped under bliss/libs
 * does not expose. Wiring a stub for it would mean either editing the production initializer (out
 * of scope for phase 08, that's phase 02) or shipping a Robolectric shadow for the system_server
 * class graph (heavyweight, deferred). The stub Application is enough — the parser this test
 * exercises only needs `android.util.Xml` which Robolectric's pure-Android shadows already provide.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class SharedPrefsXmlParserTest {

    @Test
    fun parsesAllValueTypes() {
        val xml = SyntheticBackupBuilder.buildSharedPrefsXml(SyntheticBackupBuilder.SYNTHETIC_PREFS)
        val parsed = invokeParser(xml)

        assertEquals(SyntheticBackupBuilder.SYNTHETIC_PREFS.size, parsed.size)
        assertEquals(true, parsed["show_icon_labels_on_home_screen"])
        assertEquals(1.0f, parsed["pref_iconSizeFactor"] as Float, 0.0001f)
        assertEquals(4, parsed["pref_workspaceColumns"])
        assertEquals(123456789012L, parsed["long_value"])
        assertEquals("{\"type\":\"openAppDrawer\"}", parsed["double_tap_gesture_handler"])
        @Suppress("UNCHECKED_CAST") val set = parsed["hidden-app-set"] as Set<String>
        assertEquals(2, set.size)
        assertTrue(set.contains("com.example.app1"))
        assertTrue(set.contains("com.example.app2"))
    }

    @Test
    fun emptyXmlYieldsEmptyMap() {
        val xml = SyntheticBackupBuilder.buildSharedPrefsXml(emptyMap())
        val parsed = invokeParser(xml)
        assertEquals(0, parsed.size)
    }

    private fun invokeParser(xml: ByteArray): Map<String, Any?> {
        val result = LinkedHashMap<String, Any>()
        SharedPrefsXmlParser.parse(xml, result)
        return result
    }
}
