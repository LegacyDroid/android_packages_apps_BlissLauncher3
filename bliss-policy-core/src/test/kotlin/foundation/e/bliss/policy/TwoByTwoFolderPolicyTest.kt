/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss-policy-core/src/test/kotlin/foundation/e/bliss/policy/TwoByTwoFolderPolicyTest.kt
 * Module:  :bliss-policy-core (test)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/policy/):
 *   ├── PreserveGapsReorderPolicyTest.kt   — in-place wins over swap
 *   ├── TwoByTwoFolderPolicyTest.kt        — 2×2 / 0.44f / 4 (this file) ← THIS FILE
 *   └── HonorGapsIdleAppPolicyTest.kt      — shouldAutoFillIdle == false
 *
 * Purpose:
 *   Pin the Lawnchair-style folder-preview constants. If anyone changes the
 *   2×2 grid count, scale, or maxNumItemsInPreview, this fails loudly.
 *
 * Plan reference: Plans/Migration04/05-launcher-policy-strategies.md §8
 */
package foundation.e.bliss.policy

import foundation.e.bliss.policy.folder.TwoByTwoFolderPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class TwoByTwoFolderPolicyTest {

    @Test
    fun gridCountsAreTwoByTwo() {
        assertEquals(2, TwoByTwoFolderPolicy.gridCountX)
        assertEquals(2, TwoByTwoFolderPolicy.gridCountY)
    }

    @Test
    fun itemIconScaleMatchesLawnchairMinScale() {
        assertEquals(0.44f, TwoByTwoFolderPolicy.itemIconScale, 0.0001f)
    }

    @Test
    fun maxNumItemsInPreviewIsFour() {
        assertEquals(4, TwoByTwoFolderPolicy.maxNumItemsInPreview)
    }
}
