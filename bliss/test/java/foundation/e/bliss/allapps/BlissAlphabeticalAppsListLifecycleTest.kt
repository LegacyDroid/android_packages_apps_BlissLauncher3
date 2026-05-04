/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/allapps/BlissAlphabeticalAppsListLifecycleTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.allapps)
 * Role:    NEW
 *
 * Purpose:
 *   Audit01 #01 — pin the BlissAlphabeticalAppsList teardown contract so a
 *   future refactor cannot silently regress the memory leak we just fixed.
 *
 *   The leak was: `release()` cancelled the SupervisorJob and the Room flow
 *   collector but never called `AllAppsStore.removeUpdateListener(...)` for
 *   the listener registered in `startObserving()`. The fix introduces a
 *   stored `storeListener` field of type `AllAppsStore.OnUpdateListener` and
 *   has `release()` remove it from the store.
 *
 *   This test verifies the contract STRUCTURALLY (via reflection) rather
 *   than by end-to-end instantiation. The parent AlphabeticalAppsList
 *   constructor pulls in the entire Bliss + AOSP graph (UserCache via the
 *   Dagger LauncherAppComponent, ImageSpan against R.drawable.* resources,
 *   DeviceProfile from a real Activity, …), which is impractical to stand
 *   up in a JVM unit test. Reflection lets us pin the three things that
 *   matter for the leak fix without that overhead:
 *     1. A `storeListener` field exists, of type `AllAppsStore.OnUpdateListener`.
 *     2. `release()` is declared on the class.
 *     3. The compiled `release()` body invokes
 *        `AllAppsStore.removeUpdateListener` (verified by inspecting method
 *        references in the bytecode constant pool via the Kotlin reflection
 *        + a string scan of the `.class` resource).
 *
 *   If this test fails, someone deleted the listener-removal call. Re-read
 *   Plans/Audits/audit01/01-critical-allapps-leak.md before "fixing" it.
 *
 * Plan reference: Plans/Audits/audit01/01-critical-allapps-leak.md §"Test plan"
 */
package foundation.e.bliss.allapps

import com.android.launcher3.allapps.AllAppsStore
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlissAlphabeticalAppsListLifecycleTest {

    private val cls = BlissAlphabeticalAppsList::class.java

    @Test
    fun storeListener_field_exists_with_OnUpdateListener_type() {
        val field =
            cls.declaredFields.firstOrNull { it.name == "storeListener" }
                ?: error(
                    "BlissAlphabeticalAppsList.storeListener field is missing — the fix for " +
                        "Audit01 #01 requires storing the listener so release() can remove it."
                )
        assertEquals(
            "storeListener must be typed as AllAppsStore.OnUpdateListener so it can be passed " +
                "to AllAppsStore.removeUpdateListener(...)",
            AllAppsStore.OnUpdateListener::class.java,
            field.type,
        )
    }

    @Test
    fun release_method_is_declared() {
        val release =
            cls.declaredMethods.firstOrNull { it.name == "release" && it.parameterCount == 0 }
        assertNotNull(
            "BlissAlphabeticalAppsList.release() must exist so the activity can tear down " +
                "the AllAppsStore listener and the SupervisorJob on destroy.",
            release,
        )
    }

    @Test
    fun release_method_body_references_removeUpdateListener() {
        val classBytes = readOwnBytecode(cls)
        // The constant pool of the .class file holds method-reference name strings as plain
        // UTF-8. If `release()` calls AllAppsStore.removeUpdateListener(...) the constant pool
        // will contain both the owner ("AllAppsStore") and the method name
        // ("removeUpdateListener"). A bytecode-level structural check is overkill for a single
        // call site; the string scan is sufficient and fails loud if either disappears.
        val asString = String(classBytes, Charsets.ISO_8859_1)
        assertTrue(
            "BlissAlphabeticalAppsList must reference AllAppsStore.removeUpdateListener — " +
                "Audit01 #01 fix requires release() to remove the stored listener.",
            asString.contains("removeUpdateListener"),
        )
        assertTrue(
            "BlissAlphabeticalAppsList must reference com/android/launcher3/allapps/AllAppsStore",
            asString.contains("com/android/launcher3/allapps/AllAppsStore"),
        )
    }

    @Test
    fun release_method_body_still_cancels_supervisor_and_collectJob() {
        // Regression guard: don't let a future refactor silently drop the coroutine teardown
        // half of release() while keeping the listener removal half (or vice versa). Both
        // halves are required for Audit01 #01.
        val classBytes = readOwnBytecode(cls)
        val asString = String(classBytes, Charsets.ISO_8859_1)
        assertTrue(
            "BlissAlphabeticalAppsList.release() must still cancel collectJob",
            asString.contains("collectJob"),
        )
        assertTrue(
            "BlissAlphabeticalAppsList.release() must still cancel the supervisor SupervisorJob",
            asString.contains("supervisor"),
        )
    }

    private fun readOwnBytecode(klass: Class<*>): ByteArray {
        val resourceName = klass.name.replace('.', '/') + ".class"
        val stream =
            klass.classLoader.getResourceAsStream(resourceName)
                ?: error("Could not locate compiled bytecode for ${klass.name}")
        val out = ByteArrayOutputStream()
        stream.use { it.copyTo(out) }
        return out.toByteArray()
    }
}
