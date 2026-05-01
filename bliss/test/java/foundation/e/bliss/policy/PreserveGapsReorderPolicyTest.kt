/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/policy/PreserveGapsReorderPolicyTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.policy)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/policy/):
 *   ├── PreserveGapsReorderPolicyTest.kt   — in-place wins over swap   ← THIS FILE
 *   ├── TwoByTwoFolderPolicyTest.kt        — 2×2 / 0.44f / 4
 *   └── HonorGapsIdleAppPolicyTest.kt      — shouldAutoFillIdle == false
 *
 * Purpose:
 *   Pin the PreserveGapsReorderPolicy decision: when the AOSP algorithm
 *   produces a valid drop-in-place solution, the policy must short-circuit
 *   and return it. When no in-place solution exists, the policy must return
 *   null so the AOSP algorithm runs unmodified.
 *
 * Plan reference: Plans/Migration04/05-launcher-policy-strategies.md §8
 */
package foundation.e.bliss.policy

import com.android.launcher3.CellLayout
import com.android.launcher3.celllayout.ItemConfiguration
import com.android.launcher3.celllayout.ReorderParameters
import foundation.e.bliss.policy.reorder.PreserveGapsReorderPolicy
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock

class PreserveGapsReorderPolicyTest {

    private val cellLayout: CellLayout = mock(CellLayout::class.java)
    private val params: ReorderParameters = mock(ReorderParameters::class.java)

    @Test
    fun inPlaceSolutionShortCircuitsOverSwap() {
        val inPlace = ItemConfiguration().apply { isSolution = true }
        val result =
            PreserveGapsReorderPolicy.overrideDirectionAndPreference(cellLayout, params, inPlace)
        assertSame(inPlace, result)
    }

    @Test
    fun nullWhenNoInPlaceSolution() {
        val noSolution = ItemConfiguration().apply { isSolution = false }
        val result =
            PreserveGapsReorderPolicy.overrideDirectionAndPreference(cellLayout, params, noSolution)
        assertNull(result)
    }
}
