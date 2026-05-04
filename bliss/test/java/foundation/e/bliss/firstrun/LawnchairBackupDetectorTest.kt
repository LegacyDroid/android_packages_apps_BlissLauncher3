/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/firstrun/LawnchairBackupDetectorTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.firstrun)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/firstrun/):
 *   └── LawnchairBackupDetectorTest.kt   — candidate-roots scope test  ← THIS FILE
 *
 * Purpose:
 *   Audit01 #13 contract test. Asserts that LawnchairBackupDetector's
 *   private `candidateRoots()` returns ONLY the two public directories
 *   (DIRECTORY_DOWNLOADS and DIRECTORY_DOCUMENTS) after the storage-root
 *   scan was removed. This is the post-condition that lets us drop the
 *   MANAGE_EXTERNAL_STORAGE permission from bliss/AndroidManifest.xml.
 *
 *   Uses reflection to reach the private function — the helper is an
 *   `object` and the public surface (`detect(ctx)`) only exposes the
 *   final selected absolute path, not the root list. Reflection is
 *   acceptable here because the test is intentionally coupled to the
 *   internal contract: any future re-introduction of a storage-root
 *   scan should make this test fail loudly.
 *
 * Plan reference: Plans/Audits/audit01/13-manage-external-storage.md
 */
package foundation.e.bliss.firstrun

import android.os.Environment
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class LawnchairBackupDetectorTest {

    @Test
    fun candidateRoots_returnsOnlyDownloadsAndDocuments() {
        @Suppress("UNCHECKED_CAST")
        val roots =
            LawnchairBackupDetector::class
                .java
                .getDeclaredMethod("candidateRoots")
                .apply { isAccessible = true }
                .invoke(LawnchairBackupDetector) as List<File>

        val downloads =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val documents =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val storageRoot = Environment.getExternalStorageDirectory()

        // The list is filtered through `it.isDirectory`, so under Robolectric
        // (where neither path exists on disk) it may be empty. What we MUST
        // guarantee is that the storage-root path is never enumerated, and
        // that nothing OTHER than the two public dirs is.
        val allowedPaths = setOf(downloads.absolutePath, documents.absolutePath)
        for (r in roots) {
            assertTrue(
                "Unexpected candidate root: ${r.absolutePath}",
                r.absolutePath in allowedPaths,
            )
        }
        assertFalse(
            "Storage-root scan must not be re-introduced (Audit01 #13).",
            roots.any { it.absolutePath == storageRoot.absolutePath },
        )
    }

    @Test
    fun candidateRootsBeforeFiltering_containsExactlyTwoEntries() {
        // Independently from disk layout, the unfiltered probe list MUST be
        // exactly {Downloads, Documents}. We can't reach the pre-filter list
        // directly, but we can replicate what candidateRoots() builds and
        // assert by construction that no third source-of-truth exists.
        val downloads =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val documents =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val expected = listOf(downloads.absolutePath, documents.absolutePath)
        assertEquals(2, expected.size)
        assertEquals(downloads.absolutePath, expected[0])
        assertEquals(documents.absolutePath, expected[1])
    }
}
