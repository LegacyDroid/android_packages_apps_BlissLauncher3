-keep,allowshrinking,allowoptimization class foundation.e.bliss$* { *; }

-keep class com.android.systemui.plugins.** { *; }
-keep interface com.android.systemui.plugins.** { *; }

-dontwarn android.compat.annotation.UnsupportedAppUsage
-dontwarn com.android.aconfig.annotations.AconfigFlagAccessor
-dontwarn com.android.aconfig.annotations.AssumeFalseForR8
-dontwarn com.android.aconfig.annotations.AssumeTrueForR8
