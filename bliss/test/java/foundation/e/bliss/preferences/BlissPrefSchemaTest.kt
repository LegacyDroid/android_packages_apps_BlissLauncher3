/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/preferences/BlissPrefSchemaTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.preferences)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/preferences/):
 *   └── BlissPrefSchemaTest.kt   — pins ALL_KEYS.size + INTERNAL_KEYS membership  ← THIS FILE
 *
 * Purpose:
 *   Pin the count of registered pref keys so that an accidental rename
 *   or deletion in BlissPrefs.kt fails loudly in CI rather than silently
 *   reverting user settings. Also asserts every INTERNAL_KEYS entry
 *   actually appears in ALL_KEYS (catches typo-on-add of internal keys).
 *
 * Plan reference: Plans/Migration04/03-prefs-registry.md §3.4, §9
 */
package foundation.e.bliss.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlissPrefSchemaTest {

    /**
     * Pin the registry size. If you legitimately add/remove a pref key, update this number in the
     * same commit so reviewers see the schema change explicitly.
     */
    @Test
    fun allKeysSizeIsPinned() {
        // 112 original Bliss prefs + 7 LauncherPrefs raw-string hoists
        // (PRESERVE_LAYOUT_GAPS, GRID_FOLDER_PREVIEW, FIRST_RUN_LAYOUT_CHOICE_DONE,
        //  WORKSPACE_LOCK, ENABLE_TWO_LINE_TOGGLE, HOTSEAT_FACTOR_V1_MIGRATED,
        //  CLOSING_OVERLAY_V1_MIGRATED) + 15 SettingsActivity hoists
        // (PREF_GRID_SIZE, PREF_ICON_BADGING, PREF_DRAWER_FOLDERS_MANAGE,
        //  PREF_ICON_SHAPE, PREF_THEMED_ICONS, PREF_DOCK_BG_HEX, PREF_ACCENT_HEX,
        //  PREF_DRAWER_BG_HEX, PREF_BACKUP_CREATE, PREF_BACKUP_RESTORE,
        //  PREF_LAWNCHAIR_IMPORT, PREF_RESET_APP_RENAMES, PREF_RESTART_LAUNCHER,
        //  PREF_DEVELOPER_OPTIONS, PREF_FIXED_LANDSCAPE_MODE)
        // + 1 Migration04 phase 07 hoist (PREF_FIRST_RUN_STEPS_DONE).
        assertEquals(135, BlissPrefSchema.ALL_KEYS.size)
    }

    @Test
    fun allKeysContainsEveryInternalKey() {
        for (k in BlissPrefSchema.INTERNAL_KEYS) {
            assertTrue("INTERNAL key $k missing from ALL_KEYS", k in BlissPrefSchema.ALL_KEYS)
        }
    }

    @Test
    fun userFacingKeysAndInternalKeysArePartition() {
        assertEquals(
            BlissPrefSchema.ALL_KEYS.size,
            BlissPrefSchema.USER_FACING_KEYS.size + BlissPrefSchema.INTERNAL_KEYS.size,
        )
        assertTrue(
            BlissPrefSchema.USER_FACING_KEYS.intersect(BlissPrefSchema.INTERNAL_KEYS).isEmpty()
        )
    }

    @Test
    fun containsHelperReturnsTrueForRegisteredKey() {
        assertTrue(BlissPrefSchema.contains(BlissPrefs.PREF_PRESERVE_LAYOUT_GAPS))
        assertTrue(BlissPrefSchema.contains(BlissPrefs.PREF_GRID_FOLDER_PREVIEW))
        assertTrue(BlissPrefSchema.contains(BlissPrefs.PREF_FIRST_RUN_LAYOUT_CHOICE_DONE))
    }

    @Test
    fun containsHelperReturnsFalseForUnknownKey() {
        assertTrue(!BlissPrefSchema.contains("pref_definitely_does_not_exist_xyz"))
    }
}
