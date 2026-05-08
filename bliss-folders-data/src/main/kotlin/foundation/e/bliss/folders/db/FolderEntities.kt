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
 * File:    bliss-folders-data/src/main/kotlin/foundation/e/bliss/folders/db/FolderEntities.kt
 * Module:  :bliss-folders-data
 * Role:    Room entities for drawer-folder headers and member app rows.
 *
 * Owned by:
 *   - :bliss-folders-data
 *
 * Consumed by:
 *   - foundation.e.bliss.folders.DrawerFolderService in the app source-set.
 *
 * Dependency rules:
 *   - May use AndroidX Room annotations and primitive persisted values.
 *   - Must not import com.android.launcher3.* runtime model/UI classes.
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
 * "hide-this-folder" toggle); we keep the column for parity but the current UI does not yet
 * expose it.
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
