# Migration04 §09: :bliss-compat consumer rules — Plans/Migration04/09-build-and-rollout.md §3
# Source of truth for these keep-rules is Plans/Migration04/01-compat-platform.md §5.
#
# Reflective targets — any class the Compat layer probes via Class.forName.
# These are framework classes; R8 wouldn't strip them (they're not in our
# compiled output), but rename obfuscation of the names embedded in our
# string literals would defeat the lookup. -keepnames freezes the string-
# equal name across configurations.
-keepnames class android.multiuser.Flags { *; }
-keepnames class android.window.DesktopModeFlags { *; }
-keepnames class android.window.DesktopExperienceFlags { *; }
-keepnames class com.android.window.flags.Flags { *; }
-keepnames class lineageos.providers.LineageSettings { *; }
-keepnames class lineageos.providers.LineageSettings$System { *; }
-keepnames class android.app.ActivityTaskManager { *; }

# Compat module — our wrappers. Strip-resistant; release builds inline these.
-keep,allowshrinking,allowoptimization class foundation.e.bliss.compat.** { *; }
