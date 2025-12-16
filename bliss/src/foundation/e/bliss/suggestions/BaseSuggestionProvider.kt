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
package foundation.e.bliss.suggestions

import com.android.launcher3.BuildConfig
import foundation.e.bliss.utils.Logger
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

abstract class BaseSuggestionProvider : SuggestionProvider {
    abstract val tag: String
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
                            Logger.w(tag, "Unsuccessful response: ${response.code}")
                            return@withContext emptyList()
                        }

                        val body = response.body?.string().orEmpty()
                        parseResponse(body).take(3)
                    }
                }

            suggestions.networkItems = suggestionList
        } catch (e: CancellationException) {
            Logger.e(tag, "Query cancelled for \"$query\"", e)
            throw e
        } catch (e: Exception) {
            Logger.e(tag, "Failed to fetch suggestions for \"$query\"", e)
        }

        return suggestions
    }

    companion object {
        val jsonParser = Json { ignoreUnknownKeys = true }

        val httpClient: OkHttpClient by lazy {
            val builder =
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)

            if (BuildConfig.IS_DEBUG_DEVICE) {
                val logging =
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                builder.addInterceptor(logging)
            }

            builder.build()
        }
    }
}
