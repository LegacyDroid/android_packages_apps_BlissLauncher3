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
 * File:    bliss/src/foundation/e/bliss/LauncherAppMonitorCallback.kt
 * Module:  bliss root app source-set
 * Role:    Interface providing optional callbacks for launcher lifecycle and system event observers.
 */
package foundation.e.bliss

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.database.sqlite.SQLiteDatabase
import android.os.UserHandle
import com.android.launcher3.Launcher
import com.android.launcher3.model.data.AppInfo
import java.io.FileDescriptor
import java.io.PrintWriter

@JvmDefaultWithCompatibility
interface LauncherAppMonitorCallback {
    // Launcher activity Callbacks
    fun onLauncherPreCreate(launcher: Launcher?) {
        // no-op default; concrete monitors override the launcher-lifecycle events they observe
    }

    fun onLauncherCreated() {
        // no-op default; concrete monitors override the launcher-lifecycle events they observe
    }

    fun onLauncherPreResume() {
        // no-op default; concrete monitors override the launcher-lifecycle events they observe
    }

    fun onLauncherResumed() {
        // no-op default; concrete monitors override the launcher-lifecycle events they observe
    }

    fun onLauncherStart() {
        // no-op default; concrete monitors override the launcher-lifecycle events they observe
    }

    fun onLauncherStop() {
        // no-op default; concrete monitors override the launcher-lifecycle events they observe
    }

    fun onLauncherPrePause() {
        // no-op default; concrete monitors override the launcher-lifecycle events they observe
    }

    fun onLauncherPaused() {
        // no-op default; concrete monitors override the launcher-lifecycle events they observe
    }

    fun onLauncherDestroy(launcher: Launcher) {
        // no-op default; concrete monitors override the launcher-lifecycle events they observe
    }

    fun onLauncherStyleChanged(style: String) {
        // no-op default; concrete monitors override the launcher-lifecycle events they observe
    }

    fun onLauncherRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>?,
        grantResults: IntArray?,
    ) {
        // no-op default; concrete monitors override the launcher-lifecycle events they observe
    }

    fun onLauncherFocusChanged(hasFocus: Boolean) {
        // no-op default; concrete monitors override the launcher-lifecycle events they observe
    }

    // Launcher app Callbacks
    fun onAppCreated(context: Context?) {
        // no-op default; concrete monitors override the app/system events they care about
    }

    fun onReceive(intent: Intent?) {
        // no-op default; concrete monitors override the app/system events they care about
    }

    fun onReceive() {
        // no-op default; concrete monitors override the app/system events they care about
    }

    fun onUIConfigChanged() {
        // no-op default; concrete monitors override the app/system events they care about
    }

    fun onThemeChanged() {
        // no-op default; concrete monitors override the app/system events they care about
    }

    fun onAppSharedPreferenceChanged(key: String?) {
        // no-op default; concrete monitors override the app/system events they care about
    }

    fun onPackageRemoved(packageName: String?, user: UserHandle?) {
        // no-op default; concrete monitors override the package events they observe
    }

    fun onPackageAdded(packageName: String?, user: UserHandle?) {
        // no-op default; concrete monitors override the package events they observe
    }

    fun onPackageChanged(packageName: String?, user: UserHandle?) {
        // no-op default; concrete monitors override the package events they observe
    }

    fun onPackagesAvailable(packageNames: Array<String?>?, user: UserHandle?, replacing: Boolean) {
        // no-op default; concrete monitors override the package events they observe
    }

    fun onPackagesUnavailable(
        packageNames: Array<String?>?,
        user: UserHandle?,
        replacing: Boolean,
    ) {
        // no-op default; concrete monitors override the package events they observe
    }

    fun onPackagesSuspended(packageNames: Array<String?>?, user: UserHandle?) {
        // no-op default; concrete monitors override the package events they observe
    }

    fun onPackagesUnsuspended(packageNames: Array<String?>?, user: UserHandle?) {
        // no-op default; concrete monitors override the package events they observe
    }

    fun onShortcutsChanged(
        packageName: String?,
        shortcuts: List<ShortcutInfo?>?,
        user: UserHandle?,
    ) {
        // no-op default; concrete monitors override the package events they observe
    }

    fun onAllAppsListUpdated(apps: List<AppInfo?>?) {
        // no-op default; concrete monitors override the app-list events they observe
    }

    fun onLauncherLocaleChanged() {
        // no-op default; concrete monitors override the configuration events they observe
    }

    fun onLauncherOrientationChanged() {
        // no-op default; concrete monitors override the configuration events they observe
    }

    fun onLauncherScreensizeChanged() {
        // no-op default; concrete monitors override the configuration events they observe
    }

    fun onLoadAllAppsEnd(apps: ArrayList<AppInfo?>?) {
        // no-op default; concrete monitors override the app-list events they observe
    }

    // Launcher database callbacks
    fun dump(prefix: String?, fd: FileDescriptor?, w: PrintWriter?, dumpAll: Boolean) {
        // no-op default; concrete monitors override dump if they want to contribute diagnostics
    }

    fun dump(prefix: String, fd: FileDescriptor, w: PrintWriter, args: Array<String>) {
        // no-op default; concrete monitors override dump if they want to contribute diagnostics
    }

    fun onLauncherDbUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // no-op default; concrete monitors override the database events they observe
    }

    fun onReceiveHomeIntent() {
        // no-op default; concrete monitors override the launcher-binding events they observe
    }

    fun onLauncherWorkspaceBindingFinish() {
        // no-op default; concrete monitors override the launcher-binding events they observe
    }

    fun onLauncherAllAppBindingFinish(apps: Array<AppInfo>) {
        // no-op default; concrete monitors override the launcher-binding events they observe
    }
}
