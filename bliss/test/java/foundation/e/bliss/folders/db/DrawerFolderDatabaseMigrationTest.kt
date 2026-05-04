/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/folders/db/DrawerFolderDatabaseMigrationTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.folders.db)
 * Role:    NEW
 *
 * Purpose:
 *   Pin Audit01 #02: ensure the canonical Room schema for
 *   DrawerFolderDatabase v1 is checked in and matches the entity model. This
 *   test will also be the host for migrate_N_to_M_preserves_user_folders()
 *   cases as schema versions land — see plan §"Test plan".
 *
 * Plan reference: Plans/Audits/audit01/02-room-destructive-migration.md
 */
package foundation.e.bliss.folders.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class DrawerFolderDatabaseMigrationTest {

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            DrawerFolderDatabase::class.java,
        )

    /**
     * Audit01 #02: createDatabase asserts that
     * `bliss/schemas/foundation.e.bliss.folders.db.DrawerFolderDatabase/1.json` is present,
     * parseable, and matches the entity model. Without the schema JSON committed, this test fails
     * with a FileNotFoundException — that's the regression signal for "someone
     * bumped @Database(version) without regenerating the schema export".
     *
     * When schema version 2 lands, append migrate_1_to_2_preserves_user_folders() here. See plan
     * §"Test plan".
     */
    @Test
    @Ignore(
        "Audit01 #02 BLOCKED — needs maintainer approval. " +
            "Production source change is in DrawerFolderDatabase.kt and the schema is exported to " +
            "bliss/schemas/foundation.e.bliss.folders.db.DrawerFolderDatabase/1.json, but " +
            "MigrationTestHelper reads schemas via Context.assets — and AGP does not surface the " +
            "unit-test sourceSet's assets.srcDirs to Robolectric. See close log in " +
            "Plans/Audits/audit01/02-room-destructive-migration.md for the 3 maintainer-decision " +
            "options (move to androidTest, switch to File-based check, or custom Robolectric " +
            "asset resolver). Re-enable once an option is picked."
    )
    fun version1_schema_present() {
        helper.createDatabase(TEST_DB, 1).close()
    }

    private companion object {
        const val TEST_DB = "drawer_folders_test.db"
    }
}
