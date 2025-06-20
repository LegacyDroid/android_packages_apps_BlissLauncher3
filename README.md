# BlissLauncher3

**BlissLauncher3** is a modified version of the AOSP [Launcher3](https://android.googlesource.com/platform/packages/apps/Launcher3), with features inspired from [BlissLauncher v1](https://gitlab.e.foundation/e/os/BlissLauncher/).

It is the default launcher for [/e/OS](https://e.foundation/e-os/).

## Building

- Clone the repository

  ```git
  git clone --recurse-submodules https://gitlab.e.foundation/e/os/BlissLauncher3 -b a15 BlissLauncher3
  ```

- To build this project, we need the following jars inside the `libs` folder from our AOSP build directory:
- We need to build the `TrebuchetQuickStep` module using `m TrebuchetQuickStep` and then copy the jars from the following directories:

  - **framework.jar**: `out/soong/.intermediates/frameworks/base/framework/android_common/turbine-combined/framework.jar`

  - **classes.jar**: `out/target/common/obj/JAVA_LIBRARIES/Launcher3QuickStepLib_intermediates/classes.jar`

  - **android.os.flags-aconfig-java.jar**: `out/soong/.intermediates/frameworks/base/android.os.flags-aconfig-java/android_common/javac/android.os.flags-aconfig-java.jar`

  - **com.android.window.flags.window-aconfig-java.jar**: `out/soong/.intermediates/frameworks/base/com.android.window.flags.window-aconfig-java/android_common/javac/com.android.window.flags.window-aconfig-java.jar`

  - **com_android_launcher3_flags_lib.jar**: `out/soong/.intermediates/packages/apps/Trebuchet/aconfig/com_android_launcher3_flags_lib/android_common/javac/com_android_launcher3_flags_lib.jar`

- Launch Android Studio and Import the project

- Build the project through the IDE or run the following command:

  ```bash
  ./gradlew assembleBlissWithQuickstepDebug
  ```

## Installing

- Below conditions are required to install the app:

  - User should be on **Android 15** /e/OS or LineageOS
  - Rom should be signed with **test keys**

- Download and install the APK like any other normal app from the [pipeline](https://gitlab.e.foundation/e/os/BlissLauncher/-/pipelines/latest?ref=a15)

- Download and install the icon mask like normal app [SquircleMask.apk](https://gitlab.e.foundation/internal/wiki/-/wikis/uploads/320461a58f097993b29772abe0d2b0b9/KGLN4.apk)

- Go to _Settings > Apps > Default apps > Launcher_ and change launcher to `Blisslauncher` (with green icon)

- It will open a page about `Usage access`. Allow the new BlissLauncher (`Permit usage access`)

- Go to Settings > Display > Icon Shape > Select **Squircle**

- Clear the data of Blisslauncher3 manually through settings or run the command through adb:

  ```bash
  adb shell pm clear foundation.e.blisslauncher
  ```

- Reboot

- Now it is totally ready to use and play around with!
