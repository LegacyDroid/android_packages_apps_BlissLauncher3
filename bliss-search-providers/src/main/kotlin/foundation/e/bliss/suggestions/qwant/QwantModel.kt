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
 * File:    bliss-search-providers/src/main/kotlin/foundation/e/bliss/suggestions/qwant/QwantModel.kt
 * Module:  :bliss-search-providers
 *
 * Wire model:
 *   Kotlin serialization DTOs for Qwant suggestion responses. Provider code
 *   maps these service-shaped objects into plain suggestion strings before
 *   returning to launcher UI.
 */
package foundation.e.bliss.suggestions.qwant

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class QwantData(
    @SerialName("items") val items: List<QwantItem> = emptyList(),
    @SerialName("special") val special: List<JsonElement> = emptyList(),
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class QwantItem(
    @SerialName("value") val value: String? = null,
    @SerialName("suggestType") val suggestType: Int? = null,
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class QwantResult(
    @SerialName("status") val status: String? = null,
    @SerialName("data") val data: QwantData? = null,
)
