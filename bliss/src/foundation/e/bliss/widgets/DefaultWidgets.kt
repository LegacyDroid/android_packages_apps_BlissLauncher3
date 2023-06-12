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
package foundation.e.bliss.widgets

import android.content.ComponentName
import android.content.Context
import foundation.e.bliss.utils.BlissDbUtils

object DefaultWidgets {
    private val ecloudWidget =
        ComponentName("foundation.e.drive", "foundation.e.drive.widgets.EDriveWidget")
    private val privacyWidget =
        ComponentName("foundation.e.advancedprivacy", "foundation.e.advancedprivacy.Widget")
    private val weatherWidget =
        ComponentName(
            "foundation.e.blissweather",
            "foundation.e.blissweather.widget.WeatherAppWidgetProvider"
        )

    private val widgets = listOf(ecloudWidget, privacyWidget, weatherWidget)

    @JvmStatic
    fun getWidgetsList(context: Context): List<ComponentName> {
        val providerList: MutableList<ComponentName> = mutableListOf()

        // Get widget details from old database
        val widgetItemsList: MutableList<BlissDbUtils.WidgetItems> =
            BlissDbUtils.getWidgetDetails(context)

        for (widgetItem in widgetItemsList) {
            val provider = widgetItem.componentName
            provider.let { providerList.add(it) }
        }

        // Return default widgets if the providerList is empty
        return if (providerList.isEmpty()) {
            widgets
        } else {
            providerList
        }
    }
}
