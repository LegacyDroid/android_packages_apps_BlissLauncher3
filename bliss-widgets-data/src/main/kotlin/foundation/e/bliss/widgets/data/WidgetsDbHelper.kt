/*
 * Copyright (C) 2025 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss-widgets-data/src/main/kotlin/foundation/e/bliss/widgets/data/WidgetsDbHelper.kt
 * Module:  :bliss-widgets-data
 * Role:    SQLite helper persisting widget configuration. Internal to module.
 */
package foundation.e.bliss.widgets.data

import android.content.ComponentName
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class WidgetsDbHelper(context: Context) :
    SQLiteOpenHelper(context, WIDGETS_DB, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $WIDGETS_TABLE (" +
                "position INTEGER NOT NULL," +
                "component TEXT NOT NULL," +
                "height INTEGER NOT NULL," +
                "widgetId INTEGER NOT NULL PRIMARY KEY" +
                ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // No-op: single version.
    }

    fun getWidgets(): List<WidgetInfo> {
        val widgets = mutableListOf<WidgetInfo>()
        writableDatabase?.use { db ->
            db.rawQuery("SELECT * FROM $WIDGETS_TABLE", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val position = cursor.getInt(cursor.getColumnIndexOrThrow("position"))
                    val component = cursor.getString(cursor.getColumnIndexOrThrow("component"))
                    val widgetId = cursor.getInt(cursor.getColumnIndexOrThrow("widgetId"))
                    val height = cursor.getInt(cursor.getColumnIndexOrThrow("height"))
                    widgets.add(
                        WidgetInfo(
                            position,
                            ComponentName.unflattenFromString(component)!!,
                            widgetId,
                            height,
                        )
                    )
                }
            }
        }
        return widgets
    }

    fun delete(id: Int) {
        writableDatabase.use { db ->
            db.apply {
                execSQL("DELETE FROM $WIDGETS_TABLE WHERE widgetId = ?", arrayOf(id))
                execSQL(
                    "UPDATE $WIDGETS_TABLE SET position = position - 1 WHERE widgetId > ?",
                    arrayOf(id),
                )
            }
        }
    }

    fun insert(info: WidgetInfo) {
        writableDatabase.use { db ->
            db.execSQL(
                "INSERT OR REPLACE INTO $WIDGETS_TABLE (position, component, widgetId, height) VALUES (?, ?, ?, ?)",
                arrayOf(info.position, info.component.flattenToString(), info.widgetId, info.height),
            )
        }
    }

    fun updateHeight(id: Int, height: Int) {
        writableDatabase.use { db ->
            db.execSQL(
                "UPDATE $WIDGETS_TABLE SET height = ? WHERE widgetId = ?",
                arrayOf(height, id),
            )
        }
    }

    fun getWidgetHeight(id: Int): Int? {
        var height: Int? = null
        writableDatabase?.use { db ->
            db.rawQuery(
                "SELECT height FROM $WIDGETS_TABLE WHERE widgetId = ?",
                arrayOf(id.toString()),
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    height = cursor.getInt(cursor.getColumnIndexOrThrow("height"))
                }
            }
        }
        return height
    }

    companion object {
        private const val DATABASE_VERSION = 1
        private const val WIDGETS_DB = "qsb_widgets"
        private const val WIDGETS_TABLE = "widget_items"
    }
}
