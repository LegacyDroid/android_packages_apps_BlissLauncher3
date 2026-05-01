/*
 * Copyright (C) 2026 MURENA SAS
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

import android.content.Context
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.dagger.LauncherComponentProvider
import foundation.e.bliss.utils.Logger
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

// PLAN-DRIFT-M02: see 13-drift-log.md entry for Phase 6.2 — the existing
// BaseSuggestionProvider is an abstract class (not an interface) keyed on
// `tag` / `url` / `parseResponse`. This provider needs runtime-configurable
// URL + timeout + count, so it overrides `query()` directly instead of
// reusing the static `url`/`parseResponse` template path.

/**
 * User-configurable web-suggestion backend. Reads URL/name/timeout/count from
 * `LauncherPrefs.WEB_SUGGESTION_*` and parses DuckDuckGo-style `[query, [s1, s2, ...]]` JSON
 * responses. Used when the user has populated the custom URL pref; otherwise the standard provider
 * chain (Qwant / DuckDuckGo) is selected by [SearchSuggestionUtil.getSuggestionProvider].
 */
class CustomUrlProvider(private val context: Context) : SuggestionProvider {

    override suspend fun query(query: String): SuggestionsResult {
        val results = SuggestionsResult(query)
        val prefs =
            try {
                LauncherComponentProvider.get(context).launcherPrefs
            } catch (e: Exception) {
                Logger.e(TAG, "LauncherPrefs unavailable", e)
                return results
            }

        val urlTemplate = prefs.get(LauncherPrefs.WEB_SUGGESTION_URL)
        if (urlTemplate.isEmpty() || !urlTemplate.contains("%s")) return results

        val timeoutMs = prefs.get(LauncherPrefs.WEB_SUGGESTION_MAX_DELAY)
        val maxCount = prefs.get(LauncherPrefs.WEB_SUGGESTION_MAX_COUNT).coerceAtLeast(1)
        val target =
            try {
                URL(urlTemplate.replace("%s", URLEncoder.encode(query, "UTF-8")))
            } catch (e: Exception) {
                Logger.e(TAG, "Bad custom URL template: $urlTemplate", e)
                return results
            }

        try {
            // IO dispatcher — never on the main thread (per Phase 6 hard rule).
            val items =
                withContext(Dispatchers.IO) {
                    val conn =
                        (target.openConnection() as HttpURLConnection).apply {
                            connectTimeout = timeoutMs
                            readTimeout = timeoutMs
                            requestMethod = "GET"
                        }
                    try {
                        if (conn.responseCode !in 200..299) {
                            Logger.w(TAG, "HTTP ${conn.responseCode} for $target")
                            return@withContext emptyList<String>()
                        }
                        val body = conn.inputStream.bufferedReader().readText()
                        parseDuckDuckGoStyle(body, maxCount)
                    } finally {
                        try {
                            conn.disconnect()
                        } catch (ignored: Exception) {}
                    }
                }
            results.networkItems = items
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Try/catch around every external query — search must not crash launcher.
            Logger.e(TAG, "Custom URL suggestion fetch failed", e)
        }
        return results
    }

    /**
     * DuckDuckGo-style: top-level JSONArray whose first element is the echoed query and second is
     * the suggestion array. We tolerate any shape that has at least one nested string array and
     * pull the first such array.
     */
    private fun parseDuckDuckGoStyle(body: String, max: Int): List<String> {
        return try {
            val parsed = JSONArray(body)
            val candidates = (0 until parsed.length()).map { parsed.opt(it) }
            val arr = candidates.firstOrNull { it is JSONArray } as? JSONArray ?: return emptyList()
            (0 until arr.length()).take(max).map { arr.optString(it) }.filter { it.isNotBlank() }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to parse custom suggestion response", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "CustomUrlProvider"
    }
}
