/*
 * Copyright (C) 2026 MURENA SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package foundation.e.bliss.folders

import android.content.Context
import com.android.launcher3.allapps.AllAppsStore
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.util.ComponentKey
import foundation.e.bliss.folders.db.DrawerFolderDatabase
import foundation.e.bliss.folders.db.DrawerFolderEntity
import foundation.e.bliss.folders.db.DrawerFolderItemEntity
import foundation.e.bliss.folders.db.DrawerFolderWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Bridges the Room-backed drawer-folder store to launcher runtime types.
 *
 * Resolution of [ComponentKey] -> [AppInfo] requires an [AllAppsStore] which is owned by the drawer
 * view. The service therefore exposes the raw [DrawerFolderWithItems] flow so the consumer
 * (`BlissAlphabeticalAppsList`) can do that join itself with the live store. We also offer a
 * convenience [toFolderInfo] that takes a store and produces a launcher-side [FolderInfo].
 *
 * See `Plans/Migration02/11-cross-cutting.md` (XC-4) for the AllAppsStore-lifetime reasoning.
 */
class DrawerFolderService private constructor(private val context: Context) {

    private val dao = DrawerFolderDatabase.get(context).drawerFolderDao()

    /** Reactive list of folders with their member component keys. */
    fun observeFolders(): Flow<List<DrawerFolderWithItems>> = dao.observeFoldersWithItems()

    suspend fun getAllFolders(): List<DrawerFolderWithItems> =
        withContext(Dispatchers.IO) { dao.getAllFoldersWithItems() }

    /**
     * Insert/update a folder. If [folderId] is 0, Room auto-assigns one. Members are *replaced* so
     * imports are idempotent (XC-7).
     */
    suspend fun upsert(folderId: Int, title: String, rank: Int, contents: List<ComponentKey>): Int =
        withContext(Dispatchers.IO) {
            val existingId = if (folderId > 0) folderId else 0
            val newRowId =
                dao.insertFolder(DrawerFolderEntity(id = existingId, title = title, rank = rank))
            // When REPLACE collides on PK Room returns the existing rowId; when 0 it
            // auto-generates.
            val effectiveId = if (existingId > 0) existingId else newRowId.toInt()
            dao.deleteItemsForFolder(effectiveId)
            if (contents.isNotEmpty()) {
                dao.insertItems(
                    contents.mapIndexed { i, key ->
                        DrawerFolderItemEntity(
                            folderId = effectiveId,
                            rank = i,
                            componentKey = key.toString(),
                        )
                    }
                )
            }
            effectiveId
        }

    suspend fun rename(folderId: Int, title: String) =
        withContext(Dispatchers.IO) {
            val existing = dao.getFolderWithItems(folderId) ?: return@withContext
            dao.updateFolderInfo(folderId, title, existing.folder.hide, existing.folder.rank)
        }

    suspend fun reorder(folderId: Int, rank: Int) =
        withContext(Dispatchers.IO) {
            val existing = dao.getFolderWithItems(folderId) ?: return@withContext
            dao.updateFolderInfo(folderId, existing.folder.title, existing.folder.hide, rank)
        }

    suspend fun delete(folderId: Int) = withContext(Dispatchers.IO) { dao.deleteFolder(folderId) }

    /**
     * Synchronous Java-friendly equivalent of [upsert] used by the JSON→Room migration. Must be
     * called off the main thread (LauncherApplication uses MODEL_EXECUTOR).
     */
    @JvmName("upsertBlocking")
    fun upsertBlocking(folderId: Int, title: String, rank: Int, contents: List<ComponentKey>): Int =
        runBlocking {
            upsert(folderId, title, rank, contents)
        }

    /**
     * Convert a stored folder + its items into the launcher's [FolderInfo] model, resolving each
     * member through [store]. Items that the store can't currently resolve (uninstalled, profile
     * not present, etc.) are silently dropped — they reappear if the package returns.
     *
     * Returns null if no member could be resolved (caller likely wants to skip rendering).
     */
    fun toFolderInfo(from: DrawerFolderWithItems, store: AllAppsStore<*>): FolderInfo? {
        val info = FolderInfo()
        info.id = from.folder.id
        info.title = from.folder.title
        info.rank = from.folder.rank

        from.items
            .sortedBy { it.rank }
            .forEach { item ->
                val key = ComponentKey.fromString(item.componentKey) ?: return@forEach
                val app: AppInfo = store.getApp(key) ?: return@forEach
                // The launcher expects WorkspaceItemInfo inside a FolderInfo. AppInfo has a
                // direct conversion via makeWorkspaceItem on Bliss.
                info.add(WorkspaceItemInfo(app))
            }
        if (info.getContents().isEmpty()) return null
        return info
    }

    /**
     * Build [FolderInfo]s for every persisted drawer folder, dropping any folder whose members
     * cannot be resolved by [store] (e.g. cold-start where the store has not fully populated yet).
     */
    fun toFolderInfos(from: List<DrawerFolderWithItems>, store: AllAppsStore<*>): List<FolderInfo> =
        from.mapNotNull { toFolderInfo(it, store) }

    companion object {
        @Volatile private var instance: DrawerFolderService? = null

        @JvmStatic
        fun get(context: Context): DrawerFolderService =
            instance
                ?: synchronized(this) {
                    instance
                        ?: DrawerFolderService(context.applicationContext).also { instance = it }
                }
    }
}
