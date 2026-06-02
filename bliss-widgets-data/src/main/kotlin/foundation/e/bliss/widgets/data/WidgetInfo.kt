/*
 * Copyright (C) 2025 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss-widgets-data/src/main/kotlin/foundation/e/bliss/widgets/data/WidgetInfo.kt
 * Module:  :bliss-widgets-data
 * Role:    Data class holding widget configuration.
 */
package foundation.e.bliss.widgets.data

import android.content.ComponentName
import androidx.annotation.Keep

@Keep
data class WidgetInfo(
    val position: Int,
    val component: ComponentName,
    val widgetId: Int,
    val height: Int,
)
