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
            description = "Creates the Git pre-commit hook with predefined content."

            doLast {
                val gitHooksDir = project.gitHooksDir()
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

    private fun Project.gitHooksDir(): File {
        val process =
            ProcessBuilder("git", "rev-parse", "--git-path", "hooks")
                .directory(rootDir)
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.use { it.readBytes() }
        if (process.waitFor() == 0) {
            val path = String(output).trim()
            if (path.isNotEmpty())
                return File(path).let { if (it.isAbsolute) it else File(rootDir, path) }
        }
        return File(rootDir, ".git/hooks")
    }
}
