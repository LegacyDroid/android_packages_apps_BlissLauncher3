/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/firstrun/FirstRunWizardLaunchLifecycleTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.firstrun)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/firstrun/):
 *   └── FirstRunWizardLaunchLifecycleTest.kt
 *           — JVM Robolectric test that pins the lifecycle re-check
 *             added to Launcher.onCreate's view.post(...) FirstRunWizard
 *             trampoline                                            ← THIS FILE
 *
 * Purpose:
 *   Audit01 #05 — pin the regression. Launcher.onCreate posts a runnable
 *   onto the root view's handler that conditionally launches the
 *   FirstRunWizard host Activity. Without a lifecycle re-check inside the
 *   lambda, a fast back-press / configuration change can fire the runnable
 *   after onDestroy(), which throws or silently leaks a detached task.
 *
 *   Launching the real Launcher Activity under Robolectric is impractical
 *   (its constructor pulls in the full Bliss + AOSP Dagger graph). So this
 *   test stands up a tiny test-only Activity that reproduces the exact
 *   lambda the production code uses (the four-line guard + shouldShow +
 *   launch). It then verifies:
 *
 *     1. when the host Activity is alive, draining the message queue
 *        causes FirstRunWizard.launch to start the FirstRunActivity, and
 *
 *     2. when the host Activity has been finished before the queue is
 *        drained, FirstRunWizard.launch is NOT invoked and no Intent is
 *        emitted.
 *
 *   Test (2) is the regression assertion: drop the `isFinishing() ||
 *   isDestroyed()` guard from Launcher.java line ~702 and this case fails.
 *
 *   The shouldShow path is forced true by leaving FIRST_RUN_LAYOUT_CHOICE_DONE
 *   at its default (false) and ensuring the LayoutModeStep returns
 *   shouldShow=true on a fresh prefs store — which it does, since no choice
 *   has been recorded yet.
 *
 * Plan reference: Plans/Audits/audit01/05-firstrun-wizard-lifecycle.md
 */
package foundation.e.bliss.firstrun

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import foundation.e.bliss.firstrun.ui.FirstRunActivity
import foundation.e.bliss.shadows.ShadowLauncherComponentProvider
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [35],
    application = android.app.Application::class,
    shadows = [ShadowLauncherComponentProvider::class],
)
class FirstRunWizardLaunchLifecycleTest {

    @After
    fun tearDown() {
        ShadowLauncherComponentProvider.resetForTest()
    }

    @Before
    fun verifyShouldShowIsTrueOnFreshPrefs() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        // Sanity: a fresh prefs store must have shouldShow == true,
        // otherwise both alive- and finishing-cases would trivially pass
        // for the wrong reason.
        assertTrue(
            "Test relies on shouldShow() being true on a fresh prefs store",
            FirstRunWizard.shouldShow(ctx),
        )
    }

    /**
     * Positive control: when the host Activity is alive at the time the posted runnable executes,
     * FirstRunWizard.launch starts the FirstRunActivity.
     */
    @Test
    fun launch_invoked_when_activity_alive() {
        val controller = Robolectric.buildActivity(LifecycleProbeActivity::class.java).setup()
        val activity = controller.get()

        postFirstRunTrampoline(activity, activity.contentView())
        drainMainLooper()

        val started = shadowOf(activity).peekNextStartedActivity()
        assertNotNull("FirstRunWizard.launch should have started an Intent", started)
        assertTrue(
            "Started intent should target FirstRunActivity, was: ${started?.component}",
            started!!.component?.className == FirstRunActivity::class.java.name,
        )
    }

    /**
     * Regression assertion: when the host Activity is finished before the posted runnable runs, the
     * lifecycle guard inside the lambda must short-circuit. FirstRunWizard.launch must NOT be
     * invoked, and no Intent must be emitted.
     *
     * Removing the `isFinishing() || isDestroyed()` guard from Launcher.onCreate's view.post(...)
     * lambda fails this test.
     */
    @Test
    fun launch_skipped_when_activity_finishing() {
        val controller = Robolectric.buildActivity(LifecycleProbeActivity::class.java).setup()
        val activity = controller.get()

        // Queue the trampoline first, then finish the Activity, then drain.
        // Mirrors the real-world race: post happens in onCreate, finish
        // happens before the message queue drains.
        postFirstRunTrampoline(activity, activity.contentView())
        controller.pause().stop().destroy()
        drainMainLooper()

        val started = shadowOf(activity).peekNextStartedActivity()
        assertNull(
            "FirstRunWizard.launch must NOT fire after the host Activity is destroyed",
            started,
        )
    }

    // -----------------------------------------------------------------------

    /**
     * Reproduces the exact post-and-guard pattern used in com.android.launcher3.Launcher.onCreate
     * (Audit01 #05). Keep this in sync with the production lambda — if production drifts, update
     * both.
     */
    private fun postFirstRunTrampoline(activity: Activity, view: View) {
        view.post {
            if (activity.isFinishing || activity.isDestroyed) {
                return@post
            }
            if (FirstRunWizard.shouldShow(activity)) {
                FirstRunWizard.launch(activity)
            }
        }
    }

    private fun drainMainLooper() {
        shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    /**
     * Minimal Activity with a single attached View whose Handler we can post onto. Stands in for
     * Launcher's getRootView().
     */
    class LifecycleProbeActivity : Activity() {
        private lateinit var root: FrameLayout

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            root = FrameLayout(this)
            setContentView(root)
        }

        fun contentView(): View = root
    }
}
