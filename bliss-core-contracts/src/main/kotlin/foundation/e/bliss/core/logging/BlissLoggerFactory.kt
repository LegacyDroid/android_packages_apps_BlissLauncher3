/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss-core-contracts/src/main/kotlin/foundation/e/bliss/core/logging/BlissLoggerFactory.kt
 * Module:  :bliss-core-contracts
 * Role:    Factory contract for tag-scoped BlissLogger instances.
 *
 * Owned by:
 *   - :bliss-core-contracts
 *
 * Consumed by:
 *   - App-root Logger adapter while Logger remains in the root app source-set.
 *   - Future extracted modules that should receive logging through
 *     constructor/factory injection.
 *
 * Dependency rules:
 *   - May use Kotlin/JDK types only.
 *   - Must not import com.android.launcher3.* or Android framework UI classes.
 */
package foundation.e.bliss.core.logging

interface BlissLoggerFactory {
    fun tag(name: String): BlissLogger
}
