/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/src/foundation/e/bliss/firstrun/LawnchairBackupDetector.kt
 * Module:  bliss source-set  (foundation.e.bliss.firstrun)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/firstrun/):
 *   ├── FirstRunStep.kt              — interface every wizard step implements
 *   ├── FirstRunStateStore.kt        — Set<String>-backed completion ledger
 *   ├── FirstRunWizard.kt            — orchestrator: ALL_STEPS list, gates, launch()
 *   ├── LawnchairBackupDetector.kt   — scans Documents/Downloads for .lawnchairbackup  ← THIS FILE
 *   ├── steps/                       — concrete steps (LayoutModeStep, ImportLawnchairStep)
 *   └── ui/                          — FirstRunActivity + step-fragment UI
 *
 * Purpose:
 *   Tiny helper used by ImportLawnchairStep.shouldShow to decide whether
 *   to offer the Lawnchair-import step at all. Walks the user-accessible
 *   public storage roots (Documents, Downloads) for files ending in
 *   `.lawnchairbackup` and returns the most recently modified one's
 *   absolute path, or null if none are found. We don't bother with media-
 *   storage permissions or recursive scans — this is a best-effort
 *   "is the file lying somewhere obvious" probe; if it returns null the
 *   step self-skips and the user can still trigger import later from
 *   Settings.
 *
 * Consumed by:
 *   - foundation.e.bliss.firstrun.steps.ImportLawnchairStep   — shouldShow gate
 *
 * Plan reference: Plans/Migration04/07-first-run-wizard.md §5
 */
package foundation.e.bliss.firstrun

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * Best-effort lookup of a `.lawnchairbackup` file in the user's public storage. Returns the
 * absolute path of the most recently modified candidate, or null if nothing is found / accessible.
 */
object LawnchairBackupDetector {

    private const val TAG = "LawnchairBackupDetector"
    private const val SUFFIX = ".lawnchairbackup"

    /**
     * Scan the user-accessible storage roots and return the path of the most-recently-modified
     * `.lawnchairbackup` file, or null if none.
     *
     * The receiver context is unused but kept in the signature so a future implementation can fall
     * back to MediaStore or SAF without a caller-side ABI change.
     */
    @Suppress("UNUSED_PARAMETER")
    fun detect(ctx: Context): String? {
        val roots = candidateRoots()
        var best: File? = null
        for (root in roots) {
            val match = scanDirShallow(root) ?: continue
            if (best == null || match.lastModified() > best.lastModified()) {
                best = match
            }
        }
        return best?.absolutePath
    }

    /** Public-storage directories worth probing (top-level, no recursion). */
    private fun candidateRoots(): List<File> {
        val out = mutableListOf<File>()
        // Public Documents/ and Download/ — where users typically save backups.
        try {
            out += Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            out += Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        } catch (t: Throwable) {
            Log.w(TAG, "Couldn't resolve external public dirs", t)
        }
        // Also the storage root itself; some launchers / file managers drop
        // backups directly there.
        try {
            out += Environment.getExternalStorageDirectory()
        } catch (t: Throwable) {
            Log.w(TAG, "Couldn't resolve external storage root", t)
        }
        return out.filter { it.isDirectory }
    }

    /**
     * Return the most-recently-modified `.lawnchairbackup` directly under [dir], or null. We
     * deliberately don't recurse — the wizard probe is meant to be cheap and predictable.
     */
    private fun scanDirShallow(dir: File): File? {
        val files =
            try {
                dir.listFiles { f -> f.isFile && f.name.endsWith(SUFFIX) }
            } catch (t: Throwable) {
                Log.w(TAG, "listFiles failed for ${dir.absolutePath}", t)
                null
            } ?: return null
        if (files.isEmpty()) return null
        return files.maxByOrNull { it.lastModified() }
    }
}
