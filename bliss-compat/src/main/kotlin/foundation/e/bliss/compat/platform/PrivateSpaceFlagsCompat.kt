/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 *
 * Murena/LineageOS framework.jar may not yet expose
 * android.multiuser.Flags.enableMovingContentIntoPrivateSpace() — calling it
 * directly throws NoSuchMethodError on long-press of any home-screen icon
 * (PopupContainerWithArrow.showForIcon → BubbleTextView.startLongPressAction
 *  → Workspace.beginDragShared).
 *
 * This compat shim resolves the API once via ReflectionGate, caches the
 * boolean, and returns false when the method is absent (the safe default —
 * without the flag, BlissLauncher behaves as if private-space content moving
 * is off).
 */
/*
 * File:    bliss-compat/src/main/kotlin/foundation/e/bliss/compat/platform/PrivateSpaceFlagsCompat.kt
 * Module:  :bliss-compat  (foundation.e.bliss.compat.platform)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/compat/platform/):
 *   ├── DisplayIdCompat.kt              — Context.getDisplayId() shim
 *   ├── LineageSettingsCompat.kt        — LineageSettings.System.getInt() shim
 *   └── PrivateSpaceFlagsCompat.kt      — android.multiuser.Flags shim  ← THIS FILE
 *
 * Purpose:
 *   Replaces the M03-era `foundation.e.bliss.utils.PrivateSpaceCompat`. Keeps
 *   the same public method name (`enableMovingContentIntoPrivateSpace`) so
 *   the two existing `import static …` sites in the AOSP-aligned popup /
 *   all-apps files keep working with a single import-line rewrite.
 *
 * Consumed by:
 *   - com.android.launcher3.popup.PopupContainerWithArrow (import-static)
 *   - com.android.launcher3.allapps.AlphabeticalAppsList   (import-static)
 *
 * Calls into:
 *   - foundation.e.bliss.compat.ReflectionGate
 *
 * Plan reference: Plans/Migration04/01-compat-platform.md §3.3
 */
package foundation.e.bliss.compat.platform

import foundation.e.bliss.compat.ReflectionGate

object PrivateSpaceFlagsCompat {

    private val MOVING: Boolean = ReflectionGate.invokeStaticBoolean(
        "android.multiuser.Flags",
        "enableMovingContentIntoPrivateSpace"
    ) ?: false

    /** Safe replacement for `android.multiuser.Flags.enableMovingContentIntoPrivateSpace()`. */
    @JvmStatic
    fun enableMovingContentIntoPrivateSpace(): Boolean = MOVING
}
