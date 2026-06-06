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
 */
/*
 * File:    bliss/src/foundation/e/bliss/widgets/WidgetContainer.kt
 * Module:  bliss root app source-set
 *
 * Module boundary:
 *   Widget runtime stays app-owned because this container coordinates
 *   Launcher, LauncherPrefs, WidgetCells, WidgetsFullSheet, app-widget host
 *   binding, and database-backed widget state. Data-only classes can move
 *   later, but UI/host flows need instrumentation coverage first because
 *   failures can orphan widget IDs or break restoration.
 *
 * Audit03:
 *   - Phase 05 extracts WidgetInfo, DefaultWidgets, WidgetsDbHelper into
 *     :bliss-widgets-data behind a WidgetRepository. This file will consume
 *     the repository instead of calling WidgetsDbHelper directly.
 */
package foundation.e.bliss.widgets

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Insets
import android.graphics.Rect
import android.os.Bundle
import android.os.UserHandle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.PendingAddItemInfo
import com.android.launcher3.R
import com.android.launcher3.config.FeatureFlags
import com.android.launcher3.graphics.FragmentWithPreview
import com.android.launcher3.widget.LauncherAppWidgetProviderInfo
import com.android.launcher3.widget.PendingAddShortcutInfo
import com.android.launcher3.widget.WidgetCell
import com.android.launcher3.widget.picker.WidgetsFullSheet
import com.android.launcher3.widget.util.WidgetSizes
import foundation.e.bliss.LauncherAppMonitor
import foundation.e.bliss.LauncherAppMonitorCallback
import foundation.e.bliss.utils.BlissDbUtils
import foundation.e.bliss.utils.Logger
import foundation.e.bliss.utils.ObservableList
import foundation.e.bliss.utils.OnDataChangedListener
import foundation.e.bliss.utils.disableComponent
import foundation.e.bliss.widgets.BlissAppWidgetHost.Companion.REQUEST_CONFIGURE_APPWIDGET
import foundation.e.bliss.widgets.data.DefaultWidgets
import foundation.e.bliss.widgets.data.WidgetInfo
import foundation.e.bliss.widgets.data.WidgetRepository
import io.reactivex.disposables.Disposable
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@Suppress("Deprecation", "NewApi")
class WidgetContainer(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs), OnDataChangedListener {
    private val mLauncher by lazy { Launcher.getLauncher(context) }

    private lateinit var mManageWidgetLayout: LinearLayout
    private lateinit var mRemoveWidgetLayout: FrameLayout
    private lateinit var mResizeContainer: RelativeLayout
    private lateinit var mWidgetLinearLayout: LinearLayout
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mWidgetAdapter: StaggeredAdapter
    private var lastOrientation: Int = Configuration.ORIENTATION_UNDEFINED

