/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/adapters/LauncherDebugFlags.kt
 * Module:  bliss root app source-set
 * Role:    App-owned DebugFlags adapter backed by Launcher3 BuildConfig.
 *
 * Consumes:
 *   - :bliss-core-contracts DebugFlags
 *
 * Usage structure:
 *   Extracted modules receive DebugFlags through constructors/factories. This
 *   app-root adapter is the only place that should bridge those modules to
 *   com.android.launcher3.BuildConfig.
 */
package foundation.e.bliss.adapters

import com.android.launcher3.BuildConfig
import foundation.e.bliss.core.runtime.DebugFlags

object LauncherDebugFlags : DebugFlags {
    override val isDebugDevice: Boolean
        get() = BuildConfig.IS_DEBUG_DEVICE
}
