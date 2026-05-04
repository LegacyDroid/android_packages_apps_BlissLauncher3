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
 * File:    bliss/src/foundation/e/bliss/utils/Logger.kt
 * Module:  bliss source-set  (foundation.e.bliss.utils)
 * Role:    REFACTORED
 *
 * Tree (foundation/e/bliss/utils/):
 *   ├── BlissConstants.kt         — shared constants used across bliss-side files
 *   ├── BlissDbHelper.kt          — SQLite helper for the Bliss launcher.db
 *   ├── BlissDbUtils.kt           — query helpers around BlissDbHelper
 *   ├── BlissUtils.kt             — misc helper functions
 *   ├── Logger.kt                 — centralised logger with debug-gated v/d  ← THIS FILE
 *   ├── ObservableList.kt         — observable list primitive
 *   ├── OnDataChangedListener.kt  — listener interface for data-change signals
 *   └── PrivateSpaceCompat.java   — private-space SDK shim (will move to :bliss-compat)
 *
 * Purpose:
 *   The single sink for bliss-side logging. Provides both the legacy
 *   "Logger.d(TAG, msg)" object API used by ~30 existing call sites, and
 *   the Migration04 §1.2 instance API ("Logger.tag(name).d(msg)" /
 *   "Logger.of<MyClass>().d(msg)"). The legacy API will be retired by
 *   Migration04 phase 02 (importer decomposition), at which point the
 *   tag-based companion methods can be removed.
 *
 *   Migration05 follow-up F1 — i/w/e calls also write to AOSP's
 *   com.android.launcher3.logging.FileLog so the persistent on-device log
 *   ring captures bliss-side warnings/errors alongside loader diagnostics.
 *   v/d stay logcat-only to avoid filling the ring with debug noise. The
 *   FileLog backing is opt-out: when FileLog has not been initialised via
 *   setDir() (e.g. in JVM unit tests, before LauncherApplication.onCreate)
 *   the dispatch becomes a no-op without throwing. Calls are wrapped in a
 *   try/catch as a belt-and-braces guard against the FileLog handler
 *   thread being absent under the Android-less test classpath.
 *
 * Plan reference: Plans/Migration04/08-observability-and-testing.md §1
 *                 Plans/Migration05 follow-up F1 (FileLog integration)
 */
package foundation.e.bliss.utils

import android.util.Log as AndroidLog
import com.android.launcher3.BuildConfig
import com.android.launcher3.logging.FileLog
import timber.log.Timber

/**
 * Centralised logger.
 *
 * Two flavours:
 * - Legacy static-style API (kept for existing call sites): Logger.d(TAG, "msg") Logger.e(TAG,
 *   "msg", throwable)
 * - Migration04 instance API: private val log = Logger.of<MyClass>() private static final Logger
 *   LOG = Logger.tag("MyClassAbbrev"); log.d("msg"); log.e("msg", throwable)
 *
 * Verbose / debug logs are no-ops in non-debug builds (cost: one BuildConfig.IS_DEBUG_DEVICE check;
 * JIT inlines). Warnings/errors always log + always include the Throwable when given. Tag length
 * must stay ≤ 23 chars (Android logcat limit) — caller's responsibility.
 */
class Logger @PublishedApi internal constructor(@PublishedApi internal val tag: String) {

    @JvmOverloads
    fun v(msg: String, t: Throwable? = null) {
        if (isDebug) AndroidLog.v(tag, msg, t)
    }

    @JvmOverloads
    fun d(msg: String, t: Throwable? = null) {
        if (isDebug) AndroidLog.d(tag, msg, t)
    }

    @JvmOverloads
    fun i(msg: String, t: Throwable? = null) {
        AndroidLog.i(tag, msg, t)
        persist(msg, t)
    }

    @JvmOverloads
    fun w(msg: String, t: Throwable? = null) {
        AndroidLog.w(tag, msg, t)
        persist(msg, t)
    }

    @JvmOverloads
    fun e(msg: String, t: Throwable? = null) {
        AndroidLog.e(tag, msg, t)
        persist(msg, t)
    }

