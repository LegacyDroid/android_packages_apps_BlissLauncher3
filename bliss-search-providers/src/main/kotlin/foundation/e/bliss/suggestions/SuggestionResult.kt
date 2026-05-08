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
 * File:    bliss-search-providers/src/main/kotlin/foundation/e/bliss/suggestions/SuggestionResult.kt
 * Module:  :bliss-search-providers
 *
 * Public model:
 *   Mutable result object used by launcher search UI after a provider query.
 *   Keep it free of Android UI/model classes so provider tests can construct
 *   it on a plain unit-test classpath.
 */
package foundation.e.bliss.suggestions

class SuggestionsResult(var queryText: String) {
    var networkItems: List<String?> = emptyList()
}
