/*
 * Copyright (C) 2025 MURENA SAS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */
/*
 * File:    bliss/src/foundation/e/bliss/multimode/MultiModeController.kt
 * Module:  bliss root app source-set
 * Role:    Controller managing single-layer mode and idle app verification lifecycle.
 */
package foundation.e.bliss.multimode

import android.content.Context
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.util.Executors.MODEL_EXECUTOR
import foundation.e.bliss.BaseController
import foundation.e.bliss.LauncherAppMonitor
import foundation.e.bliss.LauncherAppMonitorCallback
import foundation.e.bliss.blur.BlurWallpaperProvider
import java.io.FileDescriptor
import java.io.PrintWriter

class MultiModeController(val context: Context, val monitor: LauncherAppMonitor) : BaseController {
    private val idp by lazy { InvariantDeviceProfile.INSTANCE.get(context) }
    private val mAppMonitorCallback: LauncherAppMonitorCallback =
        object : LauncherAppMonitorCallback {
            private var cachedApps: ArrayList<AppInfo?>? = null

            override fun onLoadAllAppsEnd(apps: ArrayList<AppInfo?>?) {
                val launcherModel = monitor.launcher?.model
                if (launcherModel != null) {
                    MODEL_EXECUTOR.submit(
                        VerifyIdleAppTask(
                            context,
                            apps,
                            null,
                            null,
                            false,
                            launcherModel.mBgDataModel,
                        )
                    )
                } else {
                    cachedApps = apps
                }
            }

            override fun onLauncherCreated() {
                super.onLauncherCreated()
                cachedApps?.let {
                    onLoadAllAppsEnd(it)
                    cachedApps = null
                }
            }

            // A live model reload cannot rebuild the launcher's structure for a
            // single-layer mode change (drawer vs. no drawer, QSB first page,
            // hotseat search) — those views are wired up in Launcher.onCreate().
            // Both entry points that change the mode therefore restart the
            // launcher cleanly instead: SingleLayerModeController (settings
            // toggle) and FirstRunActivity (first-run wizard). Backup/restore and
            // Lawnchair import issue their own explicit forceReload() calls. So
            // there is deliberately no onAppSharedPreferenceChanged override here.

            override fun onLauncherOrientationChanged() {
                BlurWallpaperProvider.getInstance(context).orientationChanged()
            }

            override fun dump(
                prefix: String?,
                fd: FileDescriptor?,
                w: PrintWriter?,
                dumpAll: Boolean,
            ) {
                w?.let {
                    println()
                    println("$prefix $TAG: ${this@MultiModeController}")
                }
            }
        }

    init {
        prefs = LauncherPrefs.get(context)
        monitor.registerCallback(mAppMonitorCallback)
    }

    override fun dumpState(
        prefix: String?,
        fd: FileDescriptor?,
        writer: PrintWriter?,
        dumpAll: Boolean,
    ) {
        writer?.let {
            println()
            println("$prefix $TAG: ${this@MultiModeController}")
        }
    }

    companion object {
        private const val TAG = "MultiModeController"
        private lateinit var prefs: LauncherPrefs

        @JvmStatic
        val isSingleLayerMode
            get() =
                if (::prefs.isInitialized) {
                    prefs.get(LauncherPrefs.IS_SINGLE_LAYER_ENABLED)
                } else false

        @JvmStatic
        val isNotifCountEnabled: Boolean
            get() =
                if (::prefs.isInitialized) {
                    prefs.get(LauncherPrefs.IS_NOTIF_COUNT_ENABLED)
                } else true
    }
}
