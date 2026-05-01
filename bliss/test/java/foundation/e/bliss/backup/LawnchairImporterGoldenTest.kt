/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/backup/LawnchairImporterGoldenTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.backup)
 * Role:    REFACTORED
 *
 * Tree (foundation/e/bliss/backup/):
 *   ├── BackupRestoreHelperTest.kt     — Bliss-format ZIP round-trip test
 *   ├── DataStoreProtoParserTest.kt    — proto-format unit test (pure JVM)
 *   ├── LawnchairImporterGoldenTest.kt — end-to-end importer golden test  ← THIS FILE
 *   ├── SharedPrefsXmlParserTest.kt    — shared-prefs XML parser unit test
 *   └── fixtures/
 *       └── SyntheticBackupBuilder.kt  — produces the .lawnchairbackup ZIP byte payload
 *
 * Purpose:
 *   Linchpin regression test for the importer. Runs the CURRENT monolithic
 *   LawnchairImportHelper against a synthetic .lawnchairbackup fixture and
 *   asserts the resulting Bliss SharedPreferences match a committed canonical
 *   "key=value" dump. After phase 02 (importer decomposition) the test re-runs
 *   and any deviation surfaces as a CI failure.
 *
 *   STATUS — Migration05 follow-up F4 ACTIVATED. The Dagger-graph entry
 *   point (LauncherComponentProvider) is short-circuited via the
 *   foundation.e.bliss.shadows.ShadowLauncherComponentProvider Robolectric
 *   shadow so that LauncherPrefs.get(ctx) returns a real LauncherPrefs
 *   backed by SharedPreferences. The launcher.db (workspace-import) half
 *   of the importer is exercised only via the prefs-side path because
 *   shadowing the layout pipeline (LauncherAppState, DatabaseHelper,
 *   LauncherWidgetHolder, MAIN_EXECUTOR.submit().get() under PAUSED looper
 *   mode) is out of reach for this F-item — see Plan §8 last paragraph.
 *   The committed synthetic.lawnchairbackup intentionally has no launcher.db
 *   entry, so WorkspaceImporter.run is never called; the importer treats a
 *   missing DB as "no layout import" which is exactly the contract here.
 *
 *   The dump helpers below cover only Bliss-prefs state (the part the test
 *   can assert on). The favourites-CSV column has been removed: with no
 *   DB import there are no favourites rows to dump.
 *
 * Plan reference: Plans/Migration05/04-importer-golden-activation.md §3
 */
package foundation.e.bliss.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import foundation.e.bliss.backup.fixtures.SyntheticBackupBuilder
import foundation.e.bliss.shadows.ShadowLauncherComponentProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [35],
    application = android.app.Application::class,
    shadows = [ShadowLauncherComponentProvider::class],
)
class LawnchairImporterGoldenTest {

    /**
     * When true, the test writes the freshly-captured prefs dump to the test resources directory
     * instead of asserting against the committed golden file. Toggle once locally, commit the
     * produced file, set back to false.
     */
    private val CAPTURE_MODE = false

    @After
    fun tearDown() {
        // Reset the shadow's per-app caches so the next test gets a clean
        // LauncherPrefs / fake-component graph.
        ShadowLauncherComponentProvider.resetForTest()
    }

    @Test
    fun importMatchesGolden() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val backupBytes = SyntheticBackupBuilder.buildBackupZip()
        val backup = ByteArrayInputStream(backupBytes)

        val result = LawnchairImportHelper.importFromLawnchair(ctx, backup)
        assertTrue("Import should succeed: ${result.message}", result.success)

        val prefs = dumpBlissPrefsAsCanonicalText(ctx)

        if (CAPTURE_MODE) {
            File("bliss/test/resources/fixtures/synthetic_prefs.txt").writeText(prefs)
            return
        }

        val goldenPrefs = readResource("/fixtures/synthetic_prefs.txt")
        assertEquals(goldenPrefs, prefs)
    }

    /** Canonical Bliss-prefs dump: keys sorted, one "key=value" per line. */
    private fun dumpBlissPrefsAsCanonicalText(ctx: Context): String {
        val sp =
            ctx.getSharedPreferences(
                com.android.launcher3.LauncherFiles.SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE,
            )
        return sp.all.entries
            .sortedBy { it.key }
            .joinToString("\n") { (k, v) -> "$k=${formatValue(v)}" }
    }

    private fun formatValue(v: Any?): String =
        when (v) {
            null -> "null"
            is Set<*> ->
                v.map { it.toString() }.sorted().joinToString(",", prefix = "[", postfix = "]")
            else -> v.toString()
        }

    @Suppress("unused") // referenced by capture-mode CSV row construction
    private fun sha256Prefix8(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hex = md.digest(bytes).joinToString("") { "%02x".format(it) }
        return hex.substring(0, 8)
    }

    private fun readResource(path: String): String {
        val stream = javaClass.getResourceAsStream(path) ?: error("Missing test resource: $path")
        return stream.bufferedReader().use { it.readText() }
    }
}
