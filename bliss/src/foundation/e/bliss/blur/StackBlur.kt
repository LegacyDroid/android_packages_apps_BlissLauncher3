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
package foundation.e.bliss.blur

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

object StackBlur {
    fun blur(src: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return src

        val bitmap = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val div = radius * 2 + 1
        val wh = w * h

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        val vmin = IntArray(max(w, h))
        val dv = IntArray(256 * div) { it / div }

        var yi = 0

        for (y in 0 until h) {
            var rsum = 0
            var gsum = 0
            var bsum = 0

            for (i in -radius..radius) {
                val p = pix[yi + min(wm, max(i, 0))]
                rsum += (p shr 16) and 0xFF
                gsum += (p shr 8) and 0xFF
                bsum += p and 0xFF
            }

            for (x in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                if (y == 0) vmin[x] = min(x + radius + 1, wm)
                val p1 = pix[y * w + vmin[x]]
                val p2 = pix[y * w + max(x - radius, 0)]

                rsum += ((p1 shr 16) and 0xFF) - ((p2 shr 16) and 0xFF)
                gsum += ((p1 shr 8) and 0xFF) - ((p2 shr 8) and 0xFF)
                bsum += (p1 and 0xFF) - (p2 and 0xFF)

                yi++
            }
        }

        for (x in 0 until w) {
            var rsum = 0
            var gsum = 0
            var bsum = 0
            var yp = -radius * w

            for (i in -radius..radius) {
                val yIndex = max(0, yp) + x
                rsum += r[yIndex]
                gsum += g[yIndex]
                bsum += b[yIndex]
                yp += w
            }

            yi = x
            for (y in 0 until h) {
                pix[yi] = (0xFF shl 24) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                if (x == 0) vmin[y] = min(y + radius + 1, hm) * w
                val p1 = x + vmin[y]
                val p2 = x + max(y - radius, 0) * w

                rsum += r[p1] - r[p2]
                gsum += g[p1] - g[p2]
                bsum += b[p1] - b[p2]

                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}
