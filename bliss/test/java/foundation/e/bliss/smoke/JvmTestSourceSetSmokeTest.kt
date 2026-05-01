/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss/test/java/foundation/e/bliss/smoke/JvmTestSourceSetSmokeTest.kt
 * Module:  bliss test source-set  (foundation.e.bliss.smoke)
 * Role:    NEW
 *
 * Tree (foundation/e/bliss/smoke/):
 *   └── JvmTestSourceSetSmokeTest.kt  — verifies the JVM test source-set is wired  ← THIS FILE
 *
 * Purpose:
 *   Trivial smoke test — exists solely so we can run
 *   `./gradlew testBlissWithQuickstepDebugUnitTest` and confirm the test
 *   source-set is wired up before adding heavier suites. Does not depend on
 *   Robolectric or any Android API.
 *
 * Plan reference: Plans/Migration04/08-observability-and-testing.md §3.1
 */
package foundation.e.bliss.smoke

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JvmTestSourceSetSmokeTest {

    @Test
    fun trivialAssertionsPass() {
        assertTrue(true)
        assertEquals(4, 2 + 2)
    }
}