    private val mResizeContainerRect = Rect()
    private val mInsetPadding =
        context.resources.getDimension(R.dimen.widget_page_inset_padding).toInt()

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(0, 0, 0, 0)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (mResizeContainer.visibility == VISIBLE && ev.action == MotionEvent.ACTION_DOWN) {
            mResizeContainer.getHitRect(mResizeContainerRect)
            if (!mResizeContainerRect.contains(ev.x.toInt(), ev.y.toInt())) {
                mLauncher.hideWidgetResizeContainer()
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        mManageWidgetLayout = findViewById(R.id.manage_widget_parent)
        mRemoveWidgetLayout = findViewById(R.id.remove_widget_parent)
        mResizeContainer = findViewById(R.id.widget_resizer_container)
        mWidgetLinearLayout = findViewById(R.id.widget_linear_layout)

        findViewById<Button>(R.id.manage_widgets).setOnClickListener {
            WidgetsFullSheet.show(mLauncher, true, true)
        }

        findViewById<Button>(R.id.remove_widgets).setOnClickListener {
            val intent =
                Intent(context, WidgetsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        lastOrientation = resources.configuration.orientation
        mRecyclerView = findViewWithTag("wrapper_children")
        mWidgetAdapter = mRecyclerView.adapter as StaggeredAdapter
        mWidgetAdapter.addOnDataChangedListener(this)
        handleRemoveButtonVisibility(mWidgetAdapter.getWidgets().size)
        updatePadding()
        updateRecyclerViewHeight()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        // Reload layout if orientation has changed
        newConfig?.let {
            if (it.orientation != lastOrientation) {
                lastOrientation = it.orientation
                reloadStaggeredLayout()
            }
        }
    }

    fun reloadStaggeredLayout() {
        updatePadding()
        val spanCount = getSpanCount(mLauncher)
        mRecyclerView.layoutManager =
            NoScrollStaggeredLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
    }

    private fun updateRecyclerViewHeight() {
        mRecyclerView.post {
            val widgetFragment = mLauncher.fragmentManager.findFragmentByTag("qsb_view")
            if (widgetFragment != null) {
                (widgetFragment as WidgetFragment).setRecyclerViewHeight()
            }
        }
    }

    private fun updatePadding() {
        val insets = mLauncher.workspace.mInsets
        if (::mResizeContainer.isInitialized) {
            mResizeContainer.apply {
                val layoutParams = this.layoutParams as LayoutParams
                layoutParams.bottomMargin = mInsetPadding + (insets.bottom)
                this.layoutParams = layoutParams
            }
        }

        if (::mWidgetLinearLayout.isInitialized) {

            val windowInsets = getSystemInsets(context)
            mWidgetLinearLayout.apply {
                setPadding(
                    this.paddingLeft,
                    mInsetPadding + max(windowInsets.top, insets.top),
                    this.paddingRight,
                    max(windowInsets.bottom, insets.bottom),
                )
            }
        }

        if (::mManageWidgetLayout.isInitialized) {
            mManageWidgetLayout.apply {
                setPadding(
                    this.paddingLeft,
                    mInsetPadding,
                    this.paddingRight,
                    (insets.top - mInsetPadding),
                )
            }
        }
    }

    private fun handleRemoveButtonVisibility(childCount: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            if (childCount == 0) {
                mRemoveWidgetLayout.visibility = View.GONE
            } else if (mRemoveWidgetLayout.visibility == View.GONE) {
                mRemoveWidgetLayout.visibility = View.VISIBLE
            }
        }
    }

    fun updateWidgets() {
        if (::mRecyclerView.isInitialized) {
            val widgetRepo = WidgetRepository.get(context)
            val widgetManager = AppWidgetManager.getInstance(context)

            mWidgetAdapter.getWidgets().forEach {
                val height = widgetRepo.getWidgetHeight(it.id) ?: 0

                val info = (it as AppWidgetHostView).appWidgetInfo
                val opts =
                    WidgetSizes.getWidgetSizeOptions(
                        mLauncher,
                        info.provider,
                        mLauncher.deviceProfile.inv.numColumns,
                        mLauncher.deviceProfile.inv.numRows,
                    )

                if (height > 0) {
                    opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
                    opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
                }
                val blacklistedComponents =
                    mLauncher.resources.getStringArray(R.array.blacklisted_widget_options)
                if (!blacklistedComponents.contains(info.provider.className)) {
                    widgetManager.updateAppWidgetOptions(it.id, opts)
                }
            }
            mWidgetAdapter.notifyListeners()
        }
    }

    class StaggeredAdapter : RecyclerView.Adapter<StaggeredAdapter.WidgetViewHolder>() {

        private var widgets = mutableListOf<View>()
        private val listeners = mutableListOf<OnDataChangedListener>()

        fun addOnDataChangedListener(listener: OnDataChangedListener) {
            listeners.add(listener)
        }

        fun removeOnDataChangedListener(listener: OnDataChangedListener) {
            listeners.remove(listener)
        }

        @SuppressLint("NotifyDataSetChanged")
        fun notifyWidgetsChanged() {
            notifyDataSetChanged()
            notifyListeners()
        }

        fun notifyListeners() {
            listeners.forEach { it.onDataChanged() }
        }

        fun setWidgets(widgets: MutableList<View>) {
            this.widgets.clear()
            this.widgets = widgets
            notifyWidgetsChanged()
        }

        fun addWidget(widget: View) {
            widgets.add(widget)
            notifyWidgetsChanged()
        }

        fun getWidgets(): MutableList<View> {
            return widgets
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetViewHolder {
            val view =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.widget_item_layout, parent, false)
            return WidgetViewHolder(view)
        }

        override fun onBindViewHolder(holder: WidgetViewHolder, position: Int) {
            val widget = widgets[position]
            holder.bind(widget)
        }

        override fun getItemCount(): Int {
            return widgets.size
        }

        class WidgetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(view: View) {
                (itemView as FrameLayout).removeAllViews()
                if (view.parent != null) {
                    (view.parent as ViewGroup).removeAllViews()
                }
                (itemView as FrameLayout).addView(view)
            }
        }
    }

    /** A fragment to display the default widgets. */
    class WidgetFragment : FragmentWithPreview() {
        private lateinit var recyclerView: RecyclerView
        private lateinit var widgetObserver: Disposable
        private lateinit var widgetRepo: WidgetRepository
        private lateinit var widgetsAdapter: StaggeredAdapter

        private val mOldWidgets by lazy { BlissDbUtils.getWidgetDetails(context) }
        private val mWidgetManager by lazy { AppWidgetManager.getInstance(context) }
        private val mWidgetHost by lazy { BlissAppWidgetHost(context) }
        private val launcher by lazy { Launcher.getLauncher(context) }

        private val mAppMonitorCallback: LauncherAppMonitorCallback =
            object : LauncherAppMonitorCallback {
                override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                    if (!::widgetRepo.isInitialized) {
                        return
                    }
                    val widgets =
                        widgetRepo.getWidgets().filter { it.component.packageName == packageName }
                    if (packageName != null && widgets.isNotEmpty()) {
                        widgets.map { it.widgetId }.forEach { widgetRepo.delete(it) }
                        rebindWidgets()
                    }
                }

                override fun onPackagesAvailable(
                    packageNames: Array<String?>?,
                    user: UserHandle?,
                    replacing: Boolean,
                ) {
                    if (!shouldAttemptWidgetIdRepair(context)) return
                    if (::widgetRepo.isInitialized && ::widgetsAdapter.isInitialized) {
                        rebindWidgets()
                    }
                }
            }

        private var initialWidgetsAdded: Boolean
            set(value) {
                LauncherPrefs.getPrefs(context)
                    .edit()
                    .putBoolean(defaultWidgetsAdded, value)
                    .apply()
            }
            get() {
                return LauncherPrefs.getPrefs(context).getBoolean(defaultWidgetsAdded, false)
            }

        private val isQsbEnabled: Boolean
            get() = FeatureFlags.QSB_ON_FIRST_SCREEN.get()

        init {
            LauncherAppMonitor.getInstanceNoCreate().registerCallback(mAppMonitorCallback)
        }

        @Deprecated("Deprecated in Java")
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View {
            widgetRepo = WidgetRepository.get(context)
            widgetsAdapter = StaggeredAdapter()
            val spanCount = getSpanCount(launcher)
            recyclerView =
                RecyclerView(context, null).apply {
                    tag = "wrapper_children"
                    layoutManager =
                        NoScrollStaggeredLayoutManager(
                            spanCount,
                            StaggeredGridLayoutManager.VERTICAL,
                        )
                    adapter = widgetsAdapter
                    isNestedScrollingEnabled = false
                }
            if (isQsbEnabled) {
                loadWidgets()
            }

            return recyclerView
        }

        override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
            if (isQsbEnabled) {
                mWidgetHost.startListening()
            }

            super.onViewCreated(view, savedInstanceState)
        }

