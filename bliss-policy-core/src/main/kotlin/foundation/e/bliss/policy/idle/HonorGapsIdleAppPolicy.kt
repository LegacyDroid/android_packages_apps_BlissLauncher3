/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss-policy-core/src/main/kotlin/foundation/e/bliss/policy/idle/HonorGapsIdleAppPolicy.kt
 * Module:  :bliss-policy-core
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/policy/idle/):
 *   ├── AutoFillIdleAppPolicy.kt       — Bliss default (auto-fills empty cells)
 *   └── HonorGapsIdleAppPolicy.kt      — no-op when gaps are preserved (this file) ← THIS FILE
 *
 * Purpose:
 *   IdleAppPolicy used when PRESERVE_LAYOUT_GAPS is on. Tells callers NOT to
 *   auto-fill empty cells — the user's intentional gaps survive. Selected by
 *   LauncherPolicy.idleApp() when PRESERVE_LAYOUT_GAPS is true.
 *
 * Consumed by:
 *   - foundation.e.bliss.policy.LauncherPolicy                 — idleApp()
 *
 * Plan reference: Plans/Migration04/05-launcher-policy-strategies.md §4.3
 */
package foundation.e.bliss.policy.idle

import foundation.e.bliss.policy.IdleAppPolicy

val HonorGapsIdleAppPolicy = IdleAppPolicy { false }
