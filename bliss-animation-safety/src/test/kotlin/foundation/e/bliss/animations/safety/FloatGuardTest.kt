/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    bliss-animation-safety/src/test/kotlin/foundation/e/bliss/animations/safety/FloatGuardTest.kt
 * Module:  :bliss-animation-safety (test)
 * Role:    JVM unit tests for FloatGuard.
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
    fun tearDown() {
        FloatGuard.debugOverride = null
    }

    // --- safeDivide (release-mode) ---------------------------------------

    @Test
    fun safeDivide_normal_returnsQuotient() {
        FloatGuard.debugOverride = false
        assertEquals(2f, FloatGuard.safeDivide("x", 4f, 2f), 0f)
    }

    @Test
    fun safeDivide_zeroDenominator_returnsFallback() {
        FloatGuard.debugOverride = false
        assertEquals(99f, FloatGuard.safeDivide("x", 4f, 0f, fallback = 99f), 0f)
    }

    @Test
    fun safeDivide_negativeDenominator_returnsFallback() {
        FloatGuard.debugOverride = false
        assertEquals(7f, FloatGuard.safeDivide("x", 4f, -1f, fallback = 7f), 0f)
    }

    @Test
    fun safeDivide_nonFiniteNumerator_returnsFallback() {
        FloatGuard.debugOverride = false
        assertEquals(11f, FloatGuard.safeDivide("x", Float.NaN, 2f, fallback = 11f), 0f)
        assertEquals(
            11f,
            FloatGuard.safeDivide("x", Float.POSITIVE_INFINITY, 2f, fallback = 11f),
            0f,
        )
    }

    @Test
    fun safeDivide_default_fallback_is_one() {
        FloatGuard.debugOverride = false
        assertEquals(1f, FloatGuard.safeDivide("x", 4f, 0f), 0f)
    }

    // --- safeDivide (debug-mode) -----------------------------------------

    @Test
    fun safeDivide_zeroDenominator_throwsInDebug() {
        FloatGuard.debugOverride = true
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
        FloatGuard.debugOverride = false
        assertTrue(FloatGuard.requireAllFinite("x", 1f, 2f, 3f))
    }

    @Test
    fun requireAllFinite_passes_for_zero_and_negative() {
        FloatGuard.debugOverride = false
        assertTrue(FloatGuard.requireAllFinite("x", 0f, -1f, -1e30f))
    }

    @Test
    fun requireAllFinite_nan_returnsFalse() {
        FloatGuard.debugOverride = false
        assertFalse(FloatGuard.requireAllFinite("x", 1f, Float.NaN, 3f))
    }

    @Test
    fun requireAllFinite_positiveInfinity_returnsFalse() {
        FloatGuard.debugOverride = false
        assertFalse(FloatGuard.requireAllFinite("x", 1f, Float.POSITIVE_INFINITY, 3f))
    }

    @Test
    fun requireAllFinite_negativeInfinity_returnsFalse() {
        FloatGuard.debugOverride = false
        assertFalse(FloatGuard.requireAllFinite("x", Float.NEGATIVE_INFINITY))
    }

    // --- requireAllFinite (debug-mode) -----------------------------------

    @Test
    fun requireAllFinite_nan_throwsInDebug() {
        FloatGuard.debugOverride = true
        try {
            FloatGuard.requireAllFinite("x", 1f, Float.NaN, 3f)
            fail("expected IllegalStateException in debug mode")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("idx=1"))
        }
    }
}
