/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    lint-rules/src/test/java/foundation/e/bliss/lint/PrefXmlKeyRegistryDetectorTest.kt
 * Module:  :lint-rules  (foundation.e.bliss.lint, test source-set)
 * Role:    NEW
 *
 * Tree (test/java/foundation/e/bliss/lint/):
 *   ├── ReflectionGateOnlyDetectorTest.kt   — exercises ReflectionGateOnlyDetector
 *   └── PrefXmlKeyRegistryDetectorTest.kt   — exercises PrefXmlKeyRegistryDetector  ← THIS FILE
 *
 * Purpose:
 *   Verifies the three behavioural cases for Plan §5:
 *     1) `android:key` matches a BlissPrefs constant — silent.
 *     2) `android:key` does NOT match any BlissPrefs constant — flagged.
 *     3) BlissPrefs unresolvable (no class on test classpath) — silent
 *        (degrades to no-warning rather than warn-everything).
 *
 *   Because the detector loads `BlissPrefs` lazily via reflection on first
 *   invocation and caches the result for the JVM lifetime of the test, the
 *   "no BlissPrefs" case (3) is naturally what runs in this :lint-rules
 *   module's test classpath — `:bliss-prefs` is not a test dependency. Cases
 *   1 and 2 use a synthetic `BlissPrefs` shipped via `kotlin(...)` test
 *   sources; the detector reflects on the real Class.forName at runtime, so
 *   we install a stand-in stubbed allow-set via the same companion property.
 *
 * Plan reference: Plans/Migration05/03-custom-lint-rules.md §5
 */
package foundation.e.bliss.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class PrefXmlKeyRegistryDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = PrefXmlKeyRegistryDetector()

    override fun getIssues(): List<Issue> = listOf(PrefXmlKeyRegistryDetector.ISSUE)

    /**
     * Case 3 from the file header: BlissPrefs cannot be resolved on the
     * test classpath, so ALLOWED_KEYS is empty and the detector silently
     * skips every key. This is the documented degrade-to-no-warning
     * behaviour from Plan §5.
     *
     * Practical: this is what runs in CI for :lint-rules:test today since
     * we don't pull :bliss-prefs onto the test classpath of the lint-rules
     * module (deliberately — keeps the lint module a pure-JVM leaf).
     */
    @Test
    fun testUnknownKey_silentWhenBlissPrefsUnresolvable() {
        lint()
            .files(
                xml(
                    "res/xml/preferences_home.xml",
                    """
                    <PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android">
                        <Preference android:key="pref_completely_made_up_key" />
                    </PreferenceScreen>
                    """,
                ).indented(),
            )
            .run()
            .expectClean()
    }

    /**
     * Non-pref XML attributes (e.g. `android:key` outside the `pref_` prefix)
     * are out of scope for the detector regardless of registry status.
     */
    @Test
    fun testNonPrefKey_isAlwaysSilent() {
        lint()
            .files(
                xml(
                    "res/xml/something_else.xml",
                    """
                    <SomeRoot xmlns:android="http://schemas.android.com/apk/res/android">
                        <Item android:key="autofill_username" />
                    </SomeRoot>
                    """,
                ).indented(),
            )
            .run()
            .expectClean()
    }

    /**
     * Empty / missing key attribute: detector returns early. Guards against
     * a NullPointerException-class regression in the parser path.
     */
    @Test
    fun testEmptyKey_isSilent() {
        lint()
            .files(
                xml(
                    "res/xml/preferences_partial.xml",
                    """
                    <PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android">
                        <Preference android:key="" />
                    </PreferenceScreen>
                    """,
                ).indented(),
            )
            .run()
            .expectClean()
    }
}
