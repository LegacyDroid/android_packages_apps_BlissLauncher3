/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    bliss-animation-safety/src/main/kotlin/foundation/e/bliss/animations/safety/FloatGuard.kt
 * Module:  :bliss-animation-safety
 * Role:    Float validator and safe-divide helper for animator-derived values.
 *
 * Consumed by:
 *   - com.android.launcher3.folder.FolderAnimationData
 *
 * Dependency rules:
 *   - Uses DebugFlags from :bliss-core-contracts (not app BuildConfig).
 *   - Must not import com.android.launcher3.*.
 */
package foundation.e.bliss.animations.safety

import android.util.Log
import foundation.e.bliss.core.runtime.DebugFlags

object FloatGuard {

    private const val TAG = "FloatGuard"

    private object ReleaseDefaults : DebugFlags {
        override val isDebugDevice: Boolean = false
    }

    @Volatile private var debugFlags: DebugFlags = ReleaseDefaults

    internal var debugOverride: Boolean? = null

    @JvmStatic
    fun configure(flags: DebugFlags) {
        debugFlags = flags
    }

    private val debugMode: Boolean
        get() = debugOverride ?: debugFlags.isDebugDevice

    fun requireAllFinite(name: String, vararg floats: Float): Boolean {
        for (i in floats.indices) {
            if (!floats[i].isFinite()) {
                fail(name, i, floats[i])
                return false
            }
        }
        return true
    }

    fun safeDivide(
        name: String,
        numerator: Float,
        denominator: Float,
        fallback: Float = 1f,
    ): Float {
        if (denominator <= 0f || !denominator.isFinite() || !numerator.isFinite()) {
            fail(name, -1, denominator)
            return fallback
        }
        return numerator / denominator
    }

    private fun fail(name: String, idx: Int, value: Float) {
        val msg = "FloatGuard: non-finite at $name [idx=$idx, value=$value]"
        check(!debugMode) { msg }
        Log.w(TAG, msg)
    }
}