        override fun onDestroyView() {
            mWidgetHost.stopListening()
            super.onDestroyView()
        }

        private fun loadWidgets() {
            if (!initialWidgetsAdded) {
                val oldWidgets = mWidgetHost.appWidgetIds
                if (oldWidgets.isEmpty()) {
                    mWidgetHost.deleteHost()
                    DefaultWidgets.defaultWidgets.forEach {
                        try {
                            bindWidget(it)
                        } catch (e: Exception) {
                            Logger.e(TAG, "Could not add widget ${it.flattenToString()}")
                        }
                    }
                } else {
                    rebindWidgets(true)
                }

                disableComponent(context, DefaultWidgets.oldWeatherWidget(context))
                initialWidgetsAdded = true
            } else {
                rebindWidgets()
                Logger.e(TAG, "saved widgets added")
            }

            widgetObserver =
                defaultWidgets.observable.subscribe {
                    Logger.d(TAG, "Component: ${it.flattenToString()}")
                    bindWidget(it)
                }

            CoroutineScope(Dispatchers.Main).launch { eventFlow.collect { rebindWidgets() } }
        }

        fun rebindWidgets(backup: Boolean = false) {
            widgetsAdapter.setWidgets(mutableListOf())
            if (!backup) {
                val dbWidgets = widgetRepo.getWidgets().sortedBy { it.position }
                val keepWidgetIds = dbWidgets.mapTo(hashSetOf()) { it.widgetId }
                if (shouldAttemptWidgetIdRepair(context)) {
                    dbWidgets.forEach { restoreWidgetFromDb(it, keepWidgetIds) }
                    if (keepWidgetIds.all { mWidgetManager.getAppWidgetInfo(it) != null }) {
                        LauncherPrefs.get(context)
                            .put(LauncherPrefs.NEEDS_WIDGET_REBIND_AFTER_RESTORE, false)
                    }
                } else {
                    dbWidgets.forEach { widgetInfo -> addView(widgetInfo.widgetId) }
                }

                // Remove all widgets not present in db
                mWidgetHost.appWidgetIds
                    .filter { id -> !keepWidgetIds.contains(id) }
                    .forEach { mWidgetHost.deleteAppWidgetId(it) }
            } else {
                if (mOldWidgets.isNotEmpty()) {
                    mOldWidgets
                        .filter { mWidgetHost.appWidgetIds.contains(it.id) }
                        .sortedWith(
                            compareBy(BlissDbUtils.WidgetItems::order, BlissDbUtils.WidgetItems::id)
                        )
                        .forEach { addView(it.id, true) }
                } else {
                    mWidgetHost.appWidgetIds.sorted().forEach { addView(it, true) }
                }
            }
        }

