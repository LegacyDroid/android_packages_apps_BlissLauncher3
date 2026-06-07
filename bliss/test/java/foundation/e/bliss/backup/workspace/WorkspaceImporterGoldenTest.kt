/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/backup/workspace/WorkspaceImporterGoldenTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.backup.workspace)
 * Role:    NEW
 *
 * Purpose:
 *   audit01/09 — golden-lock the workspace half of the Lawnchair importer
 *   that LawnchairImporterGoldenTest deliberately skips. Builds a synthetic
 *   Lawnchair launcher.db blob via SyntheticBackupBuilder.builder()
 *   .withWorkspaceItem(...).buildLauncherDbBytes(), feeds it through
 *   WorkspaceImporter.run, and asserts the resulting Bliss favorites table
 *   matches a committed CSV golden file.
 *
 *   Path 2 from the audit plan: bypasses the LawnchairImportHelper top-level
 *   façade, calls WorkspaceImporter.run directly. The test asserts on the
 *   rows written by insertIntoBlissDb regardless of whether the trailing
 *   PostImportLayoutFix.apply pipeline succeeds — that pipeline depends on
 *   the full Dagger graph (LauncherAppState, LauncherModel) which the
 *   coordination contract is covered separately by
 *   PostImportLayoutFixCoordinationTest.
 *
 * Plan reference: Plans/Audits/audit01/09-importer-golden-test-workspace.md
 */
package foundation.e.bliss.backup.workspace

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.launcher3.LauncherFiles
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.model.DatabaseHelper
import foundation.e.bliss.backup.fixtures.SyntheticBackupBuilder
import foundation.e.bliss.shadows.ShadowLauncherComponentProvider
import java.io.File
import java.io.FileWriter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
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
class WorkspaceImporterGoldenTest {

    /**
     * When true, the test writes the freshly-captured favorites dump to the test resources
     * directory instead of asserting against the committed golden file. Toggle once locally, commit
     * the produced file, set back to false. Mirrors the pattern in LawnchairImporterGoldenTest.
     */
    private val captureMode = false

    private val gridName = "test_grid"
    private val targetDbFile = "launcher_$gridName.db"

    @Before
    fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        // Pin the grid name so WorkspaceImporter resolves the target db file
        // via the pref-only path (avoids LauncherAppState / Dagger lookup).
        val prefs = ShadowLauncherComponentProvider.get(ctx).launcherPrefs
        prefs.put(LauncherPrefs.GRID_NAME, gridName)
        // Pre-create the downgrade-schema sentinel so DatabaseHelper.onOpen
        // skips handleOneTimeDataUpgrade (which calls UserCache via Dagger and
        // would throw under the test's fake LauncherAppComponent).
        val schemaFile = ctx.getFileStreamPath("downgrade_schema.json")
        schemaFile.parentFile?.mkdirs()
        FileWriter(schemaFile).use {
            it.write("{\"version\":${DatabaseHelper.SCHEMA_VERSION},\"downgrade\":{}}")
        }
        // Wipe any stale target DB from a previous test run.
        ctx.getDatabasePath(targetDbFile).delete()
    }

    @After
    fun tearDown() {
        ShadowLauncherComponentProvider.resetForTest()
    }

    @Test
    fun import_preserves_cells_and_screens_for_known_fixture() {
        val payload =
            SyntheticBackupBuilder.builder()
                .withWorkspaceItem(screen = 0, cellX = 0, cellY = 0, packageName = "com.example.a")
                .withWorkspaceItem(screen = 0, cellX = 2, cellY = 3, packageName = "com.example.b")
                .withWorkspaceItem(screen = 1, cellX = 4, cellY = 1, packageName = "com.example.c")
                .buildLauncherDbBytes()

        val context: Context = ApplicationProvider.getApplicationContext()
        // Run the importer. Wrapped here to swallow any failure that comes
        // from the trailing PostImportLayoutFix.apply pipeline — that
        // pipeline needs the real Dagger LauncherAppState; the rows we
        // assert on are committed in the preceding insertIntoBlissDb call.
        try {
            foundation.e.bliss.backup.workspace.WorkspaceImporter.run(context, payload)
        } catch (t: Throwable) {
            // Expected for the trailing pipeline; insertIntoBlissDb already
            // committed its transaction by the time anything downstream
            // could throw.
        }

        val rows = readFavoritesAsCsv(context).trim()

        if (captureMode) {
            val outFile = File("bliss/test/resources/golden/favorites_after_import.csv")
            outFile.parentFile?.mkdirs()
            outFile.writeText(rows + "\n")
            return
        }

        val golden = readResource("/golden/favorites_after_import.csv").trim()
        assertEquals(golden, rows)
    }

    /**
     * Dump the BlissLauncher favorites table for the target grid as CSV: header row first, then one
     * row per record, columns in a fixed order to make diff output stable.
     */
    private fun readFavoritesAsCsv(ctx: Context): String {
        val helper =
            DatabaseHelper(
                ctx,
                targetDbFile,
                { user ->
                    (ctx.getSystemService(Context.USER_SERVICE) as android.os.UserManager)
                        .getSerialNumberForUser(user)
                },
                { /* no-op onEmptyDbCreate */ },
            )
        try {
            val db = helper.readableDatabase
            val cols =
                listOf(
                    "_id",
                    "title",
                    "intent",
                    "container",
                    "screen",
                    "cellX",
                    "cellY",
                    "spanX",
                    "spanY",
                    "itemType",
                    "appWidgetProvider",
                    "rank",
                    "appWidgetId",
                    "restored",
                    "options",
                    "appWidgetSource",
                )
            val sb = StringBuilder()
            sb.append(cols.joinToString(",")).append('\n')
            db.rawQuery(
                    "SELECT ${cols.joinToString(",")} FROM favorites " +
                        "ORDER BY container, screen, cellY, cellX",
                    null,
                )
                .use { c ->
                    while (c.moveToNext()) {
                        val parts =
                            cols.indices.map { i ->
                                if (c.isNull(i)) "" else csvEscape(c.getString(i))
                            }
                        sb.append(parts.joinToString(",")).append('\n')
                    }
                }
            return sb.toString()
        } finally {
            helper.close()
        }
    }

    private fun csvEscape(v: String): String {
        return if (v.contains(',') || v.contains('"') || v.contains('\n')) {
            "\"" + v.replace("\"", "\"\"") + "\""
        } else {
            v
        }
    }

    private fun readResource(path: String): String {
        val stream = javaClass.getResourceAsStream(path) ?: error("Missing test resource: $path")
        return stream.bufferedReader().use { it.readText() }
    }

    @Suppress("unused") // Reserved for inspection during capture-mode debugging.
    private fun describePrefs(ctx: Context): String {
        val sp =
            ctx.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
        return sp.all.entries.joinToString("\n") { "${it.key}=${it.value}" }
    }
}
