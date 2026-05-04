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
                            // Audit01 #02: never destructively wipe user drawer folders. Schema
                            // bumps must register a Migration via .addMigrations(...). The
                            // downgrade fallback is OK because side-grading APKs is a developer
                            // workflow, not a user one.
                            .fallbackToDestructiveMigrationOnDowngrade(true)
                            .build()
                            .also { instance = it }
                }
    }
}
