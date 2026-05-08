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
 * File:    bliss-search-providers/src/main/kotlin/foundation/e/bliss/suggestions/qwant/QwantProvider.kt
 * Module:  :bliss-search-providers
 *
 * Usage:
 *   Constructed by app-owned SearchSuggestionUtil with a BlissLoggerFactory
 *   and shared OkHttpClient. Parses Qwant suggestion JSON and returns values
 *   only when the service reports a successful status.
 */
package foundation.e.bliss.suggestions.qwant

import foundation.e.bliss.core.logging.BlissLoggerFactory
import foundation.e.bliss.suggestions.BaseSuggestionProvider
import okhttp3.OkHttpClient

class QwantProvider(
    loggerFactory: BlissLoggerFactory,
    httpClient: OkHttpClient,
) : BaseSuggestionProvider(loggerFactory.tag(TAG), httpClient) {
    override val url = "https://api.qwant.com/api/suggest/?q={query}"

    override fun parseResponse(body: String): List<String> {
        val result = jsonParser.decodeFromString<QwantResult>(body)
        return if (result.status == SUCCESS_STATUS) {
            result.data?.items?.mapNotNull { it.value } ?: emptyList()
        } else {
            emptyList()
        }
    }

    companion object {
        private const val SUCCESS_STATUS = "success"
        private const val TAG = "QwantProvider"
    }
}
