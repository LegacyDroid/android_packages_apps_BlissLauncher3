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
 * File:    bliss-compat/src/main/kotlin/foundation/e/bliss/compat/desktop/DesktopFlagsCompat.kt
 * Module:  :bliss-compat  (foundation.e.bliss.compat.desktop)
 * Role:    RELOCATED
 *
 * Tree (foundation/e/bliss/compat/desktop/):
 *   ├── DesktopFlagsCompat.kt           — DesktopMode/Experience/window flag getters  ← THIS FILE
 *   └── DesktopModeStatusCompat.kt      — DesktopModeStatus method shim
 *
 * Purpose:
 *   Compatibility wrapper for `android.window.DesktopModeFlags`,
 *   `android.window.DesktopExperienceFlags`, and `com.android.window.flags.Flags`
 *   which are only available on API 36+. All methods return `false` on API < 36,
 *   since desktop windowing and the associated platform feature flags don't
 *   exist on Android 15 and below. Body unchanged from the M03 file at
 *   `com.android.launcher3.util.DesktopFlagsCompat`; only the package and the
 *   SDK literal (now `BuildCompat.isAtLeastBaklava`) change.
 *
 *   Each getter also degrades to `false` (the same default as pre-API-36) when
 *   the underlying flag symbol is absent at runtime on a mismatched framework
 *   (e.g. a stock emulator image rather than /e/OS), instead of crashing the
 *   launcher with NoSuchFieldError/NoSuchMethodError. See [safe].
 *
 * Consumed by: see ~37 call-sites across `src/` and `quickstep/` listed in
 *   Plans/Migration04/01-compat-platform.md §4 (rewritten by the call-site
 *   sweep).
 *
 * Calls into:
 *   - foundation.e.bliss.compat.BuildCompat
 *
 * Plan reference: Plans/Migration04/01-compat-platform.md §3.4
 */
@file:SuppressLint("NewApi")

package foundation.e.bliss.compat.desktop

import android.annotation.SuppressLint
import androidx.annotation.RequiresApi
import com.android.launcher3.Flags
import foundation.e.bliss.compat.BuildCompat

object DesktopFlagsCompat {

    // DesktopModeFlags

    @JvmStatic
    fun enableDesktopWindowingWallpaperActivity(): Boolean =
        safe { Api36Impl.enableDesktopWindowingWallpaperActivity() }

    @JvmStatic
    fun enableDesktopWindowingQuickSwitch(): Boolean =
        safe { Api36Impl.enableDesktopWindowingQuickSwitch() }

    @JvmStatic
    fun enableDesktopAppLaunchAlttabTransitionsBugfix(): Boolean =
        safe { Api36Impl.enableDesktopAppLaunchAlttabTransitionsBugfix() }

    @JvmStatic
    fun enableDesktopAppLaunchTransitionsBugfix(): Boolean =
        safe { Api36Impl.enableDesktopAppLaunchTransitionsBugfix() }

    @JvmStatic
    fun enableStartLaunchTransitionFromTaskbarBugfix(): Boolean =
        safe { Api36Impl.enableStartLaunchTransitionFromTaskbarBugfix() }

    @JvmStatic
    fun enableDesktopWindowingExitTransitionsBugfix(): Boolean =
        safe { Api36Impl.enableDesktopWindowingExitTransitionsBugfix() }

    @JvmStatic
    fun enableTaskbarRecentsLayoutTransition(): Boolean =
        safe { Api36Impl.enableTaskbarRecentsLayoutTransition() }

    @JvmStatic
    fun enableDesktopWindowingBackNavigation(): Boolean =
        safe { Api36Impl.enableDesktopWindowingBackNavigation() }

    @JvmStatic
    fun enableDesktopWindowingTaskbarRunningApps(): Boolean =
        safe { Api36Impl.enableDesktopWindowingTaskbarRunningApps() }

    @JvmStatic
    fun enableDesktopTrampolineCloseAnimationBugfix(): Boolean =
        safe { Api36Impl.enableDesktopTrampolineCloseAnimationBugfix() }

    @JvmStatic
    fun enableQuickswitchDesktopSplitBugfix(): Boolean =
        safe { Api36Impl.enableQuickswitchDesktopSplitBugfix() }

    // DesktopExperienceFlags

    @JvmStatic
    fun enableMultipleDesktopsBackend(): Boolean =
        safe { Api36Impl.enableMultipleDesktopsBackend() }

    @JvmStatic
    fun enableMultipleDesktopsFrontend(): Boolean =
        safe { Api36Impl.enableMultipleDesktopsFrontend() }

    @JvmStatic
    fun enableTaskbarConnectedDisplays(): Boolean =
        safe { Api36Impl.enableTaskbarConnectedDisplays() }

