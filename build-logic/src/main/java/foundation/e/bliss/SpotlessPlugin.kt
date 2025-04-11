/*
 * Copyright (C) 2025 MURENA SAS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package foundation.e.bliss

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

@Suppress("Unused")
class SpotlessPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val rootDir = project.rootDir
        val licenseFile = File("${rootDir}/HEADER")

        // Dynamically exclude files that contain "Copyright (C)"
        val excludedFiles =
            project
                .fileTree(rootDir) {
                    include("bliss/**/*.java")
                    include("bliss/**/*.kt")
                    exclude("**/build/**")
                }
                .filter { it.readText().contains("Copyright (C)") }
                .map { it.relativeTo(rootDir).path }

        project.pluginManager.apply(SpotlessPlugin::class)
        project.extensions.getByType<SpotlessExtension>().run {
            java {
                target("bliss/**/*.java", "build-logic/**/*.java")
                removeUnusedImports()
                eclipse()
                leadingSpacesToTabs(2)
                leadingTabsToSpaces(4)
            }

            kotlin {
                ktfmt().kotlinlangStyle()
                target("bliss/**/*.kt", "build-logic/**/*.kt")
                targetExclude("**/build/")
                trimTrailingWhitespace()
            }

            // === License Header Only ===
            format("kotlinLicense") {
                target("bliss/**/*.java", "bliss/**/*.kt")
                targetExclude("**/build/")
                targetExclude(excludedFiles)
                licenseHeaderFile(licenseFile, "^package ")
            }

            kotlinGradle {
                ktfmt().kotlinlangStyle()
                target("bliss/**/*.gradle.kts", "build-logic/**/*.gradle.kts")
                targetExclude("**/build/")
            }

            groovyGradle {
                target("*.gradle", "bliss/**/*.gradle", "build-logic/**/*.gradle")
                targetExclude("**/build/")
            }

            format("xml") {
                target("bliss/**/*.xml")
                targetExclude("**/build/", ".idea/")
                trimTrailingWhitespace()
                leadingTabsToSpaces()
            }
        }
    }
}