        private fun bindWidget(provider: ComponentName) {
            val widgetId = mWidgetHost.allocateAppWidgetId()
            val isWidgetBound = mWidgetManager.bindAppWidgetIdIfAllowed(widgetId, provider)

            if (!isWidgetBound) {
                mWidgetHost.deleteAppWidgetId(widgetId)
                Logger.e(TAG, "Could not add widget ${provider.flattenToString()}")
                return
            }

            configureWidget(widgetId)
        }

        private fun configureWidget(widgetId: Int) {
            val info = mWidgetManager.getAppWidgetInfo(widgetId)
            if (info != null) {
                if (info.configure != null) {
                    sendIntent(widgetId)
                } else {
                    addView(widgetId)
                }
            } else {
                mWidgetHost.deleteAppWidgetId(widgetId)
            }
        }

        private fun restoreWidgetFromDb(widgetInfo: WidgetInfo, keepWidgetIds: MutableSet<Int>) {
            if (mWidgetManager.getAppWidgetInfo(widgetInfo.widgetId) != null) {
                addView(widgetInfo.widgetId)
                return
            }

            // Seedvault/app-data restore can bring back our widget database, but appWidgetIds are
            // allocated and persisted by the system; they often don't survive restores.
            val newWidgetId = mWidgetHost.allocateAppWidgetId()
            val isWidgetBound =
                mWidgetManager.bindAppWidgetIdIfAllowed(newWidgetId, widgetInfo.component)
            if (!isWidgetBound) {
                mWidgetHost.deleteAppWidgetId(newWidgetId)
                Logger.e(
                    TAG,
                    "Could not rebind restored widget ${widgetInfo.component.flattenToString()} (oldId=${widgetInfo.widgetId})",
                )
                return
            }

            if (newWidgetId != widgetInfo.widgetId) {
                widgetRepo.updateWidgetId(widgetInfo.widgetId, newWidgetId)
                keepWidgetIds.remove(widgetInfo.widgetId)
                keepWidgetIds.add(newWidgetId)
                mWidgetHost.deleteAppWidgetId(widgetInfo.widgetId)
            }
            addView(newWidgetId)
        }

