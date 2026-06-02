/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss-widgets-data/src/main/kotlin/foundation/e/bliss/widgets/data/WidgetRepository.kt
 * Module:  :bliss-widgets-data
 * Role:    Public repository API for widget persistence.
 */
package foundation.e.bliss.widgets.data

import android.content.Context

class WidgetRepository private constructor(context: Context) {
    private val db = WidgetsDbHelper(context.applicationContext)

    fun getWidgets(): List<WidgetInfo> = db.getWidgets()

    fun insert(info: WidgetInfo) = db.insert(info)

    fun delete(widgetId: Int) = db.delete(widgetId)

    fun updateHeight(widgetId: Int, height: Int) = db.updateHeight(widgetId, height)

    fun getWidgetHeight(widgetId: Int): Int? = db.getWidgetHeight(widgetId)

    companion object {
        @Volatile private var instance: WidgetRepository? = null

        @JvmStatic
        fun get(context: Context): WidgetRepository =
            instance ?: synchronized(this) {
                instance ?: WidgetRepository(context).also { instance = it }
            }
    }
}
