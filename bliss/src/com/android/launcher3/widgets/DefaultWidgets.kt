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
package com.android.launcher3.widgets

import android.content.ComponentName

object DefaultWidgets {
    private val ecloudWidget =
        ComponentName("foundation.e.drive", "foundation.e.drive.widgets.EDriveWidget")
    private val privacyWidget =
        ComponentName("foundation.e.advancedprivacy", "foundation.e.advancedprivacy.Widget")
    val oldWeatherWidget =
        ComponentName(
            "com.android.launcher3launcher",
            "com.android.launcher3launcher.features.weather.WeatherAppWidgetProvider"
        )
    val weatherWidget =
        ComponentName(
            "com.android.launcher3weather",
            "com.android.launcher3weather.widget.WeatherAppWidgetProvider"
        )

    val defaultWidgets = listOf(ecloudWidget, privacyWidget, weatherWidget)
}
