# English and Spanish localization

## Architecture and boundaries

English is the complete default resource set in `res/values/strings.xml`.
Spanish resources live in `res/values-es/strings.xml`; Android selects resources
from the device/app configuration and falls back to the default for unsupported
languages. `locales_config.xml` advertises en/es to Android's system app-language
settings. No in-app selector, language preference, library or toolchain upgrade.
Compose uses `stringResource`; drawing, semantics and event callbacks capture the
current Android Context and use `getString`. Templates use positional arguments.

`LocalizedLabels.kt` maps presentation labels for instrument/plot enums, pitch
uncertainty reasons, built-in marker types and presets to resources. Enum names,
routes, tags, marker storage values and core implementations remain unchanged.
Note arrays preserve Spanish solfege plus international notation (Do · C2) while
English uses international notation (C2). Numbers follow the application/process
locale; axis formatting trims the active decimal separator without changing
coordinates, domain calculations, clipping or gesture logic.

Persisted recordings, names, timestamps, analysis, measurements, designs and
user-authored marker content are displayed without rewriting or translating their
stored values. Legacy auto-name detection compares the original stored Spanish
form, independently of UI language. CSV content, filenames, MIME types, UUIDs,
JSON/preference keys, extensions, enum identifiers and technical constants are
not UI translations. Existing data can therefore contain Spanish words in an
English interface. No data migration or WAV/DSP changes are required.

## Completed inventory

The completed migration has **478 string keys, five plural keys and two
twelve-element note arrays** in each language. Counts refer to distinct resource
keys, not call sites or duplicated occurrences. Coverage includes navigation, measurement,
Laboratory tools/guide, Library, session details, comparison, explorer/viewer,
chart captions/legends, dialogs, settings and accessibility labels.

The initial inventory used a Kotlin-aware local literal scan (including nested
templates), then manual classification at display and persistence boundaries.
Seventy-one legacy diagnostic patterns, including nested causes, are translated
at the presentation boundary by `DiagnosticText.kt`. This narrow compatibility
adapter is necessary because the protected audio/core/ViewModel contracts emit
strings. Translations still live exclusively in Android resources; there is no
language selector, custom locale store or translation framework. New backend
diagnostics must be added to both resources and the adapter until a separately
scoped change introduces typed diagnostic identifiers. Unknown native/platform
exception details and technical error codes remain verbatim.

`RecordingStore.Session.hasStoredName` records only in-memory provenance of the
existing name field. The JSON writer, index schema and WAV recovery paths are
unchanged. Missing-name/orphan labels use resources; every stored name, even
the literal `Audio recuperado`, remains verbatim. Presentation does not infer
provenance by matching user text or reading a second copy of the index.

Plural resources cover measurements, trend readings, spectral windows, summary
readings and recovered recordings. Number/date formatting uses the active
locale; exported/stored values remain canonical. A language change invalidates
only cached inspection text and remeasures native numeric labels. Selected audio
positions, camera state, gestures, analysis results and PCM are preserved.

The remaining hardcoded UI literals are invariant technical notation (Hz, dB,
PCM, cents, Hn?, signs, time formats and symbols), routes/test tags, or legacy
identifier/comparison values. No known application-authored grammatical UI text
remains outside resources. Historical data may legitimately contain Spanish.

`python tools/check_localization.py` checks en/es key/type parity, positional
format signatures, quantity/array shapes and supported locales. It is a resource
validator, not a translation engine. Existing instrumentation uses isolated
Application contexts/files/preferences on an explicitly selected emulator.
`LocalizationUiTest` supplies real Android en/es/de configuration contexts to
Compose: English and Spanish measurement/Library, unsupported-language English
fallback, note labels and decimal formatting pass. See VALIDATION.md for exact
commands, failed characterization and current stage results.

The first stage was committed as `886834f`; its intentionally partial diagnostic
coverage is retained in the validation history. The second stage adds diagnostics,
plurals, the generated-name distinction and broader emulator/layout verification.
Exact final commands, results, evidence and limitations are in VALIDATION.md.
Selected reviewed bilingual images are indexed in
[UI evidence](screenshots/localization/INDEX.md).

## Local verification

Run the resource validator and existing Gradle checks from the repository root:

```powershell
python tools/check_localization.py
.\gradlew.bat :app:testDebugUnitTest :app:jacocoDebugUnitTestReport :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest detekt detektComplexity
python tools/detekt_summary.py
python tools/coverage_summary.py
```

Only on an explicitly verified emulator, use Android's standard app locales:

```powershell
adb -s emulator-5554 shell cmd locale get-app-locales ar.didgetaller --user 0
adb -s emulator-5554 shell cmd locale set-app-locales ar.didgetaller --user 0 --locales en
adb -s emulator-5554 shell am instrument -w -r -e class ar.didgetaller.ui.LocalizationUiTest ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner
```

Repeat the presentation inventory (`UIAuditTest#matrixInventory` and
`UIAuditTest#renameDialogIsLocalizedWithoutChangingStoredName`) with `es-AR`.
`de-DE` exercises unsupported-language English fallback while retaining locale
number formatting. Restore the original app-locale setting afterward; an empty
override follows the device language. These tests use isolated synthetic files
and preferences, not the existing application Library. Do not run instrumentation
on the Samsung. CI validates resources and compiles instrumented tests, but does
not execute this emulator matrix.

Screen-reader semantics are localized in code and inspected by Compose tests.
Spoken TalkBack output, physical-device layouts, landscape and every possible
font/window combination still require manual verification. Scroll boundaries,
scrollable tabs and deliberate ellipsis of long user names remain normal UI
behavior. No claim of acoustic correctness follows from localization.

## Platform references

- [Android localization and default resources](https://developer.android.com/guide/topics/resources/localization)
- [Compose string and plural resources](https://developer.android.com/develop/ui/compose/resources)
- [System per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages)
