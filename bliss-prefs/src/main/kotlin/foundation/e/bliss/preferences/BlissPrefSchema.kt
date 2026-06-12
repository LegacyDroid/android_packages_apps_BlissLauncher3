/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss-prefs/src/main/kotlin/foundation/e/bliss/preferences/BlissPrefSchema.kt
 * Module:  :bliss-prefs  (foundation.e.bliss.preferences)
 * Role:    RELOCATED
 *
 * Tree (foundation/e/bliss/preferences/):
 *   ├── BlissPrefs.kt           — registry of every Bliss pref-key string
 *   └── BlissPrefSchema.kt      — reflection-derived ALL_KEYS / INTERNAL_KEYS sets  ← THIS FILE
 *
 * Purpose:
 *   Compile-time-discoverable index of every pref key declared in
 *   BlissPrefs. Walks BlissPrefs::class.java.declaredFields once at class
 *   init and exposes ALL_KEYS, INTERNAL_KEYS (persistence flags / non-UI
 *   maps that should never appear in launcher_preferences.xml), and
 *   USER_FACING_KEYS = ALL_KEYS - INTERNAL_KEYS. The debug-build
 *   self-test assertAllPrefKeysAreRegistered() walks the inflated
 *   PreferenceGroup tree and `require`s every key is registered, catching
 *   typo-on-add at first launch in dev builds. Migration04 §09 relocates
 *   this file alongside BlissPrefs into the leaf `:bliss-prefs` Gradle
 *   module so the registry is physically isolated from any
 *   Android-Context-retaining code.
 *
 * Consumed by:
 *   - com.android.launcher3.settings.SettingsActivity     — debug self-test in onCreatePreferences
 *
 * Plan reference: Plans/Migration04/03-prefs-registry.md §3.4, §6
 *                 Plans/Migration04/09-build-and-rollout.md §2.1
 */
package foundation.e.bliss.preferences

import androidx.preference.Preference
import androidx.preference.PreferenceGroup

/**
 * Compile-time-discoverable index of every Bliss pref key.
 *
 * Built once at class load by reflecting over BlissPrefs::class.java.declaredFields.
 * No runtime cost on the hot path; an O(1) `contains()` check is what the
 * debug self-test uses on each Preference in the inflated tree.
 */
object BlissPrefSchema {

    /** Every "pref_*" key declared in [BlissPrefs]. */
    @JvmField
    val ALL_KEYS: Set<String> =
        BlissPrefs::class
            .java
            .declaredFields
            .asSequence()
            .filter { it.type == String::class.java }
            .filter { it.name.startsWith("PREF_") }
            .mapNotNull {
                it.isAccessible = true
                it.get(null) as? String
            }
            .toSet()

    /**
     * Keys that exist for persistence / migration tracking and are NEVER
     * shown in `launcher_preferences.xml`. The debug XML self-test
     * excludes these from the must-appear-in-XML check.
     */
    @JvmField
    val INTERNAL_KEYS: Set<String> = setOf(
        BlissPrefs.PREF_DRAWER_FOLDERS_MIGRATED_V1,
        BlissPrefs.PREF_FIRST_RUN_LAYOUT_CHOICE_DONE,
        BlissPrefs.PREF_FIRST_RUN_STEPS_DONE,
        BlissPrefs.PREF_DRAWER_FOLDERS_MAP,
        BlissPrefs.PREF_RECENT_COLORS,
        BlissPrefs.PREF_FOLDER_COLOR_OVERRIDES,
        BlissPrefs.PREF_APP_NAME_OVERRIDES,
        BlissPrefs.PREF_HIDDEN_APPS,
        BlissPrefs.PREF_HOTSEAT_FACTOR_V1_MIGRATED,
        BlissPrefs.PREF_CLOSING_OVERLAY_V1_MIGRATED,
    )

    /** Keys that *should* appear in `launcher_preferences.xml`. */
    @JvmField
    val USER_FACING_KEYS: Set<String> = ALL_KEYS - INTERNAL_KEYS

    /** True if [key] is declared in [BlissPrefs]. O(1). */
    @JvmStatic
    fun contains(key: String): Boolean = key in ALL_KEYS

    /**
     * Debug self-test: walk every [Preference] under [root] and require
     * its `key` (if non-null) to be declared in [BlissPrefs]. Throws
     * [IllegalArgumentException] on the first stranger so the dev sees
     * the typo immediately.
     *
     * Wire-up site: SettingsActivity.LauncherSettingsFragment.onCreatePreferences.
     */
    @JvmStatic
    fun assertAllPrefKeysAreRegistered(root: PreferenceGroup) {
        walk(root).forEach { p ->
            val key = p.key ?: return@forEach
            require(key in ALL_KEYS || key in INTERNAL_KEYS) {
                "Pref XML references undeclared key: $key (not in BlissPrefs)"
            }
        }
    }

    /** Depth-first traversal of every Preference under [root], including [root] itself. */
    private fun walk(root: PreferenceGroup): Sequence<Preference> = sequence {
        yield(root)
        for (i in 0 until root.preferenceCount) {
            val child = root.getPreference(i)
            if (child is PreferenceGroup) {
                yieldAll(walk(child))
            } else {
                yield(child)
            }
        }
    }
}
