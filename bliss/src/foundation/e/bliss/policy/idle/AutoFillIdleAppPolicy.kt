/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/policy/idle/AutoFillIdleAppPolicy.kt
 * Module:  bliss main source-set  (foundation.e.bliss.policy.idle)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/policy/idle/):
 *   ├── AutoFillIdleAppPolicy.kt       — Bliss default (this file)             ← THIS FILE
 *   └── HonorGapsIdleAppPolicy.kt      — no-op when gaps are preserved
 *
 * Purpose:
 *   Default IdleAppPolicy: VerifyIdleAppTask should auto-fill empty
 *   workspace cells with installed-but-not-on-workspace apps. Selected by
 *   LauncherPolicy.idleApp() when PRESERVE_LAYOUT_GAPS is false.
 *
 * Consumed by:
 *   - foundation.e.bliss.policy.LauncherPolicy                 — idleApp()
 *
 * Plan reference: Plans/Migration04/05-launcher-policy-strategies.md §4.3
 */
package foundation.e.bliss.policy.idle

import foundation.e.bliss.policy.IdleAppPolicy

object AutoFillIdleAppPolicy : IdleAppPolicy {
    override fun shouldAutoFillIdle(): Boolean = true
}
