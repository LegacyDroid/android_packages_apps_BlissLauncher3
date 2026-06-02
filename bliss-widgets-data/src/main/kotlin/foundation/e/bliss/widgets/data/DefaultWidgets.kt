/*
 * Copyright (C) 2025 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss-widgets-data/src/main/kotlin/foundation/e/bliss/widgets/data/DefaultWidgets.kt
 * Module:  :bliss-widgets-data
 * Role:    Configuration object listing default widgets.
 */
package foundation.e.bliss.widgets.data

import android.content.ComponentName
import android.content.Context

object DefaultWidgets {
    private val ecloudWidget =
        ComponentName("foundation.e.drive", "foundation.e.drive.widgets.EDriveWidget")
    private val privacyWidget =
        ComponentName("foundation.e.advancedprivacy", "foundation.e.advancedprivacy.Widget")
    private const val OLD_WEATHER_WIDGET_CLASS =
        "foundation.e.blisslauncher.features.weather.WeatherAppWidgetProvider"

    fun oldWeatherWidget(context: Context): ComponentName =
        ComponentName(context.packageName, OLD_WEATHER_WIDGET_CLASS)

    val weatherWidget: ComponentName =
        ComponentName(
            "foundation.e.blissweather",
            "foundation.e.blissweather.widget.WeatherAppWidgetProvider",
        )

    val defaultWidgets: List<ComponentName> = listOf(ecloudWidget, privacyWidget, weatherWidget)
}
