/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss-policy-core/src/test/kotlin/foundation/e/bliss/policy/HonorGapsIdleAppPolicyTest.kt
 * Module:  :bliss-policy-core (test)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/policy/):
 *   ├── PreserveGapsReorderPolicyTest.kt   — in-place wins over swap
 *   ├── TwoByTwoFolderPolicyTest.kt        — 2×2 / 0.44f / 4
 *   └── HonorGapsIdleAppPolicyTest.kt      — shouldAutoFillIdle == false  ← THIS FILE
 *
 * Purpose:
 *   Pin the HonorGapsIdleAppPolicy decision: when the policy is selected by
 *   LauncherPolicy.idleApp() (PRESERVE_LAYOUT_GAPS=true), callers must NOT
 *   auto-fill empty cells. Counterpart AutoFillIdleAppPolicy returns true.
 *
 * Plan reference: Plans/Migration04/05-launcher-policy-strategies.md §8
 */
package foundation.e.bliss.policy

import foundation.e.bliss.policy.idle.AutoFillIdleAppPolicy
import foundation.e.bliss.policy.idle.HonorGapsIdleAppPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HonorGapsIdleAppPolicyTest {

    @Test
    fun honorGapsRefusesAutoFill() {
        assertFalse(HonorGapsIdleAppPolicy.shouldAutoFillIdle())
    }

    @Test
    fun autoFillEnablesAutoFill() {
        assertTrue(AutoFillIdleAppPolicy.shouldAutoFillIdle())
    }
}
