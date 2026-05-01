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
 * Bliss touchpoint(s) (Migration04):
 *   - Imports foundation.e.bliss.compat.desktop.DesktopFlagsCompat (relocated by Migration04)
 *   - Imports foundation.e.bliss.compat.desktop.DesktopModeStatusCompat (relocated by Migration04)
 *     — Plan ref: Plans/Migration04/01-compat-platform.md §4
 *
 * The body of this file otherwise tracks AOSP. Keep diffs minimal so a
 * future origin/a16 rebase merges cleanly.
 */
package com.android.launcher3.desktop

import android.app.WindowConfiguration.ACTIVITY_TYPE_STANDARD
import android.app.WindowConfiguration.WINDOWING_MODE_FREEFORM
import android.content.Context
import foundation.e.bliss.compat.desktop.DesktopFlagsCompat
import android.window.RemoteTransition
import android.window.TransitionFilter
import android.window.TransitionFilter.CONTAINER_ORDER_TOP
import com.android.internal.jank.Cuj
import com.android.launcher3.desktop.DesktopAppLaunchTransition.AppLaunchType
import com.android.launcher3.util.Executors.MAIN_EXECUTOR
import com.android.quickstep.SystemUiProxy
import foundation.e.bliss.compat.desktop.DesktopModeStatusCompat

/** Manages transitions related to app launches in Desktop Mode. */
class DesktopAppLaunchTransitionManager(
    private val context: Context,
    private val systemUiProxy: SystemUiProxy,
) {
    private var remoteWindowLimitUnminimizeTransition: RemoteTransition? = null

    /**
     * Register a [RemoteTransition] supporting Desktop app launches, and window limit
     * minimizations.
     */
    fun registerTransitions() {
        if (!shouldRegisterTransitions()) {
            return
        }
        val unminimizeRunner = DesktopAppLaunchTransition(
                context,
                AppLaunchType.UNMINIMIZE,
                Cuj.CUJ_DESKTOP_MODE_APP_LAUNCH_FROM_INTENT,
                MAIN_EXECUTOR,
            )
        remoteWindowLimitUnminimizeTransition = if (android.os.Build.VERSION.SDK_INT >= 36) {
            RemoteTransition(unminimizeRunner, "DesktopWindowLimitUnminimize")
        } else {
            RemoteTransition(unminimizeRunner)
        }
        systemUiProxy.registerRemoteTransition(
            remoteWindowLimitUnminimizeTransition,
            buildAppLaunchFilter(),
        )
    }

    /**
     * Unregister the [RemoteTransition] supporting Desktop app launches and window limit
     * minimizations.
     */
    fun unregisterTransitions() {
        if (!shouldRegisterTransitions()) {
            return
        }
        systemUiProxy.unregisterRemoteTransition(remoteWindowLimitUnminimizeTransition)
        remoteWindowLimitUnminimizeTransition = null
    }

    private fun shouldRegisterTransitions(): Boolean =
        DesktopModeStatusCompat.canEnterDesktopMode(context) &&
            DesktopFlagsCompat.enableDesktopAppLaunchTransitionsBugfix()

    companion object {
        private fun buildAppLaunchFilter(): TransitionFilter {
            val openRequirement =
                TransitionFilter.Requirement().apply {
                    mActivityType = ACTIVITY_TYPE_STANDARD
                    mWindowingMode = WINDOWING_MODE_FREEFORM
                    mModes = DesktopAppLaunchTransition.LAUNCH_CHANGE_MODES
                    mMustBeTask = true
                    mOrder = CONTAINER_ORDER_TOP
                }
            return TransitionFilter().apply {
                mTypeSet = DesktopAppLaunchTransition.LAUNCH_CHANGE_MODES
                mRequirements = arrayOf(openRequirement)
            }
        }
    }
}