    /**
     * Mirrors the i/w/e log line into AOSP's FileLog ring (Migration05 F1).
     * - No-op when FileLog has not yet been initialised via FileLog.setDir(). FileLog.print() drops
     *   the message inside its own handler when its sLogsDirectory is null, so calling it pre-init
     *   is safe in production.
     * - The try/catch covers the JVM unit-test classpath where android.os.Looper / Handler are
     *   stubs and FileLog's HandlerThread bring-up may NPE.
     * - FileLog.print(tag, msg, e) takes Exception, not Throwable. Errors and sub-classes of
     *   Throwable that are NOT Exception (e.g. AssertionError, custom Errors) are recorded
     *   message-only via the (tag, msg) overload — their stack-trace still appears in logcat
     *   through Log.x(tag, msg, t).
     */
    private fun persist(msg: String, t: Throwable?) {
        try {
            when (t) {
                null -> FileLog.print(tag, msg)
                is Exception -> FileLog.print(tag, msg, t)
                else -> FileLog.print(tag, msg)
            }
        } catch (ignored: Throwable) {
            // FileLog not available / no Looper / Handler bring-up failure on
            // the JVM unit-test classpath — fall through; logcat already saw it.
        }
    }

    companion object {
        // Visible for testing — overridable in JVM unit tests where BuildConfig
        // isn't loaded with the production value.
        @JvmStatic internal var isDebug: Boolean = BuildConfig.IS_DEBUG_DEVICE

        @JvmStatic fun tag(name: String): Logger = Logger(name)

        inline fun <reified T> of(): Logger = Logger(T::class.java.simpleName)

        @JvmStatic fun plant() = Timber.plant(Timber.DebugTree())

        // ------------------------------------------------------------------
        // Legacy static-style API. All bliss-side call sites currently use
        // these forms: Logger.d(TAG, msg) etc. They will be migrated to the
        // instance API in Migration04 phase 02; until then both APIs coexist.
        // ------------------------------------------------------------------

        @JvmStatic
        fun v(tag: String, msg: String) {
            if (isDebug) AndroidLog.v(tag, msg)
        }

        @JvmStatic
        fun v(tag: String, msg: String, tr: Throwable) {
            if (isDebug) AndroidLog.v(tag, msg, tr)
        }

        @JvmStatic
        fun d(tag: String, msg: String) {
            if (isDebug) AndroidLog.d(tag, msg)
        }

        @JvmStatic
        fun d(tag: String, msg: String, tr: Throwable) {
            if (isDebug) AndroidLog.d(tag, msg, tr)
        }

        @JvmStatic
        @Deprecated("Use Logger.tag(name).i(msg). Audit01 #03; Migration04 phase 02.")
        fun i(tag: String, msg: String) {
            AndroidLog.i(tag, msg)
            persistStatic(tag, msg, null)
        }

        @JvmStatic
        @Deprecated("Use Logger.tag(name).i(msg, tr). Audit01 #03; Migration04 phase 02.")
        fun i(tag: String, msg: String, tr: Throwable) {
            AndroidLog.i(tag, msg, tr)
            persistStatic(tag, msg, tr)
        }

        @JvmStatic
        @Deprecated("Use Logger.tag(name).w(msg). Audit01 #03; Migration04 phase 02.")
        fun w(tag: String, msg: String) {
            AndroidLog.w(tag, msg)
            persistStatic(tag, msg, null)
        }

        @JvmStatic
        @Deprecated("Use Logger.tag(name).w(msg, tr). Audit01 #03; Migration04 phase 02.")
        fun w(tag: String, msg: String, tr: Throwable) {
            AndroidLog.w(tag, msg, tr)
            persistStatic(tag, msg, tr)
        }

        @JvmStatic
        @Deprecated("Use Logger.tag(name).e(msg). Audit01 #03; Migration04 phase 02.")
        fun e(tag: String, msg: String) {
            AndroidLog.e(tag, msg)
            persistStatic(tag, msg, null)
        }

        @JvmStatic
        @Deprecated("Use Logger.tag(name).e(msg, tr). Audit01 #03; Migration04 phase 02.")
        fun e(tag: String, msg: String, tr: Throwable) {
            AndroidLog.e(tag, msg, tr)
            persistStatic(tag, msg, tr)
        }

        /**
         * Static counterpart to the instance [persist] helper. Kotlin doesn't allow companion
         * methods to invoke an instance member, so the body is duplicated verbatim. See [persist]
         * for the rationale on the try/catch and the `else -> FileLog.print(tag, msg)` branch.
         */
        private fun persistStatic(tag: String, msg: String, t: Throwable?) {
            try {
                when (t) {
                    null -> FileLog.print(tag, msg)
                    is Exception -> FileLog.print(tag, msg, t)
                    else -> FileLog.print(tag, msg)
                }
            } catch (ignored: Throwable) {
                // FileLog not available / no Looper / Handler bring-up failure on
                // the JVM unit-test classpath — fall through; logcat already saw it.
            }
        }
    }
}
