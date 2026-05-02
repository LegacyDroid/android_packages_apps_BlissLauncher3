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
package foundation.e.bliss.gestures;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.quickstep.SystemUiProxy;

/**
 * Base class for gesture action handlers. Each handler implements a single
 * action that can be triggered by a gesture (double-tap, swipe-down, etc.).
 *
 * Ported from Lawnchair's GestureHandler hierarchy, simplified to use
 * string-based handler names stored in LauncherPrefs instead of
 * kotlinx.serialization.
 */
public abstract class GestureHandler {

    private static final String TAG = "GestureHandler";
    private static final String SERVICE_STATUSBAR = "statusbar";

    public static final String HANDLER_NONE = "none";
    public static final String HANDLER_SLEEP = "sleep";
    public static final String HANDLER_NOTIFICATIONS = "notifications";
    public static final String HANDLER_QUICK_SETTINGS = "quick_settings";
    public static final String HANDLER_APP_DRAWER = "app_drawer";
    public static final String HANDLER_APP_SEARCH = "app_search";
    public static final String HANDLER_ASSISTANT = "assistant";
    public static final String HANDLER_RECENTS = "recents";
    public static final String HANDLER_APP_PREFIX = "app:";

    protected final Launcher mLauncher;

    protected GestureHandler(Launcher launcher) {
        mLauncher = launcher;
    }

    /** Execute the gesture action. Called on the main thread. */
    public abstract void onTrigger();

    /** Display name for settings UI. */
    public abstract String getLabel();

    // ---- Factory ----

    public static GestureHandler forName(String name, Launcher launcher) {
        if (name == null)
            name = HANDLER_NONE;
        if (name.startsWith(HANDLER_APP_PREFIX)) {
            return new AppLaunchHandler(launcher, name.substring(HANDLER_APP_PREFIX.length()));
        }
        switch (name) {
            case HANDLER_SLEEP :
                return new SleepHandler(launcher);
            case HANDLER_NOTIFICATIONS :
                return new NotificationsHandler(launcher);
            case HANDLER_QUICK_SETTINGS :
                return new QuickSettingsHandler(launcher);
            case HANDLER_APP_DRAWER :
                return new AppDrawerHandler(launcher);
            case HANDLER_APP_SEARCH :
                return new AppSearchHandler(launcher);
            case HANDLER_ASSISTANT :
                return new AssistantHandler(launcher);
            case HANDLER_RECENTS :
                return new RecentsHandler(launcher);
            case HANDLER_NONE :
            default :
                return new NoOpHandler(launcher);
        }
    }

    // ---- Handler implementations ----

    /** Does nothing. */
    public static class NoOpHandler extends GestureHandler {
        public NoOpHandler(Launcher launcher) {
            super(launcher);
        }

        @Override
        public void onTrigger() {
            /* no-op */ }

        @Override
        public String getLabel() {
            return "None";
        }
    }

    /**
     * Locks the screen via accessibility service (GLOBAL_ACTION_LOCK_SCREEN).
     * Requires Android P+ (API 28). Falls back to no-op on older versions.
     */
    public static class SleepHandler extends GestureHandler {
        public SleepHandler(Launcher launcher) {
            super(launcher);
        }

        @Override
        public void onTrigger() {
            try {
                android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager) mLauncher
                        .getSystemService(Context.DEVICE_POLICY_SERVICE);
                if (dpm != null) {
                    dpm.lockNow();
                }
            } catch (SecurityException e) {
                Log.w(TAG, "Sleep gesture: insufficient permissions — " + "device admin not enabled", e);
            }
        }

