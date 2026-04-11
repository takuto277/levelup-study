This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./shared/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

### Build and Run Android Application

#### Android SDK path (Gradle)

Gradle needs `apps/mobile/local.properties` with `sdk.dir=...`. If the file is missing, copy `local.properties.example` to `local.properties` and set `sdk.dir` to your SDK path (see below).

#### Android SDK path (AVD / Device Manager)

Creating an emulator can fail with:

> **Can't locate Android SDK installation directory for the AVD .ini file.**

That means **Android Studio does not know the SDK location** (separate from Gradle’s `local.properties`). Fix it in the IDE:

1. **Android Studio** → **Settings** (macOS: **Android Studio** → **Settings**) → **Languages & Frameworks** → **Android SDK**.
2. Set **Android SDK Location** to a valid folder (default on macOS: `~/Library/Android/sdk`). If the folder is empty, use **SDK Manager** to install **Android SDK Platform** and a **System Image** for your emulator.
3. Click **Apply** / **OK**, then **restart Android Studio** and create the AVD again.

For command-line tools (`sdkmanager`, `avdmanager`), set in your shell profile:

```shell
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
```

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

#### KMP + Android Gradle Plugin (yellow `w:` lines)

You may see warnings such as:

- `The 'org.jetbrains.kotlin.multiplatform' plugin deprecated compatibility with Android Gradle plugin: 'com.android.application'`
- `... 'com.android.library'`

These are **forward-compatibility notices for AGP 9.0+**. With **AGP 8.x** (this repo), `./gradlew :composeApp:assembleDebug` should still end with **`BUILD SUCCESSFUL`**. They are **not** the same as a failed build.

If something actually fails, look for **`BUILD FAILED`**, **`e:`** errors, or the first **red** stack trace—not these yellow warnings.

**Future work (AGP 9+):** migrate `:shared` to [`com.android.kotlin.multiplatform.library`](https://developer.android.com/kotlin/multiplatform/plugin) and split `:composeApp` so the Android **application** plugin lives in a small dedicated module; see [KMP + AGP 9 migration](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html).

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…