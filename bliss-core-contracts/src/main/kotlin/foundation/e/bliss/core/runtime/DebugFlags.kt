/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss-core-contracts/src/main/kotlin/foundation/e/bliss/core/runtime/DebugFlags.kt
 * Module:  :bliss-core-contracts
 * Role:    Runtime debug-state contract for extracted Bliss modules.
 *
 * Owned by:
 *   - :bliss-core-contracts
 *
 * Consumed by:
 *   - App-root adapters backed by com.android.launcher3.BuildConfig.
 *   - Future network/provider modules that must gate debug-only behavior
 *     without importing app BuildConfig.
 *
 * Dependency rules:
 *   - May use Kotlin/JDK types only.
 *   - Must not import com.android.launcher3.* or Android framework UI classes.
 */
package foundation.e.bliss.core.runtime

interface DebugFlags {
    val isDebugDevice: Boolean
}
