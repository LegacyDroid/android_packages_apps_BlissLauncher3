/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss-core-contracts/src/main/kotlin/foundation/e/bliss/core/logging/BlissLogger.kt
 * Module:  :bliss-core-contracts
 * Role:    Logging contract for extracted Bliss modules.
 *
 * Owned by:
 *   - :bliss-core-contracts
 *
 * Consumed by:
 *   - App-root Logger adapter while Logger remains in the root app source-set.
 *   - Future extracted modules that need logging without depending on
 *     com.android.launcher3.BuildConfig or FileLog.
 *
 * Dependency rules:
 *   - May use Kotlin/JDK types only.
 *   - Must not import com.android.launcher3.* or Android framework UI classes.
 */
package foundation.e.bliss.core.logging

interface BlissLogger {
    fun v(message: String, throwable: Throwable? = null)

    fun d(message: String, throwable: Throwable? = null)

    fun i(message: String, throwable: Throwable? = null)

    fun w(message: String, throwable: Throwable? = null)

    fun e(message: String, throwable: Throwable? = null)
}
