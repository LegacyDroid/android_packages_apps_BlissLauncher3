/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    bliss-compat/src/main/kotlin/foundation/e/bliss/compat/BuildCompat.kt
 * Module:  :bliss-compat  (foundation.e.bliss.compat)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/compat/):
 *   ├── BuildCompat.kt           — SDK_INT gates with @ChecksSdkIntAtLeast  ← THIS FILE
 *   ├── ReflectionGate.kt        — single point of reflection (caches lookups)
 *   ├── platform/                — runtime-API shims (PrivateSpaceFlags, DisplayId, …)
 *   ├── desktop/                 — DesktopMode shims
 *   └── quickstep/               — QuickStep / ATM shims (compiled in withQuickstep flavour)
 *
 * Purpose:
 *   Centralised SDK-version gates. Every other file in the compat module
 *   (and any production code that needs an SDK gate) references these
 *   `isAtLeast*` constants instead of inlining `Build.VERSION.SDK_INT >= N`.
 *   The `@ChecksSdkIntAtLeast` annotations let R8 / Lint NewApi see through
 *   the wrappers so call-sites keep their `@RequiresApi` analysis.
 *
 * Plan reference: Plans/Migration04/01-compat-platform.md §3.1
 */
package foundation.e.bliss.compat

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Centralised SDK-version gates. Use these instead of inline Build.VERSION.SDK_INT
 * comparisons so static analysers and Lint NewApi can recognise the gate.
 *
 * Naming convention: isAtLeast<Letter>(major Android version).
 */
object BuildCompat {

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    val isAtLeastU: Boolean = Build.VERSION.SDK_INT >= 34

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    val isAtLeastV: Boolean = Build.VERSION.SDK_INT >= 35

    /** API 36 — Android 16 "Baklava". The Migration02/03 baseline. */
    @ChecksSdkIntAtLeast(api = 36)
    val isAtLeastBaklava: Boolean = Build.VERSION.SDK_INT >= 36
}
