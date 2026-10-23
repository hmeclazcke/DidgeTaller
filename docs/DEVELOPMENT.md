# Development

Use the [README](../README.md) for cloning, Android Studio setup, and the first debug run. This document collects the developer checks and repository conventions.

## Toolchain

The checked-in build uses JDK target 17, Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin 2.2.21, and Android SDK Platform 35. The app requires Android API 26 or later. Configure Android Studio's **Gradle JDK** and the command-line `JAVA_HOME` consistently; JDK 17 is the documented baseline.

SDK Manager provides Platform 35, platform tools, and build tools. [CI](../.github/workflows/android.yml) explicitly installs Build Tools 35.0.0. Let Android Studio create the local SDK configuration; `local.properties` is machine-specific and ignored by Git.

The optional SWIPE′ comparison also builds a small C/JNI library using **NDK
27.0.12077973** and **CMake 3.22.1**. Install these pinned versions through SDK
Manager, or let Gradle obtain them after the Android SDK licenses are accepted.
CI installs both explicitly. No Python installation is needed to build or run
the app. See [SWIPE′ comparison](SWIPE_COMPARISON.md) for source notices and
numerical-validation instructions.

Use the checked-in wrapper rather than an independently installed Gradle:

```powershell
.\gradlew.bat --version
```

On macOS/Linux, use `sh ./gradlew --version`. The initial run needs network access. If the wrapper JAR is absent, the Windows wrapper invokes [setup-wrapper.ps1](../scripts/setup-wrapper.ps1); the Unix script downloads it with `curl`. Both bootstrap paths verify the expected SHA-256. The bootstrap JAR is version 8.11.1; [wrapper properties](../gradle/wrapper/gradle-wrapper.properties) select the actual Gradle 8.13 distribution. Do not change versions to resolve a missing JAR.

## Build and JVM checks

Run from the repository root. These tasks do not install anything on a device:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleDebugAndroidTest
```

The first task runs JVM tests. The next two build the application and instrumented-test APKs. Replace `.\gradlew.bat` with `sh ./gradlew` on macOS/Linux.

| Output | Location |
| --- | --- |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| JVM test report | `app/build/reports/tests/testDebugUnitTest/index.html` |
| JVM test XML | `app/build/test-results/testDebugUnitTest/` |
| Android test sources | [app/src/androidTest](../app/src/androidTest) |
| JVM test sources | [app/src/test](../app/src/test) |

Optional real-audio regressions require private fixtures and are not prerequisites for building or running the ordinary JVM suite. Do not commit recordings, fixture manifests, or diagnostic output.

## Localization and quality reports

Python 3 is needed for the repository's standard-library validation and report scripts, not for the Android app build.

```powershell
python tools/check_localization.py
.\gradlew.bat :app:jacocoDebugUnitTestReport
.\gradlew.bat detekt detektComplexity
python tools/detekt_summary.py
python tools/coverage_summary.py
```

The coverage task depends on JVM tests. Run both Detekt tasks before its summary; the coverage summary uses the coverage and Detekt outputs for reviewed method comparisons.

- JaCoCo HTML: `app/build/reports/jacoco/debug/html/index.html`; XML: `app/build/reports/jacoco/debug/coverage.xml`.
- Detekt and complexity reports: `build/reports/detekt/`.
- Detekt findings and coverage are **informational**, without enforced thresholds. Detekt is syntax-only; JVM coverage does not exercise all Android callbacks or Compose paths.
- [Localization](LOCALIZATION.md) explains resource parity and presentation boundaries. [Detekt](DETEKT_AUDIT.md) and [coverage](COVERAGE_AUDIT.md) audits preserve dated observations and interpretation limits.

## Android tests and device data

Run instrumentation only on a verified emulator. The repository's tests include hardware guards and isolated fixtures; these do not authorize testing on a personal phone. Do not run an unqualified `connectedDebugAndroidTest` task with physical devices attached.

After verifying the emulator serial and installing compatible application/test APKs on that emulator, a focused invocation has this form:

```powershell
adb -s emulator-5554 shell am instrument -w -r -e class ar.didgetaller.ui.SessionNavigationTest ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner
```

The `-s` selects the emulator, `-w -r` waits and emits instrumentation results, and `-e class` selects a test class. Verify the actual emulator serial before using the command. CI compiles these tests but does not execute them.

For a device containing recordings, use only a compatible update with the same package/signing identity after a fresh verified backup. Never uninstall, clear data, replace the signing identity, or restore over existing recordings as troubleshooting. [Data safety](DATA_SAFETY.md) retains the detailed protocol and historical evidence.

## Making changes

Read [AGENTS.md](../AGENTS.md), [product context](PRODUCT_CONTEXT.md), [roadmap](ROADMAP.md), and the current [validation record](VALIDATION.md) before editing. Historical sections describe earlier revisions; the current code and newer dated evidence take precedence.

- Keep DSP and chart mathematics testable independently of Android. Add behavior-focused regression tests for behavior changes.
- Preserve original WAVs, readings, uncertainty, and storage compatibility. Derived caches can be regenerated; originals cannot.
- Use [architecture](ARCHITECTURE.md) to locate responsibilities. Read [viewer engineering notes](IMMERSIVE_VIEWER.md) before changing gestures, insets, or lifecycle.
- Write new technical documentation and commit messages in English. Keep both READMEs aligned.
- Review Git status, explicit staged paths, and diffs; exclude private audio, backups, credentials, keystores, and generated output. Preserve existing work and history.
- Record relevant commands, actual outcomes, and limits in [VALIDATION.md](VALIDATION.md), and decisions in [WORK_LOG.md](WORK_LOG.md). Use checks appropriate to the change; documentation-only edits do not require an APK build.

## Continuous integration

[Android Kotlin CI](../.github/workflows/android.yml) runs on pushes, pull requests, and manual dispatch. It runs JVM tests, builds the debug APK and Android test APK, checks en/es resources, generates coverage and Detekt reports, and uploads artifacts. It does not validate microphone hardware, audible synchronization, sustained recording, or physical acoustic accuracy. A workflow definition alone does not establish the result of a particular run.
