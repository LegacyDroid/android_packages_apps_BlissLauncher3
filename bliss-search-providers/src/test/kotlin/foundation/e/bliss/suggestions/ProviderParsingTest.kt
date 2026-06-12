/*
 * Copyright (C) 2026 MURENA SAS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
/*
 * File:    bliss-search-providers/src/test/kotlin/foundation/e/bliss/suggestions/ProviderParsingTest.kt
 * Module:  :bliss-search-providers
 *
 * Test scope:
 *   Verifies provider-owned JSON parsing without live network. HTTP transport
 *   behavior remains covered by BaseSuggestionProvider structure and can move
 *   to MockWebServer once that dependency is introduced deliberately.
 */
package foundation.e.bliss.suggestions

import foundation.e.bliss.core.logging.BlissLogger
import foundation.e.bliss.core.logging.BlissLoggerFactory
import foundation.e.bliss.suggestions.duckduckgo.DuckDuckGoProvider
import foundation.e.bliss.suggestions.qwant.QwantProvider
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderParsingTest {

    private val httpClient = OkHttpClient()

    @Test
    fun duckDuckGoParseResponseReturnsPhraseValues() {
        val provider = DuckDuckGoProvider(FakeLoggerFactory, httpClient)

        val result =
            provider.parseResponse(
                """[{"phrase":"calendar"},{"phrase":"calculator"},{"ignored":"missing phrase"}]"""
            )

        assertEquals(listOf("calendar", "calculator"), result)
    }

    @Test
    fun qwantParseResponseReturnsItemsForSuccessfulStatus() {
        val provider = QwantProvider(FakeLoggerFactory, httpClient)

        val result =
            provider.parseResponse(
                """
                {
                  "status": "success",
                  "data": {
                    "items": [
                      {"value": "calendar", "suggestType": 0},
                      {"value": "calculator", "suggestType": 0},
                      {"suggestType": 0}
                    ],
                    "special": []
                  }
                }
                """.trimIndent()
            )

        assertEquals(listOf("calendar", "calculator"), result)
    }

    @Test
    fun qwantParseResponseReturnsEmptyListForFailedStatus() {
        val provider = QwantProvider(FakeLoggerFactory, httpClient)

        val result =
            provider.parseResponse(
                """{"status":"error","data":{"items":[{"value":"calendar"}],"special":[]}}"""
            )

        assertEquals(emptyList<String>(), result)
    }
}

private object FakeLoggerFactory : BlissLoggerFactory {
    override fun tag(name: String): BlissLogger = FakeLogger
}

private object FakeLogger : BlissLogger {
    override fun v(message: String, throwable: Throwable?) = Unit
    override fun d(message: String, throwable: Throwable?) = Unit
    override fun i(message: String, throwable: Throwable?) = Unit
    override fun w(message: String, throwable: Throwable?) = Unit
    override fun e(message: String, throwable: Throwable?) = Unit
}
