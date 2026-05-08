/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/adapters/LauncherPrefsStringAdapter.kt
 * Module:  bliss root app source-set
 * Role:    App-owned string preference adapter backed by LauncherPrefs.
 *
 * Consumes:
 *   - :bliss-core-contracts StringPreferenceReader / StringPreferenceWriter
 *   - :bliss-prefs BlissPrefs key constants
 *
 * Usage structure:
 *   Extracted modules ask for string preferences by stable BlissPrefs key.
 *   This adapter maps those stable keys to LauncherPrefs.Item instances so
 *   library modules do not import com.android.launcher3.LauncherPrefs.
 */
package foundation.e.bliss.adapters

import com.android.launcher3.LauncherPrefs
import foundation.e.bliss.core.prefs.StringPreferenceReader
import foundation.e.bliss.core.prefs.StringPreferenceWriter
import foundation.e.bliss.preferences.BlissPrefs

class LauncherPrefsStringAdapter(private val launcherPrefs: LauncherPrefs) :
    StringPreferenceReader, StringPreferenceWriter {

    override fun getString(key: String, defaultValue: String): String =
        when (key) {
            BlissPrefs.PREF_SEARCH_PROVIDER -> launcherPrefs.get(LauncherPrefs.SEARCH_PROVIDER)
            BlissPrefs.PREF_WEB_SUGGESTION_URL ->
                launcherPrefs.get(LauncherPrefs.WEB_SUGGESTION_URL)
            BlissPrefs.PREF_WEB_SUGGESTION_NAME ->
                launcherPrefs.get(LauncherPrefs.WEB_SUGGESTION_NAME)
            else -> defaultValue
        }

    override fun putString(key: String, value: String) {
        when (key) {
            BlissPrefs.PREF_SEARCH_PROVIDER ->
                launcherPrefs.put(LauncherPrefs.SEARCH_PROVIDER, value)
            BlissPrefs.PREF_WEB_SUGGESTION_URL ->
                launcherPrefs.put(LauncherPrefs.WEB_SUGGESTION_URL, value)
            BlissPrefs.PREF_WEB_SUGGESTION_NAME ->
                launcherPrefs.put(LauncherPrefs.WEB_SUGGESTION_NAME, value)
        }
    }
}
