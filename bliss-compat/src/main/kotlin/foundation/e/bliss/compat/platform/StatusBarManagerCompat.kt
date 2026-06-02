/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss-compat/src/main/kotlin/foundation/e/bliss/compat/platform/StatusBarManagerCompat.kt
 * Module:  :bliss-compat
 * Role:    Safe status-bar reflection shim replacing direct getMethod() calls.
 *
 * Consumed by:
 *   - foundation.e.bliss.gestures.GestureHandler (app root)
 *   - com.android.launcher3.uioverrides.touchcontrollers.StatusBarTouchController (quickstep)
 *
 * Dependency rules:
 *   - Routes all reflection through ReflectionGate.
 *   - Must not import com.android.launcher3.*.
 */
package foundation.e.bliss.compat.platform

import android.content.Context
import foundation.e.bliss.compat.ReflectionGate

object StatusBarManagerCompat {
    private const val SERVICE_STATUSBAR = "statusbar"

    @JvmStatic
    fun expandNotificationsPanel(context: Context): Boolean =
        invokeNoArg(context, "expandNotificationsPanel")

    @JvmStatic
    fun expandSettingsPanel(context: Context): Boolean =
        invokeNoArg(context, "expandSettingsPanel")

    @JvmStatic
    fun toggleRecentApps(context: Context): Boolean =
        invokeNoArg(context, "toggleRecentApps")

    private fun invokeNoArg(context: Context, methodName: String): Boolean {
        val service = context.getSystemService(SERVICE_STATUSBAR) ?: return false
        return try {
            val method = ReflectionGate.lookupMethod(service.javaClass.name, methodName)
                ?: return false
            method.invoke(service)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
