/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * File:    lint-rules/src/main/java/foundation/e/bliss/lint/BlissModuleBoundaryDetector.kt
 * Module:  :lint-rules
 * Role:    Warns when extracted modules import forbidden packages.
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
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement

class BlissModuleBoundaryDetector : Detector(), SourceCodeScanner {

    companion object {
        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "BlissModuleBoundary",
            briefDescription = "Forbidden import for module boundary",
            explanation = """
                Extracted Bliss modules must not import packages that would \
                re-introduce coupling to the root app or other forbidden \
                dependencies. This detector checks import statements against \
                the per-module forbidden-prefix rules defined by Audit03.
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                BlissModuleBoundaryDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )

        private val FORBIDDEN_RULES: Map<String, List<String>> = mapOf(
            "bliss-policy-core" to listOf("com.android.launcher3.", "android."),
            "bliss-animation-safety" to listOf("com.android.launcher3."),
            "bliss-widgets-data" to listOf(
                "com.android.launcher3.",
                "foundation.e.bliss.widgets.WidgetContainer",
                "foundation.e.bliss.utils.Logger",
            ),
            "bliss-search-providers" to listOf(
                "com.android.launcher3.",
                "foundation.e.bliss.utils.Logger",
            ),
            "bliss-backup-parser" to listOf(
                "com.android.launcher3.",
                "foundation.e.bliss.utils.Logger",
            ),
            "bliss-core-contracts" to listOf(
                "com.android.launcher3.",
                "android.",
                "androidx.",
                "okhttp3.",
                "room.",
            ),
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UImportStatement::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : Detector.UastScanner, com.android.tools.lint.detector.api.UElementHandler() {
            override fun visitImportStatement(node: UImportStatement) {
                if (context.isTestSource) return

                val path = context.file.invariantSeparatorsPath
                val imported = node.importReference?.asSourceString() ?: return
                val forbidden = forbiddenPrefixesFor(path) ?: return

                for (prefix in forbidden) {
                    if (imported.startsWith(prefix)) {
                        context.report(
                            ISSUE,
                            node,
                            context.getLocation(node),
                            "Forbidden import '$imported' for module boundary",
                        )
                        break
                    }
                }
            }
        }

    private fun forbiddenPrefixesFor(filePath: String): List<String>? {
        for ((moduleDir, prefixes) in FORBIDDEN_RULES) {
            if (filePath.contains("/$moduleDir/")) {
                return prefixes
            }
        }
        return null
    }
}
