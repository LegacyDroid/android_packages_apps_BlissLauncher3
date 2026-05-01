/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/shadows/ShadowLauncherComponentProvider.kt
 * Module:  bliss test source-set  (foundation.e.bliss.shadows)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/shadows/):
 *   ├── ShadowExecutors.kt                  — replaces MAIN_EXECUTOR / MODEL_EXECUTOR
 *   │                                         with HandlerThread-backed executors
 *   ├── ShadowLauncherComponentProvider.kt  — fakes LauncherComponentProvider.get  ← THIS FILE
 *   └── ShadowLauncherWidgetHolder.kt       — no-op widget holder for tests
 *
 * Purpose:
 *   Migration05 follow-up F4 — activate LawnchairImporterGoldenTest. The
 *   real LauncherComponentProvider.get(ctx) returns a Dagger-built
 *   LauncherAppComponent. Building that component under Robolectric pulls
 *   in the entire Bliss + AOSP graph (IconCache, LauncherModel, IDP, …)
 *   which is impractical for a unit test. This shadow short-circuits the
 *   .get(ctx) call to a hand-built fake whose getLauncherPrefs() returns a
 *   directly-constructed LauncherPrefs. Production code path is untouched
 *   — the shadow is wired only on the test class via @Config(shadows=…).
 *
 * Plan reference: Plans/Migration05/04-importer-golden-activation.md §3.2
 */
package foundation.e.bliss.shadows

import android.content.Context
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.dagger.LauncherAppComponent
import com.android.launcher3.dagger.LauncherComponentProvider
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Robolectric shadow for [LauncherComponentProvider]. Replaces the Dagger component lookup with a
 * fake whose only working method is [LauncherAppComponent.getLauncherPrefs], which returns a real
 * [LauncherPrefs] backed by the application's SharedPreferences.
 *
 * Other interface methods on the proxy throw `UnsupportedOperationException` — fail-loud rather
 * than silently return null and corrupt downstream logic. If the importer ever queries another
 * component method we'll discover it immediately rather than as a confusing NPE deeper in.
 */
@Implements(LauncherComponentProvider::class)
class ShadowLauncherComponentProvider {

    companion object {
        // Per-app cache so repeated calls return the same fake (mirrors the
        // production side's LayoutInflater.Filter caching).
        private val CACHE = ConcurrentHashMap<Context, LauncherAppComponent>()

        // Per-app cache of the LauncherPrefs instance. Same context => same
        // prefs => same SharedPreferences-backed state across the import.
        private val PREFS_CACHE = ConcurrentHashMap<Context, LauncherPrefs>()

        @JvmStatic
        @Implementation
        fun get(c: Context): LauncherAppComponent {
            val app = c.applicationContext
            return CACHE.computeIfAbsent(app) { ctx -> buildFake(ctx) }
        }

        private fun buildFake(app: Context): LauncherAppComponent {
            val prefs = PREFS_CACHE.computeIfAbsent(app) { LauncherPrefs(app) }
            val handler = InvocationHandler { _, method: Method, _ ->
                when (method.name) {
                    "getLauncherPrefs" -> prefs
                    "hashCode" -> System.identityHashCode(prefs)
                    "equals" -> false
                    "toString" -> "FakeLauncherAppComponent(testing-only)"
                    else ->
                        throw UnsupportedOperationException(
                            "ShadowLauncherComponentProvider does not implement " +
                                "${method.declaringClass.simpleName}.${method.name} — " +
                                "the importer should not need this method under test."
                        )
                }
            }
            return Proxy.newProxyInstance(
                LauncherAppComponent::class.java.classLoader,
                arrayOf(LauncherAppComponent::class.java),
                handler,
            ) as LauncherAppComponent
        }

        /** Test-side reset hook in case a test wants a fresh prefs/component graph. */
        @JvmStatic
        fun resetForTest() {
            CACHE.clear()
            PREFS_CACHE.clear()
        }
    }
}
