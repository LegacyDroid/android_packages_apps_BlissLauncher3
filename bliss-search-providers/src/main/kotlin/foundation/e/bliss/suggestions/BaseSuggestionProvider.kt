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
 * File:    bliss-search-providers/src/main/kotlin/foundation/e/bliss/suggestions/BaseSuggestionProvider.kt
 * Module:  :bliss-search-providers
 *
 * Structure:
 *   - SuggestionProvider/SuggestionsResult define the public contract.
 *   - BaseSuggestionProvider owns common HTTP execution and cancellation
 *     behavior for built-in providers.
 *   - duckduckgo/ and qwant/ own provider-specific response parsing.
 *
 * Boundary:
 *   This module receives logging and debug state from app adapters. It must
 *   not import com.android.launcher3.*, app BuildConfig, LauncherPrefs, or
 *   launcher UI classes.
 */
package foundation.e.bliss.suggestions

import foundation.e.bliss.core.logging.BlissLogger
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

abstract class BaseSuggestionProvider(
    private val logger: BlissLogger,
    private val httpClient: OkHttpClient,
) : SuggestionProvider {
    abstract val url: String

    abstract fun parseResponse(body: String): List<String>

    override suspend fun query(query: String): SuggestionsResult {
        val suggestions = SuggestionsResult(query)

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val requestUrl = url.replace("{query}", encodedQuery)

            val suggestionList =
                withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(requestUrl).build()
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            logger.w("Unsuccessful response: ${response.code}")
                            return@withContext emptyList()
                        }

                        val body = response.body?.string().orEmpty()
                        parseResponse(body).take(3)
                    }
                }

            suggestions.networkItems = suggestionList
        } catch (e: CancellationException) {
            logger.e("Suggestion query cancelled", e)
            throw e
        } catch (e: Exception) {
            logger.e("Failed to fetch suggestions", e)
        }

        return suggestions
    }

    companion object {
        val jsonParser = Json { ignoreUnknownKeys = true }

        fun defaultHttpClient(debug: Boolean): OkHttpClient {
            val builder =
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)

            if (debug) {
                val logging =
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                builder.addInterceptor(logging)
            }

            return builder.build()
        }
    }
}
