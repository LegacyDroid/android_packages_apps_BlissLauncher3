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
 * Bliss touchpoint(s) (Audit02):
 *   - Reads DrawerFolderWithItems from :bliss-folders-data through
 *     DrawerFolderService.
 *   - Remains in the app source-set because the settings UI uses launcher
 *     resources and ComponentKey app-selection behavior.
 */
/*
 * File:    bliss/src/foundation/e/bliss/folders/ui/DrawerFolderManageActivity.kt
 * Module:  bliss root app source-set
 * Role:    Activity UI for managing drawer folders with list, create, edit, and delete operations.
 */
package foundation.e.bliss.folders.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R
import com.android.launcher3.util.ComponentKey
import foundation.e.bliss.folders.DrawerFolderService
import foundation.e.bliss.folders.db.DrawerFolderWithItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View-based settings UI for managing drawer folders. Mirrors Lawnchair's Compose
 * `AppDrawerFoldersPreference` (XC-1: we explicitly stay on Views).
 *
 * Current scope:
 * - List existing folders (live via [DrawerFolderService.observeFolders])
 * - Create / edit (rename + change member apps) via a dialog
 * - Delete with cascade
 *
 * Reorder via drag-handle is left as a follow-up (`updateFolderInfo` already supports `rank`); for
 * now folders sort by insertion order.
 */
class DrawerFolderManageActivity : Activity() {

    private lateinit var service: DrawerFolderService
    private lateinit var adapter: FolderListAdapter
    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + supervisor)
    private var observeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer_folder_manage)
        title = getString(R.string.drawer_folders_manage_title)

        service = DrawerFolderService.get(applicationContext)

        val rv = findViewById<RecyclerView>(R.id.folder_list)
        rv.layoutManager = LinearLayoutManager(this)
        adapter =
            FolderListAdapter(onEdit = { showEditDialog(it) }, onDelete = { confirmDelete(it) })
        rv.adapter = adapter

        findViewById<Button>(R.id.add_folder).setOnClickListener { showEditDialog(null) }
    }

    override fun onStart() {
        super.onStart()
        observeJob =
            scope.launch {
                service.observeFolders().collectLatest { latest ->
                    adapter.submit(latest)
                    findViewById<View>(R.id.empty_state).visibility =
                        if (latest.isEmpty()) View.VISIBLE else View.GONE
                }
            }
    }

    override fun onStop() {
        super.onStop()
        observeJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        supervisor.cancel()
    }

    private fun confirmDelete(folder: DrawerFolderWithItems) {
        AlertDialog.Builder(this)
            .setTitle(folder.folder.title)
            .setMessage(R.string.drawer_folders_delete)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                scope.launch { service.delete(folder.folder.id) }
            }
            .setNegativeButton(R.string.drawer_folders_cancel, null)
            .show()
    }

    private fun showEditDialog(existing: DrawerFolderWithItems?) {
        val view =
            LayoutInflater.from(this).inflate(R.layout.dialog_edit_drawer_folder, null, false)
        val nameEdit = view.findViewById<EditText>(R.id.folder_name)
        val rv = view.findViewById<RecyclerView>(R.id.app_picker)
        rv.layoutManager = LinearLayoutManager(this)

        nameEdit.setText(existing?.folder?.title.orEmpty())

        // Build the list off the main thread (LauncherApps.getActivityList is heavy).
        val pickerAdapter = AppPickerAdapter()
        rv.adapter = pickerAdapter

        val initialKeys = existing?.items?.map { it.componentKey }?.toSet().orEmpty()

        scope.launch {
            val apps = withContext(Dispatchers.IO) { loadInstalledApps() }
            pickerAdapter.submit(apps, initialKeys)
        }

        AlertDialog.Builder(this)
            .setTitle(
                if (existing == null) R.string.drawer_folders_add else R.string.drawer_folders_edit
            )
            .setView(view)
            .setPositiveButton(R.string.drawer_folders_save) { _, _ ->
                val title = nameEdit.text.toString().trim()
                val checked = pickerAdapter.checkedKeys()
                scope.launch {
                    service.upsert(
                        folderId = existing?.folder?.id ?: 0,
                        title = title.ifEmpty { getString(R.string.drawer_folders_add) },
                        rank = existing?.folder?.rank ?: nextRank(),
                        contents = checked,
                    )
                }
            }
            .setNegativeButton(R.string.drawer_folders_cancel, null)
            .show()
    }

    private suspend fun nextRank(): Int =
        withContext(Dispatchers.IO) {
            (service.getAllFolders().maxOfOrNull { it.folder.rank } ?: -1) + 1
        }

    private fun loadInstalledApps(): List<PickableApp> {
        val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val user: UserHandle = Process.myUserHandle()
        val activities = launcherApps.getActivityList(null, user)
        return activities
            .map {
                PickableApp(
                    key = ComponentKey(it.componentName, user),
                    label = it.label?.toString().orEmpty(),
                    icon = it.getBadgedIcon(0),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private data class PickableApp(val key: ComponentKey, val label: String, val icon: Drawable)

    // ---- folder list adapter ----

    private class FolderListAdapter(
        private val onEdit: (DrawerFolderWithItems) -> Unit,
        private val onDelete: (DrawerFolderWithItems) -> Unit,
    ) : RecyclerView.Adapter<FolderRowVH>() {
        private val items = mutableListOf<DrawerFolderWithItems>()

        fun submit(latest: List<DrawerFolderWithItems>) {
            items.clear()
            items.addAll(latest)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderRowVH {
            val v =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_drawer_folder_row, parent, false)
            return FolderRowVH(v)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: FolderRowVH, position: Int) {
            val row = items[position]
            holder.title.text = row.folder.title
            holder.count.text =
                holder.itemView.context.getString(R.string.drawer_folders_app_count, row.items.size)
            holder.itemView.setOnClickListener { onEdit(row) }
            holder.delete.setOnClickListener { onDelete(row) }
        }
    }

    private class FolderRowVH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.folder_title)
        val count: TextView = v.findViewById(R.id.folder_count)
        val delete: Button = v.findViewById(R.id.folder_delete)
    }

    // ---- app picker adapter ----

    private class AppPickerAdapter : RecyclerView.Adapter<AppPickVH>() {
        private val apps = mutableListOf<PickableApp>()
        private val checked = mutableSetOf<String>()

        fun submit(latest: List<PickableApp>, initial: Set<String>) {
            apps.clear()
            apps.addAll(latest)
            checked.clear()
            checked.addAll(initial)
            notifyDataSetChanged()
        }

        fun checkedKeys(): List<ComponentKey> =
            apps.filter { checked.contains(it.key.toString()) }.map { it.key }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppPickVH {
            val v =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_drawer_folder_app_pick, parent, false)
            return AppPickVH(v)
        }

        override fun getItemCount(): Int = apps.size

        override fun onBindViewHolder(holder: AppPickVH, position: Int) {
            val app = apps[position]
            holder.label.text = app.label
            holder.icon.setImageDrawable(app.icon)
            holder.check.setOnCheckedChangeListener(null)
            holder.check.isChecked = checked.contains(app.key.toString())
            holder.check.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) checked.add(app.key.toString())
                else checked.remove(app.key.toString())
            }
            holder.itemView.setOnClickListener { holder.check.toggle() }
        }
    }

    private class AppPickVH(v: View) : RecyclerView.ViewHolder(v) {
        val check: CheckBox = v.findViewById(R.id.app_check)
        val icon: ImageView = v.findViewById(R.id.app_icon)
        val label: TextView = v.findViewById(R.id.app_label)
    }
}
