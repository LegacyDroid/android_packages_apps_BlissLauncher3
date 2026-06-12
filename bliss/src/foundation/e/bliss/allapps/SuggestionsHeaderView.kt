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
 * File:    bliss/src/foundation/e/bliss/allapps/SuggestionsHeaderView.kt
 * Module:  bliss root app source-set
 * Role:    Horizontal row of app icons displayed as persistent header at drawer top.
 */
// Migration02 / Phase 7.3 — Always-visible suggestions header (top-of-drawer most-used apps).
package foundation.e.bliss.allapps

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.android.launcher3.BubbleTextView
import com.android.launcher3.R
import com.android.launcher3.model.data.AppInfo

/**
 * Horizontal row of [BubbleTextView] icons used as a persistent header at the top of the drawer.
 * The host adapter ([com.android.launcher3.allapps.BaseAllAppsAdapter]) populates this view via
 * [bindSuggestions] every time the row is bound, so child views are recreated to match the latest
 * suggestion set.
 */
class SuggestionsHeaderView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    init {
        orientation = HORIZONTAL
    }

    /**
     * Inflate one [BubbleTextView] per suggestion. Uses the same `R.layout.all_apps_icon` the
     * normal drawer cells use so styling stays consistent. Click / long-click listeners are
     * forwarded to the standard adapter handlers passed in by the caller.
     */
    fun bindSuggestions(
        apps: List<AppInfo>,
        onClick: View.OnClickListener?,
        onLongClick: View.OnLongClickListener?,
    ) {
        removeAllViews()
        if (apps.isEmpty()) return
        val inflater = LayoutInflater.from(context)
        apps.forEach { app ->
            val icon = inflater.inflate(R.layout.all_apps_icon, this, false) as BubbleTextView
            icon.applyFromApplicationInfo(app)
            // Spread evenly across the row.
            val lp = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            icon.layoutParams = lp
            icon.setOnClickListener(onClick)
            icon.setOnLongClickListener(onLongClick)
            addView(icon)
        }
    }
}
