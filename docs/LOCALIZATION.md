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

## Staged inventory and verification

Stage 1 migrates **410 distinct string resource keys**, plus two twelve-element
note arrays, in both languages. Coverage includes navigation, measurement,
Laboratory tools/guide, Library, session details, comparison, explorer/viewer,
chart captions/legends, dialogs, settings and accessibility labels. This counts
resource keys, not call sites or duplicated occurrences.

The initial inventory used a Kotlin-aware local literal scan (including nested
templates), then manual classification at display and persistence boundaries.
Legacy backend diagnostics and ViewModel event messages remain for stage 2;
their existing Spanish strings are not yet fully localized by this stage alone.
Quantity-specific wording and a wider visual audit also remain pending.

`python tools/check_localization.py` checks en/es key/type parity, positional
format signatures, quantity/array shapes and supported locales. It is a resource
validator, not a translation engine. Existing instrumentation uses isolated
Application contexts/files/preferences on an explicitly selected emulator.
`LocalizationUiTest` supplies real Android en/es/de configuration contexts to
Compose: English and Spanish measurement/Library, unsupported-language English
fallback, note labels and decimal formatting pass. See VALIDATION.md for exact
commands, failed characterization and current stage results.

At stage 1, full chart/dialog visual inspection, long text clipping, screen-reader
speech, physical device behavior and an in-place OS app-language change have not
yet been verified. No claim of acoustic correctness follows from localization.

## Platform references

- [Android localization and default resources](https://developer.android.com/guide/topics/resources/localization)
- [Compose string and plural resources](https://developer.android.com/develop/ui/compose/resources)
- [System per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages)
