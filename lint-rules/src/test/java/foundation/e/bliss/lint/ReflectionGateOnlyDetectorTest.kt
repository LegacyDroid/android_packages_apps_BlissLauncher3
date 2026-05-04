/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    lint-rules/src/test/java/foundation/e/bliss/lint/ReflectionGateOnlyDetectorTest.kt
 * Module:  :lint-rules  (foundation.e.bliss.lint, test source-set)
 * Role:    NEW
 *
 * Tree (test/java/foundation/e/bliss/lint/):
 *   ├── ReflectionGateOnlyDetectorTest.kt   — exercises ReflectionGateOnlyDetector  ← THIS FILE
 *   └── PrefXmlKeyRegistryDetectorTest.kt   — exercises PrefXmlKeyRegistryDetector
 *
 * Purpose:
 *   Verifies the three behavioural cases from Plan §4 last paragraph:
 *     1) Class.forName inside ReflectionGate.kt — silent.
 *     2) Class.forName in any other source file — flagged.
 *     3) Class.forName in a unit-test source — silent (test-source skip).
 *
 *   Uses Lint's TestLintTask harness. Detector is configured with the same
 *   Severity.ERROR as production (promoted from WARNING by Audit01 #08), so
 *   `expect("…")` matches the rendered diagnostic verbatim.
 *
 * Plan reference: Plans/Migration05/03-custom-lint-rules.md §4
 */
package foundation.e.bliss.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class ReflectionGateOnlyDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = ReflectionGateOnlyDetector()

    override fun getIssues(): List<Issue> = listOf(ReflectionGateOnlyDetector.ISSUE)

    /** Class.forName inside ReflectionGate is the allow-listed call site. */
    @Test
    fun testReflectionGateOwnFile_isSilent() {
        lint()
            .files(reflectionGateSource())
            .run()
            .expectClean()
    }

    /** Any other Bliss source file calling Class.forName is flagged. */
    @Test
    fun testOtherFile_isFlagged() {
        lint()
            .files(
                kotlin(
                    """
                    package foundation.e.bliss.backup

                    object SomethingElse {
                        fun probe(): Class<*>? = try {
                            Class.forName("android.multiuser.Flags")
                        } catch (t: Throwable) {
                            null
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectWarningCount(0)
            .expectErrorCount(1)
            .expectContains("Use ReflectionGate.lookupClass")
            .expectContains("SomethingElse.kt")
    }

    /**
     * Java call sites are equally covered — JavaContext.SourceCodeScanner
     * is language-agnostic via UAST. (The detector uses
     * `getApplicableMethodNames`, which fires for both Kotlin and Java.)
     */
    @Test
    fun testJavaCallSite_isFlagged() {
        lint()
            .files(
                java(
                    """
                    package foundation.e.bliss.legacy;

                    public final class LegacyReflector {
                        public static Class<?> probe() throws Exception {
                            return Class.forName("android.multiuser.Flags");
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectWarningCount(0)
            .expectErrorCount(1)
            .expectContains("Use ReflectionGate.lookupClass")
            .expectContains("LegacyReflector.java")
    }

    /** Minimal stand-in for the production ReflectionGate.kt — same FQN. */
    private fun reflectionGateSource(): TestFile = kotlin(
        """
        package foundation.e.bliss.compat

        object ReflectionGate {
            fun classExists(name: String): Boolean = try {
                Class.forName(name) != null
            } catch (t: Throwable) {
                false
            }
        }
        """,
    ).indented()
}
