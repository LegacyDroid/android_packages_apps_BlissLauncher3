/*
 * Copyright (C) 2026 MURENA SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package foundation.e.bliss.folders.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.android.launcher3.Launcher
import com.android.launcher3.R
import com.android.launcher3.folder.FolderIcon
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.views.ActivityContext

/**
 * The visual card for a drawer folder. It hosts a real [FolderIcon] (the same view used on the
 * workspace), so that opening the folder reuses Bliss's existing `Folder.animateOpen` path.
 *
 * Migration02 / Phase 1 — see Plans/Migration02/02-phase-1-drawer-folders.md.
 */
class DrawerFolderItemView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    private var folderIcon: FolderIcon? = null

    /**
     * Inflate (lazily on first bind) a [FolderIcon] linked to [info] and add it as our only child.
     * We reuse `inflateFolderAndIcon` so the FolderIcon is paired with a real
     * [com.android.launcher3.folder.Folder] — that gives us free `animateOpen` for the sub-list.
     */
    fun applyFromFolderInfo(info: FolderInfo, activity: ActivityContext) {
        // Replace any previously-bound folder icon so recyclerview recycling cannot leak state
        // across positions.
        folderIcon?.let { existing -> (existing.parent as? ViewGroup)?.removeView(existing) }
        folderIcon = null

        // PLAN-DRIFT-M02: see 13-drift-log.md entry for Phase 1.5 — the upstream
        // FolderIcon.inflateFolderAndIcon requires `T extends Context & ActivityContext`. Bliss's
        // Launcher implements both (Launcher is-a Context, is-a ActivityContext), so we narrow
        // the activity reference to Launcher when reachable, falling back to the simpler
        // FolderIcon.inflateIcon path for non-Launcher contexts (e.g. preview / Taskbar).
        val launcher = activity as? Launcher
        val icon: FolderIcon =
            if (launcher != null) {
                FolderIcon.inflateFolderAndIcon<Launcher>(
                    R.layout.folder_icon,
                    launcher,
                    this,
                    info,
                )
            } else {
                FolderIcon.inflateIcon(R.layout.folder_icon, activity, this, info)
            }
        // Match the cell sizing used for app icons.
        val lp =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        addView(icon, lp)
        folderIcon = icon
        // Migration02 / Phase 7.1 — XC-5: mark this preview as belonging to a drawer folder so
        // the per-folder color override lookup uses the "dr-<id>" key prefix instead of clashing
        // with workspace folder ids.
        icon.markAsDrawerFolder(true)
        // Ensure clicks on the card open the folder (FolderIcon installs its own click listener
        // via inflateIcon → activity.getItemOnClickListener; we keep a defensive forwarder).
        setOnClickListener { icon.performClick() }
        contentDescription = info.title
    }

    /** Used by tests / inspection — currently no-op. */
    @Suppress("unused") fun getFolderIcon(): View? = folderIcon
}
