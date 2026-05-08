/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss-core-contracts/src/main/kotlin/foundation/e/bliss/core/prefs/StringPreferenceWriter.kt
 * Module:  :bliss-core-contracts
 * Role:    Minimal write-only string preference contract.
 *
 * Owned by:
 *   - :bliss-core-contracts
 *
 * Consumed by:
 *   - App-root LauncherPrefs adapter.
 *   - Future extracted modules that need string preferences without importing
 *     com.android.launcher3.LauncherPrefs.
 *
 * Dependency rules:
 *   - May use Kotlin/JDK types only.
 *   - Must not import com.android.launcher3.* or Android framework UI classes.
 */
package foundation.e.bliss.core.prefs

interface StringPreferenceWriter {
    fun putString(key: String, value: String)
}
