/*
 * Copyright (C) 2026 MURENA SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
/*
 * Bliss touchpoint(s) (Audit02):
 *   - Reads DrawerFolderWithItems from :bliss-folders-data through
 *     DrawerFolderService.
 *   - Remains in the app source-set because it joins persisted folder rows
 *     with live AllAppsStore/AppInfo/FolderInfo launcher model classes.
 */
/*
 * File:    bliss/src/foundation/e/bliss/allapps/BlissAlphabeticalAppsList.kt
 * Module:  bliss root app source-set
 * Role:    Extends alphabetical apps list to inject drawer folder cards before the flat app grid.
 */
package foundation.e.bliss.allapps

import android.content.Context
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.allapps.AllAppsStore
import com.android.launcher3.allapps.AlphabeticalAppsList
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem
import com.android.launcher3.allapps.PrivateProfileManager
import com.android.launcher3.allapps.WorkProfileManager
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.util.Executors
import com.android.launcher3.views.ActivityContext
import foundation.e.bliss.folders.DrawerFolderService
import foundation.e.bliss.folders.db.DrawerFolderWithItems
import foundation.e.bliss.suggestions.AppUsageStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Bliss subclass of [AlphabeticalAppsList] that injects drawer-folder cards before the flat app
 * grid. See the Migration02 drawer-folders plan.
 *
 * Key behaviours (XC-2, XC-4, XC-5, XC-7):
 * - Folder cards appear only on the main drawer tab. The work-profile and search lists
 *   short-circuit via the `enableFolders` boolean: those callers pass `enableFolders=false`.
 * - Cold-start: we re-render whenever either the apps store or the folder Flow emits, since either
 *   one being late can leave a folder rendered with no resolved members.
 * - Idempotent: persistence is in [DrawerFolderService] via `INSERT OR REPLACE`.
 */
class BlissAlphabeticalAppsList<T>(
    context: T,
    appsStore: AllAppsStore<T>?,
    workProfileManager: WorkProfileManager?,
    privateProfileManager: PrivateProfileManager?,
    private val enableFolders: Boolean = true,
) : AlphabeticalAppsList<T>(context, appsStore, workProfileManager, privateProfileManager)
    where T : Context, T : ActivityContext {

    private val service: DrawerFolderService = DrawerFolderService.get(context.applicationContext)
    // PLAN-DRIFT-M02: Bliss launcher is not a
    // LifecycleOwner so we manage the scope ourselves and tear down via a store-update listener.
    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + supervisor)
    private var folders: List<FolderInfo> = emptyList()
    @Volatile private var rawFolders: List<DrawerFolderWithItems> = emptyList()
    private var collectJob: Job? = null
    private val storeListener: AllAppsStore.OnUpdateListener =
        AllAppsStore.OnUpdateListener { recomputeFoldersFromCache() }

    init {
        if (enableFolders) {
            startObserving()
        }
    }

    private fun startObserving() {
        // Re-resolve when the AllAppsStore updates so component-key → AppInfo joins succeed
        // even when the store hydrates after the first folder emission.
        mAllAppsStore?.addUpdateListener(storeListener)
        collectJob =
            scope.launch {
                service.observeFolders().collectLatest { latest ->
                    rawFolders = latest
                    recomputeFoldersFromCache()
                }
            }
    }

    private fun recomputeFoldersFromCache() {
        val store = mAllAppsStore
        folders =
            if (store != null) {
                service.toFolderInfos(rawFolders, store)
            } else {
                emptyList()
            }
        // Re-render the adapter on the main thread — the AllAppsStore listener may fire on
        // MODEL_EXECUTOR.
        Executors.MAIN_EXECUTOR.execute { onAppsUpdated() }
    }

    override fun addAppsWithSections(appList: MutableList<AppInfo>?, startPosition: Int): Int {
        // Inject the always-visible suggestions header before any folder cards / app sections.
        // We compute it independently of the drawer-folder pref so it works even when drawer
        // folders are disabled.
        var position = startPosition
        position = injectSuggestionsHeader(appList, position)

        if (!enableFolders || folders.isEmpty() || appList.isNullOrEmpty()) {
            return super.addAppsWithSections(appList, position)
        }
        val prefsEnabled =
            try {
                LauncherPrefs.get(mActivityContext as Context)
                    .get(LauncherPrefs.DRAWER_FOLDERS_ENABLED)
            } catch (t: Throwable) {
                false
            }
        if (!prefsEnabled) {
            return super.addAppsWithSections(appList, position)
        }

        val foldedKeys = HashSet<String>()
        for (folder in folders) {
            mAdapterItems.add(AdapterItem.asFolder(folder))
            position++
            for (item in folder.contents) {
                item.componentKey?.toString()?.let(foldedKeys::add)
            }
        }
        val remaining =
            appList
                .filterNot { app ->
                    app.componentKey?.toString()?.let(foldedKeys::contains) == true
                }
                .toMutableList()
        return super.addAppsWithSections(remaining, position)
    }

    /**
     * Resolve top-N most-used apps from [AppUsageStats] and inject a single `VIEW_TYPE_SUGGESTIONS`
     * adapter item at the head of the list. Returns the new position so the caller can continue
     * appending. Honors [LauncherPrefs.SHOW_SUGGESTED_APPS]; quietly returns the unchanged position
     * if the pref is off, the usage-stats permission is missing (AppUsageStats.usageStats returns
     * empty), or no candidate package is resolvable from the AllAppsStore.
     */
    private fun injectSuggestionsHeader(appList: List<AppInfo>?, startPosition: Int): Int {
        if (!enableFolders) return startPosition // search/work tabs skip suggestions too
        val ctx = (mActivityContext as? Context) ?: return startPosition
        val showSuggestions =
            try {
                LauncherPrefs.get(ctx).get(LauncherPrefs.SHOW_SUGGESTED_APPS)
            } catch (_: Throwable) {
                return startPosition
            }
        if (!showSuggestions) return startPosition
        if (appList.isNullOrEmpty()) return startPosition

        val stats =
            try {
                AppUsageStats(ctx).usageStats
            } catch (_: Throwable) {
                return startPosition
            }
        if (stats.isEmpty()) return startPosition

        val resolved =
            stats.take(MAX_SUGGESTIONS).mapNotNull { stat ->
                appList.firstOrNull { it.componentName?.packageName == stat.packageName }
            }
        if (resolved.isEmpty()) return startPosition

        // PLAN-DRIFT-M02: §7.3 — insert at index 0 so the row is the very first adapter row,
        // ahead of any drawer folders / sections (the plan example uses `add(0, ...)`).
        mAdapterItems.add(0, AdapterItem.asSuggestions(resolved))
        return startPosition + 1
    }

    companion object {
        private const val MAX_SUGGESTIONS = 4
    }

    /** Cancel observers; called when the launcher activity is being destroyed. */
    fun release() {
        try {
            mAllAppsStore?.removeUpdateListener(storeListener)
        } catch (_: Throwable) {}
        collectJob?.cancel()
        supervisor.cancel()
    }
}
