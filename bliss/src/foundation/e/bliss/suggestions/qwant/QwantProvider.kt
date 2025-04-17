/*
 * Copyright (C) 2024 MURENA SAS
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
package foundation.e.bliss.suggestions.qwant

import foundation.e.bliss.suggestions.RetrofitService
import foundation.e.bliss.suggestions.SuggestionProvider
import foundation.e.bliss.suggestions.SuggestionsResult
import java.io.IOException
import retrofit2.HttpException
import timber.log.Timber

class QwantProvider : SuggestionProvider {

    private val suggestionService: QwantApi
        get() = RetrofitService.getInstance(QwantApi.BASE_URL).create(QwantApi::class.java)

    override suspend fun query(query: String): SuggestionsResult {
        return try {
            val result = suggestionService.query(query)
            Timber.d("Result: $result")
            SuggestionsResult(query).apply {
                networkItems =
                    if (result.status == "success") {
                        result.data?.items?.map { it.value }?.take(3) ?: emptyList()
                    } else emptyList()
            }
        } catch (e: HttpException) {
            Timber.e("HTTP error: ${e.code()} - ${e.message()}")
            SuggestionsResult(query).apply { networkItems = emptyList() }
        } catch (e: IOException) {
            Timber.e("IO error: $e - ${e.message}")
            SuggestionsResult(query).apply { networkItems = emptyList() }
        }
    }
}
