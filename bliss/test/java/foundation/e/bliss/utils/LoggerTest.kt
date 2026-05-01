/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/utils/LoggerTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.utils)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/utils/):
 *   └── LoggerTest.kt          — JVM unit test for Logger / FileLog backing  ← THIS FILE
 *
 * Purpose:
 *   Verifies Migration05 follow-up F1: that Logger.tag(...).i/w/e calls do
 *   NOT throw when AOSP's FileLog has never been initialised via setDir(),
 *   which is the exact state of the JVM unit-test classpath. The persist()
 *   helper inside Logger swallows any Throwable from FileLog's Handler /
 *   Looper bring-up, so the test exercises that swallow path.
 *
 * Plan reference: Plans/Migration05 follow-up F1 (FileLog integration)
 */
package foundation.e.bliss.utils

import org.junit.Test

class LoggerTest {

    @Test
    fun infoLevel_doesNotThrow_whenFileLogUninitialised() {
        Logger.tag("LoggerTest").i("hello")
    }

    @Test
    fun warnLevel_withThrowable_doesNotThrow() {
        Logger.tag("LoggerTest").w("oops", IllegalStateException("boom"))
    }

    @Test
    fun errorLevel_withNonExceptionThrowable_doesNotThrow() {
        // Guards the `else -> FileLog.print(tag, msg)` branch in persist()
        // for Throwable subclasses that aren't Exception (e.g. AssertionError).
        Logger.tag("LoggerTest").e("err", AssertionError("assertion"))
    }

    @Test
    fun debugLevel_isLogcatOnly_andDoesNotThrow() {
        Logger.tag("LoggerTest").d("debug-msg")
    }
}