        private fun shouldAttemptWidgetIdRepair(context: Context): Boolean {
            return LauncherPrefs.get(context).get(LauncherPrefs.NEEDS_WIDGET_REBIND_AFTER_RESTORE)
        }

        private fun addView(widgetId: Int, backup: Boolean = false) {
            val info = mWidgetManager.getAppWidgetInfo(widgetId)
            if (info == null) {
                mWidgetHost.deleteAppWidgetId(widgetId)
                return
            }
            val widgetInfo = LauncherAppWidgetProviderInfo.fromProviderInfo(launcher, info)
            val view = mWidgetHost.createView(widgetId, widgetInfo)
            configureWidgetView(view, widgetId, widgetInfo)

            if (backup && isOldWeatherWidget(view)) {
                mWidgetHost.deleteAppWidgetId(view.id)
                // Swap with new widget
                bindWidget(DefaultWidgets.weatherWidget)
                return
            }

            val opts = mWidgetManager.getAppWidgetOptions(view.appWidgetId)
            val params = LayoutParams(-1, opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT))
            params.height =
                if (backup) {
                    resolveBackupHeight(widgetId, widgetInfo, opts).also {
                        view.updateAppWidgetOptions(opts)
                    }
                } else {
                    widgetRepo.getWidgetHeight(view.id) ?: 0
                }

            if (params.height > 0) {
                view.layoutParams = params
            }
            widgetsAdapter.addWidget(view)

