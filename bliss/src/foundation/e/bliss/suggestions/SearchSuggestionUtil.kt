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
 * File:    bliss/src/foundation/e/bliss/suggestions/SearchSuggestionUtil.kt
 * Module:  bliss root app source-set
 *
 * Structure:
 *   - App source owns provider preference selection, Browser default-engine
 *     lookup, and custom URL preference gating.
 *   - :bliss-search-providers owns built-in Qwant/DuckDuckGo transport and
 *     JSON parsing.
 *
 * Usage:
 *   BlissInput calls getSuggestionProvider(context) for autocomplete queries
 *   and getUriForQuery(context, query) when launching the selected search
 *   engine in a browser.
 */
package foundation.e.bliss.suggestions

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.dagger.LauncherComponentProvider
import foundation.e.bliss.adapters.LauncherDebugFlags
import foundation.e.bliss.suggestions.duckduckgo.DuckDuckGoProvider
import foundation.e.bliss.suggestions.qwant.QwantProvider
import foundation.e.bliss.utils.Logger

object SearchSuggestionUtil {
    fun getSuggestionProvider(context: Context): SuggestionProvider {
        // A user-configured custom URL takes precedence over the built-in
        // providers. Empty / non-`%s` URLs fall through.
        try {
            val prefs = LauncherComponentProvider.get(context).launcherPrefs
            val customUrl = prefs.get(LauncherPrefs.WEB_SUGGESTION_URL)
            if (customUrl.isNotEmpty() && customUrl.contains("%s")) {
                return CustomUrlProvider(context)
            }
        } catch (e: Exception) {
            // Pref subsystem unavailable — fall through to defaults.
        }

        val userPref = getUserSearchProvider(context)
        if (userPref != "default") {
            return when (userPref) {
                "qwant" -> QwantProvider(Logger, providerHttpClient)
                "duckduckgo" -> DuckDuckGoProvider(Logger, providerHttpClient)
                else -> DuckDuckGoProvider(Logger, providerHttpClient)
            }
        }
        return with(defaultSearchEngine(context)) {
            when {
                contains(Providers.QWANT.key, true) || contains(Providers.MURENASEARCH.key, true) ->
                    QwantProvider(Logger, providerHttpClient)
                else -> DuckDuckGoProvider(Logger, providerHttpClient)
            }
        }
    }

    fun getUriForQuery(context: Context, query: String): Uri {
        val encoded = Uri.encode(query)
        val userPref = getUserSearchProvider(context)
        if (userPref != "default") {
            return when (userPref) {
                "duckduckgo" -> "${Providers.DUCKDUCKGO.url}?q=$encoded"
                "qwant" -> "${Providers.QWANT.url}?q=$encoded"
                "murena" -> "${Providers.MURENASEARCH.url}?q=$encoded"
                "mojeek" -> "${Providers.MOJEEK.url}search?q=$encoded"
                "spot" -> "${Providers.SPOT.url}?q=$encoded"
                "ecosia" -> "${Providers.ECOSIA.url}search?q=$encoded"
                "startpage" -> "${Providers.STARTPAGE.url}sp/search?query=$encoded"
                "brave" -> "${Providers.BRAVE.url}search?q=$encoded"
                else -> "${Providers.DUCKDUCKGO.url}?q=$encoded"
            }.toUri()
        }

        val defaultSearchEngine = defaultSearchEngine(context)

        return with(defaultSearchEngine) {
            when {
                contains(Providers.MURENASEARCH.key, true) ->
                    "${Providers.MURENASEARCH.url}?q=$encoded"
                contains(Providers.QWANT.key, true) -> "${Providers.QWANT.url}?q=$encoded"
                contains(Providers.DUCKDUCKGO.key, true) -> "${Providers.DUCKDUCKGO.url}?q=$encoded"
                contains(Providers.MOJEEK.key, true) -> "${Providers.MOJEEK.url}search?q=$encoded"
                contains(Providers.SPOT.key, true) -> "${Providers.SPOT.url}?q=$encoded"
                else -> "${Providers.MURENASEARCH.url}?q=$encoded"
            }.toUri()
        }
    }

    private fun getUserSearchProvider(context: Context): String {
        return try {
            LauncherComponentProvider.get(context).launcherPrefs.get(LauncherPrefs.SEARCH_PROVIDER)
        } catch (e: Exception) {
            "default"
        }
    }

    private fun defaultSearchEngine(context: Context): String {
        val contentResolver = context.contentResolver
        val uri =
            Uri.parse("content://foundation.e.browser.provider")
                .buildUpon()
                .appendPath("search_engine")
                .build()

        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor.use {
            return if (it != null && it.moveToFirst()) {
                it.getString(0)
            } else {
                ""
            }
        }
    }

    private val providerHttpClient by lazy {
        BaseSuggestionProvider.defaultHttpClient(LauncherDebugFlags.isDebugDevice)
    }
}
