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
package foundation.e.bliss.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import kotlin.Exception

fun Context.toggleKeyboard(view: View, hasFocus: Boolean) {
    val inputMethodManager = getSystemService(InputMethodManager::class.java)
    if (hasFocus) {
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    } else {
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

fun <T> resourcesToMap(array: List<T>): Map<T, T> {
    val map = mutableMapOf<T, T>()

    if (array.size.mod(2) == 0) {
        for (i in array.indices step 2) {
            map[array[i]] = array[i + 1]
        }
    } else {
        throw Exception("Failed to parse array resource")
    }

    return map
}
