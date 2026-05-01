/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * File:    bliss/src/foundation/e/bliss/compat/quickstep/QuickStepContractCompat.kt
 * Module:  bliss source-set, withQuickstep flavour  (foundation.e.bliss.compat.quickstep)
 * Role:    RELOCATED
 *
 * Tree (foundation/e/bliss/compat/quickstep/):
 *   ├── ActivityTaskManagerCompat.kt    — ActivityTaskManager.getService() shim
 *   └── QuickStepContractCompat.kt      — QuickStepContract rounded-corner shim  ← THIS FILE
 *
 * Purpose:
 *   Compatibility helper for `QuickStepContract` methods that depend on
 *   internal framework APIs which may not exist on all framework builds
 *   (`ScreenDecorationsUtils.supportsRoundedCornersOnWindows` /
 *   `getWindowCornerRadius`). Reflection now routes through
 *   `ReflectionGate`; the resource-fallback branches are unchanged.
 *
 *   Compiled only into the `withQuickstep` flavour via the source-set
 *   filter in `build.gradle`.
 *
 * Consumed by: see ~11 call-sites across `quickstep/` enumerated in the
 *   call-site sweep table.
 *
 * Calls into:
 *   - foundation.e.bliss.compat.ReflectionGate
 *   - com.android.launcher3.util.Themes (fallback corner radius)
 *
 * Plan reference: Plans/Migration04/01-compat-platform.md §4
 */
package foundation.e.bliss.compat.quickstep

import android.content.Context
import android.content.res.Resources
import com.android.launcher3.util.Themes
import foundation.e.bliss.compat.ReflectionGate
import foundation.e.bliss.utils.Logger

object QuickStepContractCompat {

    private val LOG = Logger.tag("QSContractCompat")

    private const val SCREEN_DECO_CLASS = "com.android.internal.policy.ScreenDecorationsUtils"

    private val sNativeMethodAvailable: Boolean = run {
        val m =
            ReflectionGate.lookupMethod(
                SCREEN_DECO_CLASS,
                "supportsRoundedCornersOnWindows",
                Resources::class.java,
            )
        if (m == null) {
            LOG.i(
                "ScreenDecorationsUtils.supportsRoundedCornersOnWindows(Resources)" +
                    " not available, using resource fallback"
            )
            false
        } else {
            m.returnType == java.lang.Boolean.TYPE
        }
    }

    private val sCornerRadiusMethodAvailable: Boolean = run {
        val m =
            ReflectionGate.lookupMethod(
                SCREEN_DECO_CLASS,
                "getWindowCornerRadius",
                Context::class.java,
            )
        if (m == null) {
            LOG.i(
                "ScreenDecorationsUtils.getWindowCornerRadius(Context)" +
                    " not available, using resource fallback"
            )
            false
        } else {
            m.returnType == java.lang.Float.TYPE
        }
    }

    /**
     * Safe replacement for `QuickStepContract.supportsRoundedCornersOnWindows(Resources)`. When the
     * underlying framework API is available, delegates to it; otherwise reads the
     * `config_supportsRoundedCornersOnWindows` system resource directly, defaulting to `true` if
     * the resource is also absent (most modern devices support this).
     */
    @JvmStatic
    fun supportsRoundedCornersOnWindows(resources: Resources): Boolean {
        if (sNativeMethodAvailable) {
            return com.android.systemui.shared.system.QuickStepContract
                .supportsRoundedCornersOnWindows(resources)
        }
        // Fallback: read the system config resource directly.
        val resId =
            resources.getIdentifier("config_supportsRoundedCornersOnWindows", "bool", "android")
        if (resId != 0) {
            return resources.getBoolean(resId)
        }
        // Default: most modern devices support rounded corners on windows.
        return true
    }

    /**
     * Safe replacement for `QuickStepContract.getWindowCornerRadius(Context)`. When the underlying
     * framework API (`ScreenDecorationsUtils.getWindowCornerRadius`) is available, delegates to
     * `QuickStepContract.getWindowCornerRadius(Context)`; otherwise reads the
     * `config_windowCornerRadius` system dimen resource directly, defaulting to
     * [Themes.getDialogCornerRadius] if the resource is absent.
     */
    @JvmStatic
    fun getWindowCornerRadius(context: Context): Float {
        if (sCornerRadiusMethodAvailable) {
            return com.android.systemui.shared.system.QuickStepContract.getWindowCornerRadius(
                context
            )
        }
        // Fallback: read the system config resource directly.
        val resources = context.resources
        val resId = resources.getIdentifier("config_windowCornerRadius", "dimen", "android")
        if (resId != 0) {
            return resources.getDimensionPixelSize(resId).toFloat()
        }
        // Default: use dialog corner radius as a reasonable approximation.
        return Themes.getDialogCornerRadius(context)
    }
}
