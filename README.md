# BlissLauncher3

**BlissLauncher3** is a customized version of [AOSP Launcher3](https://android.googlesource.com/platform/packages/apps/Launcher3), enhanced with features inspired by [BlissLauncher v1](https://gitlab.e.foundation/e/os/BlissLauncher/).

It serves as the default launcher for [/e/OS](https://e.foundation/e-os/).

---

## Building

### 1. Clone the Repository

```bash
git clone https://gitlab.e.foundation/e/os/BlissLauncher3 -b a16 BlissLauncher3
cd BlissLauncher3
git submodule update --init --recursive
```

### 2. Prepare Required JARs

To build this project, the following JAR files are needed inside the `libs` folder.
These can be obtained from your AOSP build directory after building the `Launcher3QuickStep` module:

Run:

```bash
m Launcher3QuickStep Launcher3QuickStepLib
```

Then copy the following JARs:

| JAR File                                             | Path                                                                                                                                                          |
| ---------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **framework.jar**                                    | `out/soong/.intermediates/frameworks/base/framework/android_common/turbine-combined/framework.jar`                                                            |
| **classes.jar**                                      | `out/target/common/obj/JAVA_LIBRARIES/Launcher3QuickStepLib_intermediates/classes.jar`                                                                        |
| **android.os.flags-aconfig-java.jar**                | `out/soong/.intermediates/frameworks/base/android.os.flags-aconfig-java/android_common/javac/android.os.flags-aconfig-java.jar`                               |
| **com.android.window.flags.window-aconfig-java.jar** | `out/soong/.intermediates/frameworks/base/com.android.window.flags.window-aconfig-java/android_common/javac/com.android.window.flags.window-aconfig-java.jar` |
| **com_android_launcher3_flags_lib.jar**              | `out/soong/.intermediates/packages/apps/Launcher3/aconfig/com_android_launcher3_flags_lib/android_common/javac/com_android_launcher3_flags_lib.jar`           |

> **Note:** Required libraries may already be included in the repository.

### 3. Build the Project

* Open the project in **Android Studio**, or
* Build via command line:

```bash
./gradlew assembleBlissWithQuickstepDebug
```

---

## Installing

### Requirements

* Device must be running **Android 16** (or equivalent /e/OS or LineageOS build).
* ROM must be **signed with test keys**.

### Steps

1. **Download & Install the APK**

   Get the latest build from the [pipeline](https://gitlab.e.foundation/e/os/BlissLauncher/-/pipelines/latest?ref=a16).

2. **Install the Icon Mask (Optional)**
   Download and install [SquircleMask.apk](https://gitlab.e.foundation/internal/wiki/-/wikis/uploads/320461a58f097993b29772abe0d2b0b9/KGLN4.apk).

3. **Set BlissLauncher as Default**

   * Go to: `Settings > Apps > Default apps > Launcher`
   * Choose **BlissLauncher** (green icon)

4. **Grant Usage Access**

   * When prompted, open the **Usage access** settings page.
   * Enable **Permit usage access** for BlissLauncher.

5. **Change Icon Shape**

   * Navigate to: `Settings > Display > Icon Shape`
   * Select **Squircle**

6. **Clear BlissLauncher Data**

   * Via settings: Clear app data manually,
     **or** use ADB:

     ```bash
     adb shell pm clear foundation.e.blisslauncher
     ```

7. **Reboot the Device**

After reboot, **BlissLauncher3** will be fully functional and ready to use.

