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
 * File:    bliss-compat/src/main/kotlin/foundation/e/bliss/compat/desktop/DesktopModeStatusCompat.kt
 * Module:  :bliss-compat  (foundation.e.bliss.compat.desktop)
 * Role:    RELOCATED
 *
 * Tree (foundation/e/bliss/compat/desktop/):
 *   ├── DesktopFlagsCompat.kt           — DesktopMode/Experience/window flag getters
 *   └── DesktopModeStatusCompat.kt      — DesktopModeStatus method shim  ← THIS FILE
 *
 * Purpose:
 *   Compatibility wrapper for `DesktopModeStatus` from the WM Shell shared
 *   library. `DesktopModeStatus` internally references
 *   `android.window.DesktopExperienceFlags` which only exists on API 36+.
 *   All methods return `false` on API < 36, since desktop windowing doesn't
 *   exist on Android 15 and below. Body unchanged from the M03 file at
 *   `com.android.launcher3.util.DesktopModeStatusCompat`.
 *
 * Consumed by: see ~22 call-sites across `quickstep/` listed in the call-site
 *   sweep table (Plans/Migration04/01-compat-platform.md §4).
 *
 * Calls into:
 *   - foundation.e.bliss.compat.BuildCompat
 *
 * Plan reference: Plans/Migration04/01-compat-platform.md §3.4
 */
@file:SuppressLint("NewApi")

package foundation.e.bliss.compat.desktop

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RequiresApi
import foundation.e.bliss.compat.BuildCompat

object DesktopModeStatusCompat {

    @JvmStatic
    fun canEnterDesktopMode(context: Context): Boolean =
        safe { Api36Impl.canEnterDesktopMode(context) }

    @JvmStatic
    fun canEnterDesktopModeOrShowAppHandle(context: Context): Boolean =
        safe { Api36Impl.canEnterDesktopModeOrShowAppHandle(context) }

    @JvmStatic
    fun enterDesktopByDefaultOnFreeformDisplay(context: Context): Boolean =
        safe { Api36Impl.enterDesktopByDefaultOnFreeformDisplay(context) }

    @JvmStatic
    fun enableMultipleDesktops(context: Context): Boolean =
        safe { Api36Impl.enableMultipleDesktops(context) }

    @JvmStatic
    fun useRoundedCorners(context: Context): Boolean =
        safe { Api36Impl.useRoundedCorners() }

    @JvmStatic
    fun useRoundedCorners(): Boolean =
        safe { Api36Impl.useRoundedCorners() }

    /**
     * Runs [block] only on API 36+, returning false when the underlying WM Shell /
     * `android.window.DesktopExperienceFlags` symbol is absent on a mismatched framework
     * (e.g. a stock emulator image) rather than crashing. Desktop windowing simply stays off.
     */
    private inline fun safe(block: () -> Boolean): Boolean =
        if (BuildCompat.isAtLeastBaklava) {
            try {
                block()
            } catch (e: LinkageError) {
                false
            }
        } else {
            false
        }

    @SuppressLint("NewApi")
    @RequiresApi(36)
    private object Api36Impl {
        @JvmStatic
        fun canEnterDesktopMode(context: Context): Boolean =
            com.android.wm.shell.shared.desktopmode.DesktopModeStatus
                .canEnterDesktopMode(context)

        @JvmStatic
        fun canEnterDesktopModeOrShowAppHandle(context: Context): Boolean =
            com.android.wm.shell.shared.desktopmode.DesktopModeStatus
                .canEnterDesktopModeOrShowAppHandle(context)

        @JvmStatic
        fun enterDesktopByDefaultOnFreeformDisplay(context: Context): Boolean =
            com.android.wm.shell.shared.desktopmode.DesktopModeStatus
                .enterDesktopByDefaultOnFreeformDisplay(context)

        @JvmStatic
        fun enableMultipleDesktops(context: Context): Boolean =
            com.android.wm.shell.shared.desktopmode.DesktopModeStatus
                .enableMultipleDesktops(context)

        @JvmStatic
        fun useRoundedCorners(): Boolean =
            com.android.wm.shell.shared.desktopmode.DesktopModeStatus
                .useRoundedCorners()
    }
}
