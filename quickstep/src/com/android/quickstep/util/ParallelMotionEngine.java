/*
 * Copyright (C) 2026 The LegacyDroid Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.quickstep.util;

import android.os.SystemClock;
import android.view.animation.Interpolator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistent registry of spring nodes, one per task, shared by every transition. Signals flip
 * targets instead of queueing: a close during an open reverses the same node keeping its
 * momentum, an open closes every other visible node in parallel, and stacking follows scale
 * priority so the window closest to fullscreen draws on top. Spring: stiffness 320, damping 32,
 * mass 1; launcher content and depth reuse the same profile through {@link #spring(long)}.
 */
public class ParallelMotionEngine {

    private static final ParallelMotionEngine INSTANCE = new ParallelMotionEngine();

    public static final float STIFFNESS = 320f;
    public static final float DAMPING = 32f;

    private static final float MAX_FRAME_DT = 0.032f;
    private static final float POSITION_EPSILON = 0.003f;
    private static final float VELOCITY_EPSILON = 0.02f;

    public static ParallelMotionEngine get() {
        return INSTANCE;
    }

    /** Spring progress of one task's window: 0 is the icon, 1 is fullscreen. */
    public static class Node {
        public final int taskId;
        public float progress;

        float velocity;
        float target;
        boolean active;
        private float mCloseFrom = -1f;

        Node(int taskId) {
            this.taskId = taskId;
        }

        boolean visible() {
            return target > 0f || progress > 0f;
        }

        private void moveTo(float target) {
            this.target = target;
            active = true;
        }

        private void step(float dt) {
            if (!active) {
                return;
            }
            // Semi-implicit Euler on -k(x - target) - cv.
            float accel = -STIFFNESS * (progress - target) - DAMPING * velocity;
            velocity += accel * dt;
            progress += velocity * dt;
            if (Math.abs(progress - target) < POSITION_EPSILON
                    && Math.abs(velocity) < VELOCITY_EPSILON) {
                progress = target;
                velocity = 0;
                active = false;
            }
        }
    }

    private final Map<Integer, Node> mNodes = new LinkedHashMap<>();
    private long mLastFrameTime = -1;

    public Node node(int taskId) {
        Node node = mNodes.get(taskId);
        if (node == null) {
            node = new Node(taskId);
            mNodes.put(taskId, node);
        }
        return node;
    }

    /**
     * Close-side lookup: transitions can promote the task id between open and close, so when the
     * exact id carries no progress of its own, fall back to the node still opening.
     */
    public Node resolve(int taskId) {
        Node node = mNodes.get(taskId);
        if (node != null && node.progress > 0f) {
            return node;
        }
        Node candidate = null;
        for (Node other : mNodes.values()) {
            if (!other.visible() || other.target < 1f) {
                continue;
            }
            if (other.active) {
                return other;
            }
            candidate = other;
        }
        return candidate != null ? candidate : node(taskId);
    }

    /** Windows currently at least partly on screen. */
    public int visibleNodeCount() {
        int count = 0;
        for (Node node : mNodes.values()) {
            if (node.visible()) {
                count++;
            }
        }
        return count;
    }

    /** Opens each task and reverses every other visible node back toward its icon. */
    public void signalOpen(int... taskIds) {
        mLastFrameTime = -1;
        for (Node node : mNodes.values()) {
            if (node.visible() && !contains(taskIds, node.taskId)) {
                node.moveTo(0f);
            }
        }
        for (int taskId : taskIds) {
            node(taskId).moveTo(1f);
        }
    }

    /**
     * Hands the node to the incoming close transition: progress freezes at the handoff point
     * and the node stops integrating, since the close animator now drives it.
     */
    public void signalClose(int taskId) {
        mLastFrameTime = -1;
        Node node = resolve(taskId);
        node.target = 0f;
        node.active = false;
        node.velocity = 0f;
        node.mCloseFrom = Math.max(0f, node.progress);
    }

    /** Mirrors the close spring's travel (0 fullscreen, 1 icon) into the node each frame. */
    public void sync(int taskId, float closeTravel) {
        Node node = resolve(taskId);
        if (node.target >= 1f) {
            // An open signal landed after this close started; that signal wins.
            return;
        }
        if (node.mCloseFrom < 0f) {
            node.mCloseFrom = Math.max(0f, node.progress);
        }
        node.progress = node.mCloseFrom * (1f - closeTravel);
        node.target = 0f;
        node.active = false;
        node.velocity = 0f;
    }

    /**
     * Turns every visible node still heading toward fullscreen back toward its icon,
     * carrying velocity so the window reverses with the momentum it built. Nodes frozen
     * by a close (target 0) stay with their close animator.
     */
    public void reverseOpening() {
        mLastFrameTime = -1;
        for (Node node : mNodes.values()) {
            if (node.visible() && node.target > 0f) {
                node.moveTo(0f);
            }
        }
    }

    /** True when no node is integrating, i.e. every spring has settled on its target. */
    public boolean atRest() {
        for (Node node : mNodes.values()) {
            if (node.active) {
                return false;
            }
        }
        return true;
    }

    /**
     * Advances every node; call once per frame. Uptime millis is safe from animator callbacks
     * and from synchronous paths like RectFSpringAnim.end(), where Choreographer is not in a
     * frame and getFrameTime() would throw.
     */
    public void step() {
        long frameTime = SystemClock.uptimeMillis();
        float dt = mLastFrameTime < 0 ? 0f
                : Math.min((frameTime - mLastFrameTime) / 1000f, MAX_FRAME_DT);
        mLastFrameTime = frameTime;
        for (Node node : mNodes.values()) {
            node.step(dt);
        }
    }

    /**
     * Scale priority: z rises with progress so the window nearer fullscreen draws on top.
     * Creation order breaks ties.
     */
    public int stackZ(Node node) {
        int idx = 0;
        boolean seenSelf = false;
        for (Node other : mNodes.values()) {
            if (other == node) {
                seenSelf = true;
                continue;
            }
            if (!other.visible()) {
                continue;
            }
            if (other.progress < node.progress
                    || (other.progress == node.progress && !seenSelf)) {
                idx++;
            }
        }
        return 50 + Math.round(node.progress * 50) + idx;
    }

    /** Layer above every stacked window (base 50 + progress 50 + rank). */
    public int stackZTop() {
        return 101 + mNodes.size();
    }

    /**
     * Analytic solution of the node spring over {@code durationMillis}, for animators that
     * must stay in lockstep with the nodes.
     */
    public static Interpolator spring(long durationMillis) {
        final float durationSeconds = durationMillis / 1000f;
        final float decayRate = DAMPING / 2f;
        final float angular = (float) Math.sqrt(STIFFNESS - decayRate * decayRate);
        return fraction -> {
            float t = fraction * durationSeconds;
            float envelope = (float) Math.exp(-decayRate * t);
            return 1f - envelope * ((float) Math.cos(angular * t)
                    + decayRate / angular * (float) Math.sin(angular * t));
        };
    }

    private static boolean contains(int[] taskIds, int taskId) {
        for (int id : taskIds) {
            if (id == taskId) {
                return true;
            }
        }
        return false;
    }
}
