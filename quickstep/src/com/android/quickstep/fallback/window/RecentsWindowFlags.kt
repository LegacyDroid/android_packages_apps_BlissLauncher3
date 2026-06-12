/*
 * Copyright (C) 2025 The Android Open Source Project
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
 *     — Plan ref: Plans/Migration04/01-compat-platform.md §4
 *
 * The body of this file otherwise tracks AOSP. Keep diffs minimal so a
 * future origin/a16 rebase merges cleanly.
 */
package com.android.quickstep.fallback.window

import foundation.e.bliss.compat.desktop.DesktopFlagsCompat

class RecentsWindowFlags {
    companion object {
        @JvmStatic
        val enableLauncherOverviewInWindow: Boolean
            get() = DesktopFlagsCompat.enableLauncherOverviewInWindow()

        @JvmStatic
        val enableFallbackOverviewInWindow: Boolean
            get() = DesktopFlagsCompat.enableFallbackOverviewInWindow()

        @JvmStatic
        val enableOverviewOnConnectedDisplays: Boolean
            get() = DesktopFlagsCompat.enableOverviewOnConnectedDisplays()

        @JvmStatic
        val enableOverviewInWindow: Boolean
            get() = DesktopFlagsCompat.enableOverviewInWindow()
    }
}
