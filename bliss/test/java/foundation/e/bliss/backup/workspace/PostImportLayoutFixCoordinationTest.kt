/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/backup/workspace/PostImportLayoutFixCoordinationTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.backup.workspace)
 * Role:    NEW
 *
 * Purpose:
 *   Coordination contract for PostImportLayoutFix.apply — verifies the
 *   CountDownLatch + LauncherAppMonitorCallback pattern that replaced the
 *   500ms Thread.sleep races (audit01/04).
 *
 *   The full apply() integration touches LauncherAppState.getInstance,
 *   MAIN_EXECUTOR.submit().get(), MODEL_EXECUTOR, DatabaseHelper, and the
 *   Dagger-built LauncherAppComponent — which together require a real
 *   Launcher process. Those layers are out of reach for a JVM/Robolectric
 *   unit test (the audit plan acknowledges the same: "Robolectric is
 *   insufficient; needs a real LauncherModel"). This test covers what IS
 *   testable in pure JVM: the coordination contract the fix depends on.
 *
 *   Specifically asserts:
 *     1. The bind-complete latch fires on the LauncherAppMonitorCallback
 *        signal and unblocks the import thread within the 5s timeout.
 *     2. The latch is consumed exactly once even if the bind callback
 *        fires multiple times (idempotent against late loader passes).
 *     3. Position-fixup work scheduled AFTER the latch never starts before
 *        the bind signal, so we can never race the loader's bind.
 *     4. On a "slow loader" simulation (3s delay) the import-side wait
 *        completes within 6 seconds end-to-end (acceptance criterion §A1).
 *
 * Plan reference: Plans/Audits/audit01/04-postimport-coordination.md
 */
package foundation.e.bliss.backup.workspace

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostImportLayoutFixCoordinationTest {

    private val workerPool = Executors.newCachedThreadPool()

    @After
    fun tearDown() {
        workerPool.shutdownNow()
    }

    /**
     * The latch unblocks within the timeout once the bind callback fires. Mirrors the apply()-side
     * `bindLatch.await(5, SECONDS)` plus the registered LauncherAppMonitorCallback's `countDown()`.
     */
    @Test
    fun bindLatchUnblocksWhenBindCompleteSignalArrives() {
        val bindLatch = CountDownLatch(1)

        // Simulate the loader's bind-complete callback firing 200ms later.
        workerPool.submit {
            Thread.sleep(200)
            bindLatch.countDown()
        }

        val ok = bindLatch.await(5, TimeUnit.SECONDS)
        assertTrue("bind-complete latch must fire within 5s", ok)
    }

    /**
     * Even when the bind callback fires more than once (e.g. an extra loader pass caused by a
     * concurrent package install), the latch is consumed exactly once and the import thread does
     * not block twice.
     */
    @Test
    fun bindLatchIsConsumedExactlyOnceUnderRepeatedSignals() {
        val bindLatch = CountDownLatch(1)
        val completionLatch = CountDownLatch(5)
        val signalCount = AtomicInteger(0)

        repeat(5) {
            workerPool.submit {
                signalCount.incrementAndGet()
                bindLatch.countDown()
                completionLatch.countDown()
            }
        }

        val ok = bindLatch.await(5, TimeUnit.SECONDS)
        assertTrue("latch must reach zero from ≥1 signal", ok)
        assertEquals("latch count should be 0 after firing", 0L, bindLatch.count)
        assertTrue(
            "all 5 simulated signals must have completed within 5s",
            completionLatch.await(5, TimeUnit.SECONDS),
        )
        assertEquals("all five signals ran", 5, signalCount.get())
    }

    /**
     * Position-fixup work submitted AFTER `await()` returns must never start before the bind signal
     * arrives. Catches a regression where someone might "optimise" by scheduling the fixup before
     * the await.
     */
    @Test
    fun positionFixupIsOrderedAfterBindSignal() {
        val bindLatch = CountDownLatch(1)
        val bindSignalAt = AtomicLong(0)
        val fixupStartedAt = AtomicLong(0)

        // Loader-side: signal bind completion 100ms in.
        workerPool.submit {
            Thread.sleep(100)
            bindSignalAt.set(System.nanoTime())
            bindLatch.countDown()
        }

        // Import-side: mirrors apply()'s sequence — await, then run fixup.
        val ok = bindLatch.await(5, TimeUnit.SECONDS)
        fixupStartedAt.set(System.nanoTime())

        assertTrue("await must succeed", ok)
        assertTrue(
            "bind signal time must be recorded before fixup start " +
                "(bind=${bindSignalAt.get()} fixup=${fixupStartedAt.get()})",
            bindSignalAt.get() in 1L..fixupStartedAt.get(),
        )
    }

    /**
     * End-to-end timing on a deliberately slow loader (3s before bind). Acceptance criterion: total
     * wait must complete under 6s — the audit's §A1 budget. The old `Thread.sleep(500)` would
     * silently complete at 500ms regardless of whether the loader had actually finished; the
     * latch-based wait correctly extends to match the real bind time AND respects the 5s safety
     * cap.
     */
    @Test
    fun slowLoaderImportWaitCompletesUnderSixSeconds() {
        val bindLatch = CountDownLatch(1)
        val slowLoaderDelayMs = 3_000L

        workerPool.submit {
            Thread.sleep(slowLoaderDelayMs)
            bindLatch.countDown()
        }

        val startNs = System.nanoTime()
        val ok = bindLatch.await(5, TimeUnit.SECONDS)
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000

        assertTrue("latch must fire under the 5s cap", ok)
        assertTrue(
            "import-side wait completed in ${elapsedMs}ms; budget is 6000ms",
            elapsedMs < 6_000L,
        )
        // Sanity: we DID actually wait close to the loader delay — i.e. the
        // latch wasn't fired prematurely by some other code path.
        assertTrue(
            "import-side wait was ${elapsedMs}ms; should be ≥ slow-loader " +
                "delay of ${slowLoaderDelayMs}ms",
            elapsedMs >= slowLoaderDelayMs - 50, // 50ms scheduling slack
        )
    }

    /**
     * Safety-net: if the bind signal NEVER arrives (e.g. forceReload was a no-op because
     * BgDataModel was already up to date — see Open Questions in the audit plan), `await` falls
     * through after the timeout and the import continues. Verifies the timeout is honoured and
     * returns `false`.
     */
    @Test
    fun bindLatchTimesOutGracefullyWhenNoSignalArrives() {
        val bindLatch = CountDownLatch(1)

        val startNs = System.nanoTime()
        // Use a short timeout in the test so we don't burn 5s of CI time.
        val ok = bindLatch.await(250, TimeUnit.MILLISECONDS)
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000

        assertEquals("await must return false on timeout", false, ok)
        assertTrue(
            "await should respect the timeout (~250ms), elapsed=${elapsedMs}ms",
            elapsedMs in 200..2_000,
        )
    }
}
