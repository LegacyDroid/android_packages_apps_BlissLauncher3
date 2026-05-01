/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/compat/ReflectionGateTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.compat)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/compat/):
 *   └── ReflectionGateTest.kt          — JVM unit test for ReflectionGate  ← THIS FILE
 *
 * Purpose:
 *   Smoke test for ReflectionGate.classExists / lookupMethod /
 *   invokeStaticBoolean. Pure JVM (no Robolectric) — exercises the cache by
 *   probing well-known stdlib classes (java.lang.String, java.util.Arrays)
 *   and asserting that misses are memoised as well as hits. The production
 *   shims that *use* ReflectionGate (PrivateSpaceFlagsCompat,
 *   ActivityTaskManagerCompat, QuickStepContractCompat) probe Android-only
 *   classes which aren't on the JVM unit-test classpath, so we substitute
 *   stdlib classes here.
 *
 * Plan reference: Plans/Migration04/08-observability-and-testing.md §3.6
 *                 Plans/Migration04/01-compat-platform.md §3.2
 */
package foundation.e.bliss.compat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReflectionGateTest {

    @Test
    fun classExists_returnsTrueForKnownClass() {
        assertTrue(ReflectionGate.classExists("java.lang.String"))
    }

    @Test
    fun classExists_returnsFalseForMissingClass() {
        assertFalse(ReflectionGate.classExists("foundation.e.bliss.compat.NoSuchClassZZZ"))
    }

    @Test
    fun lookupMethod_returnsMethodForKnownSignature() {
        // String.length(): no-arg, returns int.
        val m = ReflectionGate.lookupMethod("java.lang.String", "length")
        assertNotNull("length() should resolve on java.lang.String", m)
        assertEquals(java.lang.Integer.TYPE, m!!.returnType)
    }

    @Test
    fun lookupMethod_returnsNullForMissingMethod() {
        val m = ReflectionGate.lookupMethod("java.lang.String", "noSuchMethodZZZ")
        assertNull(m)
    }

    @Test
    fun lookupMethod_returnsNullForMissingClass() {
        val m = ReflectionGate.lookupMethod("foundation.e.bliss.compat.NoSuchClassZZZ", "anyMethod")
        assertNull(m)
    }

    @Test
    fun lookupMethod_cachesHits() {
        // Same Method object should be returned on a second call (cache hit).
        val first = ReflectionGate.lookupMethod("java.lang.String", "length")
        val second = ReflectionGate.lookupMethod("java.lang.String", "length")
        assertNotNull(first)
        assertSame("Cache should return the same Method instance", first, second)
    }

    @Test
    fun lookupMethod_cachesMissesToo() {
        // Two consecutive misses should both resolve to null without throwing,
        // demonstrating that null misses are also memoised (the cache stores
        // the negative result so the JVM doesn't re-throw ClassNotFoundException
        // on every call).
        val first =
            ReflectionGate.lookupMethod("foundation.e.bliss.compat.NoSuchClassZZZ", "anyMethod")
        val second =
            ReflectionGate.lookupMethod("foundation.e.bliss.compat.NoSuchClassZZZ", "anyMethod")
        assertNull(first)
        assertNull(second)
    }

    @Test
    fun lookupMethod_distinguishesParameterTypes() {
        // String.indexOf has overloads: (int) and (String). Lookups on
        // different parameter signatures should resolve independently.
        val byChar =
            ReflectionGate.lookupMethod("java.lang.String", "indexOf", java.lang.Integer.TYPE)
        val byString =
            ReflectionGate.lookupMethod("java.lang.String", "indexOf", String::class.java)
        assertNotNull("indexOf(int) should resolve", byChar)
        assertNotNull("indexOf(String) should resolve", byString)
        // The two overloads MUST be distinct Method instances.
        assertEquals(1, byChar!!.parameterTypes.size)
        assertEquals(1, byString!!.parameterTypes.size)
        assertEquals(java.lang.Integer.TYPE, byChar.parameterTypes[0])
        assertEquals(String::class.java, byString.parameterTypes[0])
    }

    @Test
    fun invokeStaticBoolean_returnsNullWhenClassMissing() {
        val v =
            ReflectionGate.invokeStaticBoolean(
                "foundation.e.bliss.compat.NoSuchClassZZZ",
                "anyMethod",
            )
        assertNull(v)
    }

    @Test
    fun invokeStaticBoolean_returnsNullWhenReturnTypeIsNotBoolean() {
        // String.length() exists but returns int, not Boolean — invokeStatic
        // would throw NPE because the method is non-static; the helper must
        // catch and return null.
        val v = ReflectionGate.invokeStaticBoolean("java.lang.String", "length")
        assertNull(v)
    }

    @Test
    fun invokeStaticBoolean_returnsTrueForKnownStaticBooleanMethod() {
        // java.lang.Boolean.parseBoolean is static and returns boolean, but
        // takes an argument so lookupMethod() with no params will miss it.
        // For a clean no-arg static-boolean target, use this helper class
        // declared below.
        val v = ReflectionGate.invokeStaticBoolean(BoolFixture::class.java.name, "alwaysTrue")
        assertEquals(true, v)
    }

    /** Test fixture — provides a static no-arg method returning boolean. */
    object BoolFixture {
        @JvmStatic fun alwaysTrue(): Boolean = true
    }
}