    // Custom DesktopModeFlag instances

    @JvmStatic
    fun enableTaskbarBehindShade(): Boolean =
        safe { Api36Impl.enableTaskbarBehindShade() }

    @JvmStatic
    fun enableGestureNavOnConnectedDisplays(): Boolean =
        safe { Api36Impl.enableGestureNavOnConnectedDisplays() }

    @JvmStatic
    fun enableLauncherOverviewInWindow(): Boolean =
        safe { Api36Impl.enableLauncherOverviewInWindow() }

    @JvmStatic
    fun enableFallbackOverviewInWindow(): Boolean =
        safe { Api36Impl.enableFallbackOverviewInWindow() }

    @JvmStatic
    fun enableOverviewOnConnectedDisplays(): Boolean =
        safe { Api36Impl.enableOverviewOnConnectedDisplays() }

    @JvmStatic
    fun enableOverviewInWindow(): Boolean =
        enableLauncherOverviewInWindow() ||
            enableFallbackOverviewInWindow() ||
            enableOverviewOnConnectedDisplays()

    // com.android.window.flags.Flags (aconfig-generated platform flags)

    @JvmStatic
    fun enableDesktopWindowingMode(): Boolean =
        safe { Api36Impl.enableDesktopWindowingMode() }

    @JvmStatic
    fun predictiveBackThreeButtonNav(): Boolean =
        safe { Api36Impl.predictiveBackThreeButtonNav() }

    @JvmStatic
    fun removeDepartTargetFromMotion(): Boolean =
        safe { Api36Impl.removeDepartTargetFromMotion() }

    @JvmStatic
    fun universalResizableByDefault(): Boolean =
        safe { Api36Impl.universalResizableByDefault() }

    @JvmStatic
    fun enableDesktopRecentsTransitionsCornersBugfix(): Boolean =
        safe { Api36Impl.enableDesktopRecentsTransitionsCornersBugfix() }

    @JvmStatic
    fun enableDesktopWindowingPersistence(): Boolean =
        safe { Api36Impl.enableDesktopWindowingPersistence() }

    @JvmStatic
    fun moveToExternalDisplayShortcut(): Boolean =
        safe { Api36Impl.moveToExternalDisplayShortcut() }

    @JvmStatic
    fun enableDesktopTaskbarOnFreeformDisplays(): Boolean =
        safe { Api36Impl.enableDesktopTaskbarOnFreeformDisplays() }

    // ProtoLog flags

    @JvmStatic
    fun enableStateManagerProtoLog(): Boolean =
        safe { Api36Impl.enableStateManagerProtoLog() }

    @JvmStatic
    fun enableRecentsWindowProtoLog(): Boolean =
        safe { Api36Impl.enableRecentsWindowProtoLog() }

