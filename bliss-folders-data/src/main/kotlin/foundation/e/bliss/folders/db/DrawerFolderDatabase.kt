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
 * File:    bliss-folders-data/src/main/kotlin/foundation/e/bliss/folders/db/DrawerFolderDatabase.kt
 * Module:  :bliss-folders-data
 * Role:    Room database singleton for persisted drawer folders.
 *
 * Owned by:
 *   - :bliss-folders-data
 *
 * Consumed by:
 *   - foundation.e.bliss.folders.DrawerFolderService in the app source-set.
 *
 * Dependency rules:
 *   - May use AndroidX Room and Android Context.
 *   - Must not import com.android.launcher3.* runtime model/UI classes.
 */
package foundation.e.bliss.folders.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DrawerFolderEntity::class, DrawerFolderItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DrawerFolderDatabase : RoomDatabase() {
    abstract fun drawerFolderDao(): DrawerFolderDao

    companion object {
        @Volatile private var instance: DrawerFolderDatabase? = null

        @JvmStatic
        fun get(context: Context): DrawerFolderDatabase =
            instance
                ?: synchronized(this) {
                    instance
                        ?: Room.databaseBuilder(
                                context.applicationContext,
                                DrawerFolderDatabase::class.java,
                                "drawer_folders.db",
                            )
                            .fallbackToDestructiveMigration(true)
                            .build()
                            .also { instance = it }
                }
    }
}