        @Override
        public String getLabel() {
            return "Sleep (lock screen)";
        }
    }

    /** Opens the notification panel via SystemUiProxy. */
    public static class NotificationsHandler extends GestureHandler {
        public NotificationsHandler(Launcher launcher) {
            super(launcher);
        }

        @Override
        public void onTrigger() {
            try {
                SystemUiProxy.INSTANCE.get(mLauncher).expandNotificationPanel();
            } catch (Exception e) {
                Log.w(TAG, "Failed to expand notification panel", e);
                // Fallback: try StatusBarManager reflection
                expandViaStatusBarManager();
            }
        }

        private void expandViaStatusBarManager() {
            try {
                Object sbm = mLauncher.getSystemService(SERVICE_STATUSBAR);
                if (sbm != null) {
                    java.lang.reflect.Method expand = sbm.getClass().getMethod("expandNotificationsPanel");
                    expand.invoke(sbm);
                }
            } catch (Exception e) {
                Log.w(TAG, "StatusBarManager fallback also failed", e);
            }
        }

        @Override
        public String getLabel() {
            return "Open notifications";
        }
    }

    /** Opens the quick settings panel via SystemUiProxy. */
    public static class QuickSettingsHandler extends GestureHandler {
        public QuickSettingsHandler(Launcher launcher) {
            super(launcher);
        }

        @Override
        public void onTrigger() {
            try {
                SystemUiProxy.INSTANCE.get(mLauncher).toggleQuickSettingsPanel();
            } catch (Exception e) {
                Log.w(TAG, "Failed to toggle quick settings", e);
                // Fallback: try StatusBarManager reflection
                try {
                    Object sbm = mLauncher.getSystemService(SERVICE_STATUSBAR);
                    if (sbm != null) {
                        java.lang.reflect.Method expand = sbm.getClass().getMethod("expandSettingsPanel");
                        expand.invoke(sbm);
                    }
                } catch (Exception ex) {
                    Log.w(TAG, "Quick settings fallback also failed", ex);
                }
            }
        }

        @Override
        public String getLabel() {
            return "Open quick settings";
        }
    }

    /** Opens the app drawer by transitioning to ALL_APPS state. */
    public static class AppDrawerHandler extends GestureHandler {
        public AppDrawerHandler(Launcher launcher) {
            super(launcher);
        }

        @Override
        public void onTrigger() {
            mLauncher.getStateManager().goToState(LauncherState.ALL_APPS);
        }

        @Override
        public String getLabel() {
            return "Open app drawer";
        }
    }

    /** Opens the app search (drawer with search focused). */
    public static class AppSearchHandler extends GestureHandler {
        public AppSearchHandler(Launcher launcher) {
            super(launcher);
        }

        @Override
        public void onTrigger() {
            mLauncher.getStateManager().goToState(LauncherState.ALL_APPS);
            // Focus search after transition
            mLauncher.getAppsView().postDelayed(() -> {
                android.widget.EditText editText = mLauncher.getAppsView().getSearchUiManager().getEditText();
                if (editText != null) {
                    editText.requestFocus();
                }
            }, 300);
        }

        @Override
        public String getLabel() {
            return "Open app search";
        }
    }

    /** Launches a specific app by its component name. */
    public static class AppLaunchHandler extends GestureHandler {
        private final String mComponentString;

        public AppLaunchHandler(Launcher launcher, String componentString) {
            super(launcher);
            mComponentString = componentString;
        }

        @Override
        public void onTrigger() {
            try {
                ComponentName cn = ComponentName.unflattenFromString(mComponentString);
                if (cn == null) {
                    Log.w(TAG, "Invalid component: " + mComponentString);
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(cn);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                mLauncher.startActivity(intent);
            } catch (Exception e) {
                Log.w(TAG, "Failed to launch app: " + mComponentString, e);
            }
        }

        @Override
        public String getLabel() {
            return "Launch app";
        }
    }

    /** Opens the recent apps screen. */
    public static class RecentsHandler extends GestureHandler {
        public RecentsHandler(Launcher launcher) {
            super(launcher);
        }

        @Override
        public void onTrigger() {
            try {
                // Use GLOBAL_ACTION_RECENTS via accessibility
                android.accessibilityservice.AccessibilityService service = null;
                // Fallback: try StatusBarManager to toggle recents
                Object sbm = mLauncher.getSystemService(SERVICE_STATUSBAR);
                if (sbm != null) {
                    java.lang.reflect.Method toggle = sbm.getClass().getMethod("toggleRecentApps");
                    toggle.invoke(sbm);
                    return;
                }
            } catch (Exception e) {
                Log.w(TAG, "toggleRecentApps failed, trying intent", e);
            }
            try {
                // Final fallback: send recents intent
                Intent intent = new Intent("com.android.systemui.recents.TOGGLE_RECENTS");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mLauncher.sendBroadcast(intent);
            } catch (Exception e) {
                Log.w(TAG, "Failed to open recents", e);
            }
        }

        @Override
        public String getLabel() {
            return "Open recents";
        }
    }

    /** Launches the device assistant (Google Assistant or equivalent). */
    public static class AssistantHandler extends GestureHandler {
        public AssistantHandler(Launcher launcher) {
            super(launcher);
        }

        @Override
        public void onTrigger() {
            try {
                Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (intent.resolveActivity(mLauncher.getPackageManager()) != null) {
                    mLauncher.startActivity(intent);
                    return;
                }
                // Fallback to ASSIST action
                Intent assistIntent = new Intent(Intent.ACTION_ASSIST);
                assistIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (assistIntent.resolveActivity(mLauncher.getPackageManager()) != null) {
                    mLauncher.startActivity(assistIntent);
                    return;
                }
                // Final fallback: search
                Intent searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
                searchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mLauncher.startActivity(searchIntent);
            } catch (Exception e) {
                Log.w(TAG, "Failed to launch assistant", e);
            }
        }

        @Override
        public String getLabel() {
            return "Open assistant";
        }
    }
}