    /**
     * Runs [block] only on API 36+, returning false when the underlying
     * `android.window.DesktopModeFlags` / `DesktopExperienceFlags` /
     * `com.android.window.flags.Flags` symbol is absent on a mismatched framework
     * (e.g. a stock emulator image) rather than crashing. The flag simply reads off.
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
        fun enableDesktopWindowingWallpaperActivity(): Boolean =
            android.window.DesktopModeFlags
                .ENABLE_DESKTOP_WINDOWING_WALLPAPER_ACTIVITY.isTrue

        @JvmStatic
        fun enableDesktopWindowingQuickSwitch(): Boolean =
            android.window.DesktopModeFlags
                .ENABLE_DESKTOP_WINDOWING_QUICK_SWITCH.isTrue

        @JvmStatic
        fun enableDesktopAppLaunchAlttabTransitionsBugfix(): Boolean =
            android.window.DesktopModeFlags
                .ENABLE_DESKTOP_APP_LAUNCH_ALTTAB_TRANSITIONS_BUGFIX.isTrue

        @JvmStatic
        fun enableDesktopAppLaunchTransitionsBugfix(): Boolean =
            android.window.DesktopModeFlags
                .ENABLE_DESKTOP_APP_LAUNCH_TRANSITIONS_BUGFIX.isTrue

        @JvmStatic
        fun enableStartLaunchTransitionFromTaskbarBugfix(): Boolean =
            android.window.DesktopModeFlags
                .ENABLE_START_LAUNCH_TRANSITION_FROM_TASKBAR_BUGFIX.isTrue

        @JvmStatic
        fun enableDesktopWindowingExitTransitionsBugfix(): Boolean =
            android.window.DesktopModeFlags
                .ENABLE_DESKTOP_WINDOWING_EXIT_TRANSITIONS_BUGFIX.isTrue

        @JvmStatic
        fun enableTaskbarRecentsLayoutTransition(): Boolean =
            android.window.DesktopModeFlags
                .ENABLE_TASKBAR_RECENTS_LAYOUT_TRANSITION.isTrue

        @JvmStatic
        fun enableDesktopWindowingBackNavigation(): Boolean =
            android.window.DesktopModeFlags
                .ENABLE_DESKTOP_WINDOWING_BACK_NAVIGATION.isTrue

        @JvmStatic
        fun enableDesktopWindowingTaskbarRunningApps(): Boolean =
            android.window.DesktopModeFlags
                .ENABLE_DESKTOP_WINDOWING_TASKBAR_RUNNING_APPS.isTrue

        @JvmStatic
        fun enableDesktopTrampolineCloseAnimationBugfix(): Boolean =
            android.window.DesktopModeFlags
                .ENABLE_DESKTOP_TRAMPOLINE_CLOSE_ANIMATION_BUGFIX.isTrue

        @JvmStatic
        fun enableQuickswitchDesktopSplitBugfix(): Boolean =
            android.window.DesktopModeFlags
                .ENABLE_QUICKSWITCH_DESKTOP_SPLIT_BUGFIX.isTrue

        @JvmStatic
        fun enableMultipleDesktopsBackend(): Boolean =
            android.window.DesktopExperienceFlags
                .ENABLE_MULTIPLE_DESKTOPS_BACKEND.isTrue

        @JvmStatic
        fun enableMultipleDesktopsFrontend(): Boolean =
            android.window.DesktopExperienceFlags
                .ENABLE_MULTIPLE_DESKTOPS_FRONTEND.isTrue

        @JvmStatic
        fun enableTaskbarConnectedDisplays(): Boolean =
            android.window.DesktopExperienceFlags
                .ENABLE_TASKBAR_CONNECTED_DISPLAYS.isTrue

        @JvmStatic
        fun enableTaskbarBehindShade(): Boolean =
            android.window.DesktopModeFlags.DesktopModeFlag(
                Flags::enableTaskbarBehindShade, false
            ).isTrue

        @JvmStatic
        fun enableGestureNavOnConnectedDisplays(): Boolean =
            android.window.DesktopModeFlags.DesktopModeFlag(
                Flags::enableGestureNavOnConnectedDisplays, false
            ).isTrue

        @JvmStatic
        fun enableLauncherOverviewInWindow(): Boolean =
            android.window.DesktopModeFlags.DesktopModeFlag(
                Flags::enableLauncherOverviewInWindow, false
            ).isTrue

        @JvmStatic
        fun enableFallbackOverviewInWindow(): Boolean =
            android.window.DesktopModeFlags.DesktopModeFlag(
                Flags::enableFallbackOverviewInWindow, false
            ).isTrue

        @JvmStatic
        fun enableOverviewOnConnectedDisplays(): Boolean =
            android.window.DesktopModeFlags.DesktopModeFlag(
                Flags::enableOverviewOnConnectedDisplays, false
            ).isTrue

        @JvmStatic
        fun enableDesktopWindowingMode(): Boolean =
            com.android.window.flags.Flags.enableDesktopWindowingMode()

        @JvmStatic
        fun predictiveBackThreeButtonNav(): Boolean =
            com.android.window.flags.Flags.predictiveBackThreeButtonNav()

        @JvmStatic
        fun removeDepartTargetFromMotion(): Boolean =
            com.android.window.flags.Flags.removeDepartTargetFromMotion()

        @JvmStatic
        fun universalResizableByDefault(): Boolean =
            com.android.window.flags.Flags.universalResizableByDefault()

        @JvmStatic
        fun enableDesktopRecentsTransitionsCornersBugfix(): Boolean =
            com.android.window.flags.Flags.enableDesktopRecentsTransitionsCornersBugfix()

        @JvmStatic
        fun enableDesktopWindowingPersistence(): Boolean =
            com.android.window.flags.Flags.enableDesktopWindowingPersistence()

        @JvmStatic
        fun moveToExternalDisplayShortcut(): Boolean =
            com.android.window.flags.Flags.moveToExternalDisplayShortcut()

        @JvmStatic
        fun enableDesktopTaskbarOnFreeformDisplays(): Boolean =
            com.android.window.flags.Flags.enableDesktopTaskbarOnFreeformDisplays()

        @JvmStatic
        fun enableStateManagerProtoLog(): Boolean =
            android.window.DesktopModeFlags.DesktopModeFlag(
                Flags::enableStateManagerProtoLog, true
            ).isTrue

        @JvmStatic
        fun enableRecentsWindowProtoLog(): Boolean =
            android.window.DesktopModeFlags.DesktopModeFlag(
                Flags::enableRecentsWindowProtoLog, true
            ).isTrue
    }
}
