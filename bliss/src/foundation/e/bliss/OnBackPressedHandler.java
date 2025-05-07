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
package foundation.e.bliss;

import androidx.annotation.FloatRange;

/**
 * Interface that mimics {@link android.window.OnBackInvokedCallback} without
 * dependencies on U's API such as {@link android.window.BackEvent}.
 *
 * <p>
 * Impl can assume below order during a back gesture:
 * <ol>
 * <li>[optional] one {@link #onBackStarted()} will be called to start the
 * gesture
 * <li>zero or multiple {@link #onBackProgressed(float)} will be called during
 * swipe gesture
 * <li>either one of {@link #onBackInvoked()} or {@link #onBackCancelled()} will
 * be called to end the gesture
 */
public interface OnBackPressedHandler {

    /** Called when back has started. */
    default void onBackStarted() {
    }

    /** Called when back is committed. */
    void onBackInvoked();

    /** Called with back gesture's progress. */
    default void onBackProgressed(@FloatRange(from = 0.0, to = 1.0) float backProgress) {
    }

    /** Called when user drops the back gesture. */
    default void onBackCancelled() {
    }
}
