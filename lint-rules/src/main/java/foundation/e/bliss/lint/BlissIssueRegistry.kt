/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    lint-rules/src/main/java/foundation/e/bliss/lint/BlissIssueRegistry.kt
 * Module:  :lint-rules  (foundation.e.bliss.lint)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/lint/):
 *   ├── BlissIssueRegistry.kt           — registers the two detectors  ← THIS FILE
 *   ├── ReflectionGateOnlyDetector.kt   — invariant: Class.forName only in ReflectionGate
 *   └── PrefXmlKeyRegistryDetector.kt   — invariant: every <pref android:key> exists in BlissPrefs
 *
 * Purpose:
 *   Lint discovery point for the BlissLauncher3 custom checks. Lint scans the
 *   classpath for `META-INF/services/com.android.tools.lint.client.api.IssueRegistry`
 *   service files and instantiates whatever class is listed there. The two
 *   architectural invariants from Migration04 (centralised reflection,
 *   single-source-of-truth pref keys) are surfaced as build-time warnings
 *   via the issues exposed here.
 *
 * Consumed by:
 *   - Android Lint runtime (via META-INF/services discovery)
 *
 * Calls into:
 *   - foundation.e.bliss.lint.ReflectionGateOnlyDetector  — its ISSUE singleton
 *   - foundation.e.bliss.lint.PrefXmlKeyRegistryDetector  — its ISSUE singleton
 *
 * Plan reference: Plans/Migration05/03-custom-lint-rules.md §3
 */
package foundation.e.bliss.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

/**
 * Registers the two architectural-invariant detectors with Android Lint.
 *
 * Discovery happens via the standard `META-INF/services/...IssueRegistry` file
 * shipped under `lint-rules/src/main/resources/`. No reflection magic on the
 * Bliss side — Lint itself looks up the class by name at start-up.
 */
class BlissIssueRegistry : IssueRegistry() {

    override val issues = listOf(
        ReflectionGateOnlyDetector.ISSUE,
        PrefXmlKeyRegistryDetector.ISSUE,
        BlissModuleBoundaryDetector.ISSUE,
    )

    override val api: Int = CURRENT_API

    // 8 is the minimum Lint API the modern detector contract has been stable
    // on; bumping this only matters if we adopt a newer-API-only call.
    override val minApi: Int = 8

    override val vendor: Vendor = Vendor(
        vendorName = "BlissLauncher3 / Murena",
        feedbackUrl = "https://gitlab.e.foundation/e/devices/blisslauncher",
        contact = "https://murena.com",
    )
}
