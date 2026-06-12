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
 * File:    bliss/src/foundation/e/bliss/compat/quickstep/ActivityTaskManagerCompat.kt
 * Module:  bliss source-set, withQuickstep flavour  (foundation.e.bliss.compat.quickstep)
 * Role:    RELOCATED
 *
 * Tree (foundation/e/bliss/compat/quickstep/):
 *   ├── ActivityTaskManagerCompat.kt    — ActivityTaskManager.getService() shim  ← THIS FILE
 *   └── QuickStepContractCompat.kt      — QuickStepContract rounded-corner shim
 *
 * Purpose:
 *   Compatibility helper for `ActivityTaskManager.getService()`, a hidden
 *   system API available on /e/OS and LineageOS custom frameworks but
 *   sometimes absent on stock AOSP builds. Reflection now routes through
 *   `ReflectionGate`; the static cache of method-availability survives.
 *
 *   Compiled only into the `withQuickstep` flavour via the source-set
 *   filter in `build.gradle` — see Plans/Migration04/01-compat-platform.md §2.
 *
 * Consumed by: see ~12 call-sites across `quickstep/` enumerated in the
 *   call-site sweep table.
 *
 * Calls into:
 *   - foundation.e.bliss.compat.ReflectionGate
 *
 * Plan reference: Plans/Migration04/01-compat-platform.md §4
 */
package foundation.e.bliss.compat.quickstep

import android.app.ActivityTaskManager
import android.app.IActivityTaskManager
import com.android.systemui.shared.system.TaskStackChangeListener
import com.android.systemui.shared.system.TaskStackChangeListeners
import foundation.e.bliss.compat.ReflectionGate
import foundation.e.bliss.utils.Logger

object ActivityTaskManagerCompat {

    private val LOG = Logger.tag("ATMCompat")

    /**
     * Cached result of the runtime availability check. Performed once at class load via
     * ReflectionGate so that call sites can short-circuit without repeated overhead.
     */
    private val sServiceAvailable: Boolean = run {
        val getServiceMethod =
            ReflectionGate.lookupMethod("android.app.ActivityTaskManager", "getService")
        if (getServiceMethod == null) {
            LOG.i("ActivityTaskManager.getService() not available on this framework")
            false
        } else {
            // Verify the return type matches what callers expect.
            IActivityTaskManager::class.java.isAssignableFrom(getServiceMethod.returnType)
        }
    }

    /**
     * Returns `true` if `ActivityTaskManager.getService()` is available in the current runtime. Use
     * this to guard call sites that depend on `IActivityTaskManager`, including calls to
     * `TaskStackChangeListeners` which internally invokes this API.
     */
    @JvmStatic fun isServiceAvailable(): Boolean = sServiceAvailable

    /**
     * Returns the `IActivityTaskManager` service, or `null` if the API is not available on this
     * framework build.
     */
    @JvmStatic
    fun getService(): IActivityTaskManager? {
        if (!sServiceAvailable) return null
        return ActivityTaskManager.getService()
    }

    /**
     * Registers a [TaskStackChangeListener] if the underlying `ActivityTaskManager` service is
     * available. On frameworks that lack the service (e.g. stock AOSP debug builds), this is a safe
     * no-op.
     *
     * @throws SecurityException if the caller lacks MANAGE_ACTIVITY_TASKS permission
     */
    @JvmStatic
    fun registerTaskStackListener(listener: TaskStackChangeListener) {
        if (!sServiceAvailable) return
        TaskStackChangeListeners.getInstance().registerTaskStackListener(listener)
    }

    /**
     * Unregisters a previously registered [TaskStackChangeListener]. Safe to call even if the
     * service is unavailable (no-op in that case).
     *
     * @throws SecurityException if the caller lacks MANAGE_ACTIVITY_TASKS permission
     */
    @JvmStatic
    fun unregisterTaskStackListener(listener: TaskStackChangeListener) {
        if (!sServiceAvailable) return
        TaskStackChangeListeners.getInstance().unregisterTaskStackListener(listener)
    }
}
