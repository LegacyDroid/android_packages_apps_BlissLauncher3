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

import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("Unused")
class GitHooksPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.register("createPreCommitHook") {
            description = "Creates the .git/hooks/pre-commit file with predefined content."

            doLast {
                val gitHooksDir = File(project.rootDir, ".git/hooks")
                if (!gitHooksDir.exists()) {
                    gitHooksDir.mkdirs()
                }

                val preCommitFile = File(gitHooksDir, "pre-commit")
                if (preCommitFile.exists()) return@doLast
                val content =
                    """
                    #!/bin/bash

                    [ -f ./hooks/pre-commit.sh ] && ./hooks/pre-commit.sh
                     """
                        .trimIndent()

                preCommitFile.writeText(content)
                preCommitFile.setExecutable(true)
            }
        }
    }
}
