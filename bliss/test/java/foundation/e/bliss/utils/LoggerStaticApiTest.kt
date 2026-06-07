/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/utils/LoggerStaticApiTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.utils)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/utils/):
 *   ├── LoggerTest.kt              — JVM unit test for instance API + FileLog backing
 *   └── LoggerStaticApiTest.kt     — JVM unit test for legacy static API gating  ← THIS FILE
 *
 * Purpose:
 *   Verifies Audit01 #03: the legacy `Logger.i/w/e(tag, msg)` static
 *   overloads emit unconditionally (mirroring the instance API), while
 *   `Logger.v/d(...)` remain debug-gated. Asserted by flipping
 *   `Logger.isDebug = false` to simulate a release build, then capturing
 *   logcat output via Robolectric's ShadowLog. The previous behaviour
 *   silently dropped every release-build INFO/WARN/ERROR call site, which
 *   broke production triage; this test pins the contract.
 *
 * Plan reference: Plans/Audits/audit01/03-logger-info-gating.md
 */
package foundation.e.bliss.utils

import android.util.Log
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
// Audit01 #03 close: pin Robolectric to SDK 35. The bliss module's
// targetSdkVersion=36 but Robolectric 4.14.1 only ships an SDK-35 runtime,
// so DefaultSdkPicker rejects target-36 with `targetSdkVersion=36 > maxSdkVersion=35`.
// Mirrors the neighbour pattern in BackupRestoreHelperTest / LoggerTest siblings.
@Config(sdk = [35], application = android.app.Application::class)
class LoggerStaticApiTest {

    private var originalIsDebug: Boolean = false

    @Before
    fun setup() {
        originalIsDebug = Logger.isDebug
        // Simulate a release build — the bug being fixed silenced all
        // legacy static i/w/e calls under this flag.
        Logger.isDebug = false
        ShadowLog.clear()
    }

    @After
    fun tearDown() {
        Logger.isDebug = originalIsDebug
        ShadowLog.clear()
    }

    // --- info / warn / error MUST emit even when isDebug == false ---------

    @Test
    fun static_i_emits_in_release() {
        Logger.i("AuditTag", "info-msg")
        val logs = ShadowLog.getLogsForTag("AuditTag")
        assertEquals(1, logs.size)
        assertEquals(Log.INFO, logs[0].type)
        assertEquals("info-msg", logs[0].msg)
    }

    @Test
    fun static_i_with_throwable_emits_in_release() {
        val boom = IllegalStateException("boom")
        Logger.i("AuditTag", "info-msg", boom)
        val logs = ShadowLog.getLogsForTag("AuditTag")
        assertEquals(1, logs.size)
        assertEquals(Log.INFO, logs[0].type)
        assertEquals("info-msg", logs[0].msg)
        assertEquals(boom, logs[0].throwable)
    }

    @Test
    fun static_w_emits_in_release() {
        Logger.w("AuditTag", "warn-msg")
        val logs = ShadowLog.getLogsForTag("AuditTag")
        assertEquals(1, logs.size)
        assertEquals(Log.WARN, logs[0].type)
        assertEquals("warn-msg", logs[0].msg)
    }

    @Test
    fun static_w_with_throwable_emits_in_release() {
        val boom = IllegalStateException("boom")
        Logger.w("AuditTag", "warn-msg", boom)
        val logs = ShadowLog.getLogsForTag("AuditTag")
        assertEquals(1, logs.size)
        assertEquals(Log.WARN, logs[0].type)
        assertEquals(boom, logs[0].throwable)
    }

    @Test
    fun static_e_emits_in_release() {
        Logger.e("AuditTag", "err-msg")
        val logs = ShadowLog.getLogsForTag("AuditTag")
        assertEquals(1, logs.size)
        assertEquals(Log.ERROR, logs[0].type)
        assertEquals("err-msg", logs[0].msg)
    }

    @Test
    fun static_e_with_throwable_emits_in_release() {
        val boom = RuntimeException("kaboom")
        Logger.e("AuditTag", "err-msg", boom)
        val logs = ShadowLog.getLogsForTag("AuditTag")
        assertEquals(1, logs.size)
        assertEquals(Log.ERROR, logs[0].type)
        assertEquals("err-msg", logs[0].msg)
        assertEquals(boom, logs[0].throwable)
    }

    // --- verbose / debug MUST stay gated ---------------------------------

    @Test
    fun static_v_does_not_emit_in_release() {
        Logger.v("AuditTag", "verbose-msg")
        assertTrue(ShadowLog.getLogsForTag("AuditTag").isEmpty())
    }

    @Test
    fun static_d_does_not_emit_in_release() {
        Logger.d("AuditTag", "debug-msg")
        assertTrue(ShadowLog.getLogsForTag("AuditTag").isEmpty())
    }

    @Test
    fun static_v_emits_when_isDebug_true() {
        Logger.isDebug = true
        Logger.v("AuditTag", "verbose-msg")
        val logs = ShadowLog.getLogsForTag("AuditTag")
        assertEquals(1, logs.size)
        assertEquals(Log.VERBOSE, logs[0].type)
    }

    @Test
    fun static_d_emits_when_isDebug_true() {
        Logger.isDebug = true
        Logger.d("AuditTag", "debug-msg")
        val logs = ShadowLog.getLogsForTag("AuditTag")
        assertEquals(1, logs.size)
        assertEquals(Log.DEBUG, logs[0].type)
    }

    // --- persistStatic must not throw when FileLog is uninitialised -------

    @Test
    fun static_i_does_not_throw_when_FileLog_uninitialised() {
        // FileLog.setDir() is never called in the JVM unit-test classpath;
        // persistStatic() must swallow any Handler / Looper bring-up failure.
        Logger.i("AuditTag", "no-FileLog")
        Logger.w("AuditTag", "no-FileLog")
        Logger.e("AuditTag", "no-FileLog")
    }
}