            updateWidgetOptionsForView(view, info, params.height)
            widgetRepo.insert(
                WidgetInfo(
                    widgetsAdapter.getWidgets().indexOf(view),
                    view.appWidgetInfo.provider,
                    view.appWidgetId,
                    params.height,
                )
            )
        }

        private fun configureWidgetView(
            view: AppWidgetHostView,
            widgetId: Int,
            widgetInfo: LauncherAppWidgetProviderInfo,
        ) {
            view.id = widgetId
            view.layoutTransition = LayoutTransition()
            view.setOnLongClickListener {
                if (
                    (widgetInfo.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL) ==
                        AppWidgetProviderInfo.RESIZE_VERTICAL
                ) {
                    launcher.hideWidgetResizeContainer()
                    launcher.showWidgetResizeContainer(view as RoundedWidgetView)
                }
                true
            }
        }

        private fun isOldWeatherWidget(view: AppWidgetHostView): Boolean {
            return view.appWidgetInfo.provider.equals(DefaultWidgets.oldWeatherWidget(context))
        }

        private fun resolveBackupHeight(
            widgetId: Int,
            widgetInfo: LauncherAppWidgetProviderInfo,
            opts: Bundle,
        ): Int {
            val oldHeight =
                if (mOldWidgets.isNotEmpty()) {
                    mOldWidgets.find { widgetItems -> widgetItems.id == widgetId }?.height
                } else {
                    0
                }
            val minHeight: Int = widgetInfo.minResizeHeight
            val maxHeight: Int =
                InvariantDeviceProfile.INSTANCE.get(context).getDeviceProfile(context).heightPx *
                    3 / 4
            val normalisedDifference = (maxHeight - minHeight) / 100
            val resolved =
                if (oldHeight != null && oldHeight > 0) {
                    minHeight + normalisedDifference * oldHeight
                } else {
                    0
                }
            opts.remove(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            opts.remove(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            opts.remove(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            opts.remove(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            return resolved
        }

        private fun updateWidgetOptionsForView(
            view: AppWidgetHostView,
            info: AppWidgetProviderInfo,
            height: Int,
        ) {
            val opts =
                WidgetSizes.getWidgetSizeOptions(
                    launcher,
                    info.provider,
                    launcher.deviceProfile.inv.numColumns,
                    launcher.deviceProfile.inv.numRows,
                )
            if (height > 0) {
                opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
                opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
            }
            val blacklistedComponents =
                launcher.resources.getStringArray(R.array.blacklisted_widget_options)
            if (!blacklistedComponents.contains(info.provider.className)) {
                mWidgetManager.updateAppWidgetOptions(view.appWidgetId, opts)
            }
        }

        private fun sendIntent(widgetId: Int) {
            val bundle = Bundle().apply { putInt(EXTRA_APPWIDGET_ID, widgetId) }
            mWidgetHost.startAppWidgetConfigureActivityForResult(
                launcher,
                widgetId,
                0,
                REQUEST_CONFIGURE_APPWIDGET,
                bundle,
            )
        }

        @Deprecated("Deprecated in Java")
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            val widgetId = data?.getIntExtra(EXTRA_APPWIDGET_ID, -1) ?: -1
            Logger.d(TAG, "Request: $requestCode | Result: $resultCode | Widget: $widgetId")
            if (resultCode == RESULT_OK && requestCode == REQUEST_CONFIGURE_APPWIDGET) {
                addView(widgetId)
            } else {
                rebindWidgets()
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onDestroy() {
            super.onDestroy()
            widgetObserver.dispose()
        }

        fun setRecyclerViewHeight() {
            val layoutManager = recyclerView.layoutManager as StaggeredGridLayoutManager
            val adapter = recyclerView.adapter as StaggeredAdapter

            adapter.let {
                recyclerView.viewTreeObserver.addOnGlobalLayoutListener {
                    // Our maximum column span is 2.
                    // Column span may change on rotation for phones
                    // Hardcode 2 to workaround
                    val totalHeight = IntArray(2) { 0 }

                    for (i in 0 until adapter.itemCount) {
                        if (layoutManager.spanCount == 1) {
                            totalHeight[0] +=
                                adapter
                                    .getWidgets()[i]
                                    .measuredHeight
                                    .coerceAtLeast(launcher.deviceProfile.iconSizePx)
                            continue
                        }
                        val viewHolder = recyclerView.findViewHolderForAdapterPosition(i)
                        viewHolder?.itemView?.let { itemView ->
                            val layoutParams =
                                itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams
                            val spanIndex = layoutParams.spanIndex
                            totalHeight[spanIndex] += itemView.measuredHeight
                        }
                    }

                    val maxHeight = totalHeight.maxOrNull() ?: 0
                    val padding = recyclerView.paddingTop + recyclerView.paddingBottom
                    val layoutParams = recyclerView.layoutParams
                    layoutParams.height = maxHeight + padding
                    recyclerView.layoutParams = layoutParams
                }
            }
        }

        companion object {
            const val TAG = "WidgetFragment"
            const val defaultWidgetsAdded = "default_widgets_added"
            val defaultWidgets = ObservableList<ComponentName>()
            val eventFlow = MutableSharedFlow<Unit>()

            @JvmStatic
            fun onWidgetClick(context: Context, view: View, closeSheet: (Boolean) -> Unit) {

                val cell =
                    when (view) {
                        is WidgetCell -> view
                        else -> {
                            var p = view.parent
                            while (p is View) {
                                if (p is WidgetCell) {
                                    break
                                }
                                p = p.parent
                            }
                            p as? WidgetCell
                        }
                    } ?: return

                val tag = cell.tag
                if (tag is PendingAddShortcutInfo) {
                    Toast.makeText(
                            context,
                            context.getString(R.string.select_a_widget),
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                    return
                }

                closeSheet(true)

                val widget = tag as PendingAddItemInfo
                defaultWidgets.add(widget.componentName)
            }

            @JvmStatic
            fun onWidgetAdded(componentName: ComponentName) {
                defaultWidgets.add(componentName)
            }
        }
    }

    override fun onDataChanged() {
        if (::mWidgetAdapter.isInitialized) {
            handleRemoveButtonVisibility(mWidgetAdapter.getWidgets().size)
        }
    }

    companion object {
        @SuppressLint("NewApi")
        fun getSystemInsets(context: Context): Insets {
            val windowInsets =
                context
                    .getSystemService(WindowManager::class.java)
                    ?.currentWindowMetrics
                    ?.windowInsets
                    ?.getInsets(WindowInsets.Type.systemBars())
            return windowInsets ?: Insets.NONE
        }

        fun getSpanCount(launcher: Launcher?): Int {
            val profile = launcher?.deviceProfile ?: return 1
            return when {
                profile.isTablet -> 2
                profile.isPhone && profile.isLandscape -> 2
                else -> 1
            }
        }
    }
}
