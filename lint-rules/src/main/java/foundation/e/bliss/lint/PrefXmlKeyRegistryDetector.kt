/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    lint-rules/src/main/java/foundation/e/bliss/lint/PrefXmlKeyRegistryDetector.kt
 * Module:  :lint-rules  (foundation.e.bliss.lint)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/lint/):
 *   ├── BlissIssueRegistry.kt           — registers the two detectors
 *   ├── ReflectionGateOnlyDetector.kt   — invariant: Class.forName only in ReflectionGate
 *   └── PrefXmlKeyRegistryDetector.kt   — invariant: every <pref android:key> exists in BlissPrefs  ← THIS FILE
 *
 * Purpose:
 *   Enforces Migration04 §3 principle 2 ("the registry is the single source
 *   of truth"). Every `android:key` attribute appearing in any preference XML
 *   must correspond to a `PREF_*` String constant on
 *   `foundation.e.bliss.preferences.BlissPrefs`. The runtime self-test in
 *   `BlissPrefSchema.assertAllPrefKeysAreRegistered` only fires on debug
 *   builds; this detector catches drift on every CI build, including release.
 *
 *   Implementation strategy: Plan §5 Option A — reflection on the consuming
 *   project's classpath. The detector lazily loads `BlissPrefs` via
 *   `Class.forName(...)`. NOTE: this is the ONE allowed `Class.forName` site
 *   in the codebase aside from `ReflectionGate.kt`. It is intentional: the
 *   detector lives in :lint-rules (NOT main source), so the sibling
 *   `ReflectionGateOnlyDetector` does not scan it. Documented here so a
 *   future grep is not surprised.
 *
 * Consumed by:
 *   - foundation.e.bliss.lint.BlissIssueRegistry — exposes ISSUE
 *   - Android Lint runtime — invokes visitAttribute on every <… android:key>
 *
 * Calls into:
 *   - foundation.e.bliss.preferences.BlissPrefs (via reflection at lint time)
 *
 * Plan reference: Plans/Migration05/03-custom-lint-rules.md §5 (Option A)
 */
package foundation.e.bliss.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import org.w3c.dom.Attr
import java.lang.reflect.Modifier

/**
 * XML-scoped detector that walks every `android:key` attribute in resource
 * XML and verifies it appears as a `String` constant on `BlissPrefs`.
 *
 * Falls back to a no-op (allow-everything) when `BlissPrefs` cannot be
 * resolved via reflection — degrades to no-warning rather than warn-everything,
 * which matters during early-build phases where the registry hasn't compiled
 * yet (Plan §5).
 */
class PrefXmlKeyRegistryDetector : ResourceXmlDetector(), XmlScanner {

    companion object {
        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "PrefXmlKeyRegistry",
            briefDescription = "Pref XML key not declared in BlissPrefs",
            explanation = """
                Every `<… android:key="pref_xxx" …>` attribute in preference \
                XML must exist as a `const val PREF_<NAME> = "pref_xxx"` entry \
                on `foundation.e.bliss.preferences.BlissPrefs`.

                A pref-key typo silently disables the preference on release \
                builds (the BlissPrefSchema runtime self-test only runs in \
                debug). This detector closes the gap at build time.

                See `Plans/Migration04/03-prefs-registry.md` §3.1.
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                PrefXmlKeyRegistryDetector::class.java,
                Scope.RESOURCE_FILE_SCOPE,
            ),
        )

        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        private const val ATTR_KEY = "key"
        private const val BLISS_PREFS_FQN = "foundation.e.bliss.preferences.BlissPrefs"

        /**
         * Resolved lazily on first detector invocation. Empty set means
         * "BlissPrefs not on classpath" → suppress all warnings (Plan §5).
         *
         * Reflection here is intentional and is the one exception to the
         * `ReflectionGateOnly` invariant — see file header. The detector lives
         * in the :lint-rules module which is not scanned by Lint itself.
         */
        private val ALLOWED_KEYS: Set<String> by lazy { resolveBlissPrefs() }

        private fun resolveBlissPrefs(): Set<String> = try {
            val cls = Class.forName(BLISS_PREFS_FQN)
            cls.declaredFields
                .filter { Modifier.isStatic(it.modifiers) }
                .filter { it.type == String::class.java }
                .mapNotNull {
                    it.isAccessible = true
                    it.get(null) as? String
                }
                .toSet()
        } catch (t: Throwable) {
            emptySet()
        }
    }

    override fun getApplicableAttributes(): Collection<String> = listOf(ATTR_KEY)

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        // Only `android:key`, not arbitrary `key` attributes from other
        // namespaces (e.g. app:key on AndroidX widgets that might happen to
        // exist under a different schema URI).
        if (attribute.namespaceURI != ANDROID_NS) return

        val key = attribute.value ?: return
        if (key.isEmpty()) return

        // Pref XML keys uniformly use the "pref_" prefix in BlissLauncher3.
        // Any other key namespace (e.g. autofill keys in form layouts) is out
        // of scope for this invariant.
        if (!key.startsWith("pref_")) return

        val allowed = ALLOWED_KEYS
        if (allowed.isEmpty()) {
            // Couldn't resolve BlissPrefs — degrade to silent rather than
            // warn-everything (Plan §5).
            return
        }
        if (key in allowed) return

        context.report(
            ISSUE,
            attribute,
            context.getLocation(attribute),
            "Pref key '$key' is not declared in BlissPrefs. " +
                "Add a `const val PREF_… = \"$key\"` entry, or fix the typo. " +
                "See Plans/Migration04/03-prefs-registry.md §3.1.",
        )
    }
}
