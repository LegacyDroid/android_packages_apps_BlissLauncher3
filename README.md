# BlissLauncher3 (LegacyDroid)

**BlissLauncher3** is a customized fork of the AOSP [Launcher3](https://android.googlesource.com/platform/packages/apps/Launcher3), derived from [BlissLauncher v1](https://gitlab.e.foundation/e/os/BlissLauncher/-/tree/master).

This is the default launcher for the **LegacyDroid** custom Android ROM project (based on LineageOS 21).

## Building

- Clone the repository

  ```git
  git clone --recurse-submodules https://github.com/LegacyDroid/android_packages_apps_BlissLauncher3 BlissLauncher3
  ```

- To build this project, we need the following jars inside the `prebuilts` folder from your AOSP build directory:

  - **framework-14.jar**: `out/soong/.intermediates/frameworks/base/framework/android_common/turbine-combined/framework.jar`

  - We need to build the `Launcher3QuickStepLib` module using `m Launcher3QuickStepLib` and then copy the jar from the following directory:

    - **classes.jar**: `out/target/common/obj/JAVA_LIBRARIES/Launcher3QuickStepLib_intermediates/classes.jar`

- Now we need to run two tasks to generate the proper jars:

  ```bash
  # Unzip the classes.jar file
  ./gradlew unzipJar

  # Generate the final jar for Quickstep
  ./gradlew makeReleaseJar
  ```

- Launch Android Studio and Import the project

- Build the project through the IDE or run the following command:

  ```bash
  ./gradlew assembleBlissWithQuickstepDebug
  ```

## Installing

- Below conditions are required to install the app:

  - User should be on **Android 14** LegacyDroid or compatible LineageOS-based ROM
  - ROM should be signed with **test keys**

- Download and install the APK like any other normal app

- Download and install the icon mask (SquircleMask.apk) like a normal app

- Go to _Settings > Apps > Default apps > Launcher_ and change launcher to `BlissLauncher3` (with green icon)

- It will open a page about `Usage access`. Allow the new BlissLauncher3 (`Permit usage access`)

- Go to Settings > Display > Icon Shape > Select **Squircle**

- Clear the data of BlissLauncher3 manually through settings or run the command through adb:

  ```bash
  adb shell pm clear com.android.launcher3
  ```

- Reboot

- Now it is totally ready to use and play around with!

## License

BlissLauncher3 combines code under two licenses:

* **Our original code** (files we wrote from scratch for LegacyDroid) is licensed under the **Apache License 2.0**.
* **BlissLauncher itself** and all our modifications to it fall under the **GNU General Public License v3.0 (GPLv3)**.

### What this means for you:
* **Source files:** If you're pulling individual standalone files we authored, you can use them under Apache 2.0.
* **Building / Distributing the APK:** Because BlissLauncher is GPLv3, any compiled build or modified launcher app as a whole is covered by **GPLv3**.

---

**Upstream Credits:**  
- BlissLauncher: [e.foundation](https://gitlab.e.foundation/e/os/BlissLauncher)
- Launcher3: [AOSP](https://android.googlesource.com/platform/packages/apps/Launcher3)
