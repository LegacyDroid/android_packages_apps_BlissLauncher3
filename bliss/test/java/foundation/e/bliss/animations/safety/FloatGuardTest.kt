/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/animations/safety/FloatGuardTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.animations.safety)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/animations/safety/):
 *   └── FloatGuardTest.kt          — JVM unit test for FloatGuard           ← THIS FILE
 *
 * Purpose:
 *   Pure JVM (no Robolectric) test of FloatGuard.safeDivide and
 *   FloatGuard.requireAllFinite. Exercises both debug-mode (throws on
 *   non-finite) and release-mode (logs + fallback) by flipping the
 *   internal `debugMode` switch — that switch exists specifically so this
 *   test can validate release behaviour without rebuilding.
 *
 * Plan reference: Plans/Migration04/06-animation-safety.md §6
 */
package foundation.e.bliss.animations.safety

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FloatGuardTest {

    @After
    fun restoreDebugMode() {
        // Reset to default after each test so the suite is order-independent.
        FloatGuard.debugMode = false
    }

    // --- safeDivide (release-mode) ---------------------------------------

    @Test
    fun safeDivide_normal_returnsQuotient() {
        FloatGuard.debugMode = false
        assertEquals(2f, FloatGuard.safeDivide("x", 4f, 2f), 0f)
    }

    @Test
    fun safeDivide_zeroDenominator_returnsFallback() {
        FloatGuard.debugMode = false
        assertEquals(99f, FloatGuard.safeDivide("x", 4f, 0f, fallback = 99f), 0f)
    }

    @Test
    fun safeDivide_negativeDenominator_returnsFallback() {
        FloatGuard.debugMode = false
        assertEquals(7f, FloatGuard.safeDivide("x", 4f, -1f, fallback = 7f), 0f)
    }

    @Test
    fun safeDivide_nonFiniteNumerator_returnsFallback() {
        FloatGuard.debugMode = false
        assertEquals(11f, FloatGuard.safeDivide("x", Float.NaN, 2f, fallback = 11f), 0f)
        assertEquals(
            11f,
            FloatGuard.safeDivide("x", Float.POSITIVE_INFINITY, 2f, fallback = 11f),
            0f,
        )
    }

    @Test
    fun safeDivide_default_fallback_is_one() {
        FloatGuard.debugMode = false
        assertEquals(1f, FloatGuard.safeDivide("x", 4f, 0f), 0f)
    }

    // --- safeDivide (debug-mode) -----------------------------------------

    @Test
    fun safeDivide_zeroDenominator_throwsInDebug() {
        FloatGuard.debugMode = true
        try {
            FloatGuard.safeDivide("x", 4f, 0f)
            fail("expected IllegalStateException in debug mode")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("FloatGuard"))
        }
    }

    // --- requireAllFinite (release-mode) ---------------------------------

    @Test
    fun requireAllFinite_passes_for_all_finite() {
        FloatGuard.debugMode = false
        assertTrue(FloatGuard.requireAllFinite("x", 1f, 2f, 3f))
    }

    @Test
    fun requireAllFinite_passes_for_zero_and_negative() {
        // Finite includes zero and negatives — only NaN/±Inf fail.
        FloatGuard.debugMode = false
        assertTrue(FloatGuard.requireAllFinite("x", 0f, -1f, -1e30f))
    }

    @Test
    fun requireAllFinite_nan_returnsFalse() {
        FloatGuard.debugMode = false
        assertFalse(FloatGuard.requireAllFinite("x", 1f, Float.NaN, 3f))
    }

    @Test
    fun requireAllFinite_positiveInfinity_returnsFalse() {
        FloatGuard.debugMode = false
        assertFalse(FloatGuard.requireAllFinite("x", 1f, Float.POSITIVE_INFINITY, 3f))
    }

    @Test
    fun requireAllFinite_negativeInfinity_returnsFalse() {
        FloatGuard.debugMode = false
        assertFalse(FloatGuard.requireAllFinite("x", Float.NEGATIVE_INFINITY))
    }

    // --- requireAllFinite (debug-mode) -----------------------------------

    @Test
    fun requireAllFinite_nan_throwsInDebug() {
        FloatGuard.debugMode = true
        try {
            FloatGuard.requireAllFinite("x", 1f, Float.NaN, 3f)
            fail("expected IllegalStateException in debug mode")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("idx=1"))
        }
    }
}
