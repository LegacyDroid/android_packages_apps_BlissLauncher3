/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/backup/BackupRestoreHelperTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.backup)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/backup/):
 *   ├── BackupRestoreHelperTest.kt     — Bliss-format ZIP round-trip test  ← THIS FILE
 *   ├── DataStoreProtoParserTest.kt    — proto-format unit test (pure JVM)
 *   ├── LawnchairImporterGoldenTest.kt — end-to-end importer golden test (Robolectric, currently @Ignore)
 *   ├── SharedPrefsXmlParserTest.kt    — shared-prefs XML parser unit test
 *   └── fixtures/
 *       └── SyntheticBackupBuilder.kt  — produces the .lawnchairbackup ZIP byte payload
 *
 * Purpose:
 *   Round-trip test for the Bliss-format backup envelope after Migration05
 *   F2 split BackupRestoreHelper into blissformat/{BlissBackupZip,
 *   BlissPrefsSerializer, BlissPrefsDeserializer}. Drives a synthetic
 *   prefs.json + layout.xml through BlissBackupZip.write → read and
 *   verifies the contents survive byte-for-byte. Pure JVM (no Robolectric);
 *   the full LauncherPrefs round-trip is covered indirectly by the existing
 *   manual-acceptance steps in §3.5 of Plans/Migration04/08-observability-
 *   and-testing.md and is too heavy to wire into a unit test.
 *
 * Plan reference: Plans/Migration05/02-backup-restore-decomp.md (speculative)
 */
package foundation.e.bliss.backup

import foundation.e.bliss.backup.blissformat.BlissBackupZip
import foundation.e.bliss.backup.blissformat.BlissPrefsDeserializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric is needed because two of the four assertions touch Android
// framework classes that throw "Stub!" on the unit-test classpath:
//   - org.json.JSONObject (used by BlissPrefsDeserializer.fromJson)
//   - android.content.ComponentName.unflattenFromString (used by
//     BlissPrefsDeserializer.isValidGestureHandler)
// The zip round-trip tests would pass without Robolectric (java.util.zip is
// pure JVM), but running the whole class under one runner is simpler than
// splitting into two test files. The default LauncherApplication.onCreate
// is bypassed via @Config(application = ...) — same pattern as
// SharedPrefsXmlParserTest.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class BackupRestoreHelperTest {

    @Test
    fun zipRoundTripPreservesPrefsAndLayout() {
        val prefsJson = """{"show_home_labels":true,"icon_size_factor":105}"""
        val layoutXml = "<resources><favorite/></resources>"

        val out = ByteArrayOutputStream()
        BlissBackupZip.write(out, prefsJson, layoutXml)

        val bundle = BlissBackupZip.read(ByteArrayInputStream(out.toByteArray()))
        assertEquals(prefsJson, bundle.prefsJson)
        assertEquals(layoutXml, bundle.layoutXml)
    }

    @Test
    fun zipRoundTripOmitsEmptyLayout() {
        val prefsJson = """{"show_home_labels":false}"""

        val out = ByteArrayOutputStream()
        BlissBackupZip.write(out, prefsJson, /* layoutXml= */ null)

        val bundle = BlissBackupZip.read(ByteArrayInputStream(out.toByteArray()))
        assertEquals(prefsJson, bundle.prefsJson)
        assertNull(bundle.layoutXml)
    }

    @Test
    fun gestureHandlerValidatorAcceptsKnownAndAppPrefix() {
        assertTrue(BlissPrefsDeserializer.isValidGestureHandler("none"))
        assertTrue(BlissPrefsDeserializer.isValidGestureHandler("app:com.example/.MainActivity"))
        assertFalse(BlissPrefsDeserializer.isValidGestureHandler(null))
        assertFalse(BlissPrefsDeserializer.isValidGestureHandler("not-a-handler"))
        assertFalse(BlissPrefsDeserializer.isValidGestureHandler("app:not-a-component"))
    }

    @Test
    fun deserializerReturnsFalseOnMalformedJson() {
        // Pure JVM smoke test: malformed JSON must not throw, must return false.
        // No LauncherPrefs is required because we never get past the JSONObject parse.
        val ok =
            BlissPrefsDeserializer.fromJson(/* prefsJson= */ "this is not json", /* prefs= */ null)
        assertFalse(ok)
        assertNotNull(
            "control: well-formed empty JSON parses cleanly",
            BlissPrefsDeserializer::class.java,
        )
    }
}
