/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/folder/GridFolderControllerLifetimeTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.folder)
 * Role:    NEW
 *
 * Purpose:
 *   Pin Audit01 #11: GridFolderController.release() must unregister the
 *   LauncherAppMonitorCallback it registered in init {} so the next-launcher
 *   recreation does not leave two controllers firing on onReceiveHomeIntent.
 *
 * Plan reference: Plans/Audits/audit01/11-monitor-callback-lifetime.md
 */
package foundation.e.bliss.folder

import androidx.test.core.app.ApplicationProvider
import foundation.e.bliss.LauncherAppMonitor
import foundation.e.bliss.LauncherAppMonitorCallback
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class GridFolderControllerLifetimeTest {

    @Test
    fun release_unregisters_callback_from_monitor() {
        val monitor = mock(LauncherAppMonitor::class.java)
        val controller = GridFolderController(ApplicationProvider.getApplicationContext(), monitor)
        verify(monitor).registerCallback(any(LauncherAppMonitorCallback::class.java))
        controller.release()
        verify(monitor).unregisterCallback(any(LauncherAppMonitorCallback::class.java))
    }
}
