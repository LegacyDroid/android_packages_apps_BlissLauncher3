/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    lint-rules/src/main/java/foundation/e/bliss/lint/ReflectionGateOnlyDetector.kt
 * Module:  :lint-rules  (foundation.e.bliss.lint)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/lint/):
 *   ├── BlissIssueRegistry.kt           — registers the two detectors
 *   ├── ReflectionGateOnlyDetector.kt   — invariant: Class.forName only in ReflectionGate  ← THIS FILE
 *   └── PrefXmlKeyRegistryDetector.kt   — invariant: every <pref android:key> exists in BlissPrefs
 *
 * Purpose:
 *   Enforces Migration04 §3 principle 4 ("reflection is centralised"). All
 *   Class.forName lookups in BlissLauncher3 must go through
 *   foundation.e.bliss.compat.ReflectionGate, which caches the result and
 *   swallows Throwable centrally. Direct call sites bypass that contract
 *   and become silent regression risks (no R8 keep-rule audit, no caching).
 *
 * Consumed by:
 *   - foundation.e.bliss.lint.BlissIssueRegistry — exposes ISSUE
 *   - Android Lint runtime — invokes visitMethodCall on every Class.forName
 *
 * Calls into:
 *   - (Lint internals; no Bliss-side calls)
 *
 * Plan reference: Plans/Migration05/03-custom-lint-rules.md §4
 */
package foundation.e.bliss.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getContainingUFile

/**
 * Detector that flags any Class.forName call site outside the allow-listed
 * ReflectionGate object. Severity is intentionally WARNING for Migration05;
 * promotion to ERROR is gated on a full clean run (Plan §7) and lands in a
 * follow-up migration once the codebase is verified violation-free.
 */
class ReflectionGateOnlyDetector : Detector(), SourceCodeScanner {

    companion object {
        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "ReflectionGateOnly",
            briefDescription = "Class.forName outside ReflectionGate",
            explanation = """
                Reflection in BlissLauncher3 must go through \
                `foundation.e.bliss.compat.ReflectionGate`. Direct calls bypass \
                its caching, Throwable handling, and R8 keep-rule audit.

                See `Plans/Migration04/01-compat-platform.md` §3.2.
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                ReflectionGateOnlyDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )

        private const val FORBIDDEN_OWNER = "java.lang.Class"
        private const val FORBIDDEN_METHOD = "forName"
        private const val ALLOWED_QUALIFIED_CLASS =
            "foundation.e.bliss.compat.ReflectionGate"
    }

    override fun getApplicableMethodNames(): List<String> = listOf(FORBIDDEN_METHOD)

    override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod,
    ) {
        // Only Class.forName, not arbitrary forName(...) on other classes.
        val owner = method.containingClass?.qualifiedName ?: return
        if (owner != FORBIDDEN_OWNER) return

        // Skip test sources — invariant applies to production reflection only.
        // `Scope.JAVA_FILE_SCOPE` excludes test sources by default in production
        // lint runs, but lintBlissWithQuickstepDebugAndroidTest can still
        // request a test-scoped run; guard explicitly.
        if (context.isTestSource) return

        // Walk up to the directly-enclosing class of the call site. ReflectionGate
        // is a Kotlin `object`, so the call site's containing UClass has the
        // FQN we want. We also tolerate any nested helper class inside it.
        // Using getContainingUClass (not UFile.classes[0]) keeps us correct when
        // Lint's TYPE_ALIAS test mode synthesises extra top-level decls.
        val enclosingClass = node.getContainingUClass()
        var enclosingFqn = enclosingClass?.qualifiedName
        if (enclosingFqn == null) {
            // Top-level kotlin function: fall back to UFile.classes[0].
            enclosingFqn = node.getContainingUFile()
                ?.classes
                ?.firstOrNull()
                ?.qualifiedName
                ?: return
        }
        if (enclosingFqn == ALLOWED_QUALIFIED_CLASS) return
        // Any nested class declared *inside* ReflectionGate is also allowed
        // (e.g. private data classes used by the cache implementation).
        if (enclosingFqn.startsWith("$ALLOWED_QUALIFIED_CLASS.") ||
            enclosingFqn.startsWith("$ALLOWED_QUALIFIED_CLASS$")
        ) return

        context.report(
            ISSUE,
            node,
            context.getLocation(node),
            "Use ReflectionGate.lookupClass / lookupMethod / invokeStaticBoolean " +
                "instead of Class.forName.",
        )
    }
}
