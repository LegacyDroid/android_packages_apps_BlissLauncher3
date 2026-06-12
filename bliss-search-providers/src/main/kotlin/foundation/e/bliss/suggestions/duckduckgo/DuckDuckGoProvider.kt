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
 */
/*
 * File:    bliss-search-providers/src/main/kotlin/foundation/e/bliss/suggestions/duckduckgo/DuckDuckGoProvider.kt
 * Module:  :bliss-search-providers
 *
 * Usage:
 *   Constructed by app-owned SearchSuggestionUtil with a BlissLoggerFactory
 *   and shared OkHttpClient. Parses DuckDuckGo autocomplete JSON into plain
 *   suggestion strings for SuggestionsResult.networkItems.
 */
package foundation.e.bliss.suggestions.duckduckgo

import foundation.e.bliss.core.logging.BlissLoggerFactory
import foundation.e.bliss.suggestions.BaseSuggestionProvider
import okhttp3.OkHttpClient

class DuckDuckGoProvider(
    loggerFactory: BlissLoggerFactory,
    httpClient: OkHttpClient,
) : BaseSuggestionProvider(loggerFactory.tag(TAG), httpClient) {
    override val url = "https://duckduckgo.com/ac/?q={query}"

    override fun parseResponse(body: String): List<String> {
        val results = jsonParser.decodeFromString<List<DuckDuckGoResult>>(body)
        return results.mapNotNull { it.phrase }
    }

    companion object {
        private const val TAG = "DuckDuckGoProvider"
    }
}
