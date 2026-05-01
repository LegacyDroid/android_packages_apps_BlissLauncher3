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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Drawer-folder header row.
 *
 * `rank` controls ordering of folder cards within the drawer. `hide` is reserved (Lawnchair has a
 * "hide-this-folder" toggle); we keep the column for parity but the Phase 1 UI does not yet expose
 * it.
 */
@Entity(tableName = "drawer_folders")
data class DrawerFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val hide: Boolean = false,
    val rank: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * One app inside a drawer folder.
 *
 * `componentKey` is the launcher's standard `ComponentKey.toString()` form (`pkg/cls#userId`),
 * which the AllAppsStore can later resolve to a live AppInfo at render time.
 */
@Entity(
    tableName = "drawer_folder_items",
    foreignKeys =
        [
            ForeignKey(
                entity = DrawerFolderEntity::class,
                parentColumns = ["id"],
                childColumns = ["folderId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index(value = ["folderId"])],
)
data class DrawerFolderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val folderId: Int,
    val rank: Int = 0,
    @ColumnInfo(name = "component_key") val componentKey: String,
    val timestamp: Long = System.currentTimeMillis(),
)
