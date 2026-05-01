/*
 * Copyright (C) 2026 MURENA SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package foundation.e.bliss.folders.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawerFolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: DrawerFolderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<DrawerFolderItemEntity>)

    @Query("SELECT * FROM drawer_folders ORDER BY rank ASC, id ASC")
    fun observeFolders(): Flow<List<DrawerFolderEntity>>

    @Transaction
    @Query("SELECT * FROM drawer_folders WHERE id = :id")
    suspend fun getFolderWithItems(id: Int): DrawerFolderWithItems?

    @Transaction
    @Query("SELECT * FROM drawer_folders ORDER BY rank ASC, id ASC")
    fun observeFoldersWithItems(): Flow<List<DrawerFolderWithItems>>

    @Transaction
    @Query("SELECT * FROM drawer_folders ORDER BY rank ASC, id ASC")
    suspend fun getAllFoldersWithItems(): List<DrawerFolderWithItems>

    @Query(
        "UPDATE drawer_folders SET title = :title, hide = :hide, rank = :rank, " +
            "timestamp = :ts WHERE id = :id"
    )
    suspend fun updateFolderInfo(
        id: Int,
        title: String,
        hide: Boolean,
        rank: Int,
        ts: Long = System.currentTimeMillis(),
    )

    @Query("DELETE FROM drawer_folder_items WHERE folderId = :folderId")
    suspend fun deleteItemsForFolder(folderId: Int)

    @Query("DELETE FROM drawer_folders WHERE id = :id") suspend fun deleteFolder(id: Int)
}

data class DrawerFolderWithItems(
    @Embedded val folder: DrawerFolderEntity,
    @Relation(parentColumn = "id", entityColumn = "folderId")
    val items: List<DrawerFolderItemEntity>,
)
