# Repository language policy

Developer instructions, source identifiers, comments, KDoc, build instructions,
test explanations and current technical documentation use English. The public
README is bilingual: `README.md` is English and `README.es.md` is Spanish, with
reciprocal links. Android default resources use English; `values-es` and any
future Spanish regional resources remain Spanish. Argentine Spanish resolves
through Android's Spanish language fallback; there is no separate regional
resource directory in the current tree.

## Classify before translating

| Category | Handling |
| --- | --- |
| A — Developer explanation or diagnostic assertion | Translate, preserving meaning and executable logic |
| B — Localized UI or quoted UI label | Preserve the intended locale and resource mapping |
| C — Spanish public documentation | Maintain in Spanish, including README.es.md |
| D — Exact fixture, test expectation or demonstration name | Preserve unless the test explicitly prepares localized synthetic showcase data |
| E — Persisted key, identifier or compatibility-sensitive message | Preserve; cosmetic renaming is outside language cleanup |
| F — Third-party material | Preserve attribution, license and original source |
| G — Historical evidence | Preserve dated facts and wording; introduce current English guidance separately |

## Audit scope and exceptions — 2026-11-06

The audit used Git-tracked text, excluding Spanish resources, the Spanish README,
third-party source, binaries, caches, checkpoints and user data. Accented-character
and Spanish technical-word searches produced **candidates**, not a count of
translation errors. Context review identified two Spanish production comments,
eighteen technical test/example messages, two workflow step labels and seven bootstrap
comments/messages for translation. No production identifier needed renaming.

`DiagnosticText.kt` matches legacy Spanish diagnostic messages to localized
resources. Those strings and their producers form a compatibility-sensitive
mapping (E), not untranslated English UI. `LocalizedLabels.kt` likewise maps
legacy labels. `AcousticPlot` legacy titles, marker types, preference names and
session names remain intact. Localization tests intentionally assert Spanish
text (B/D), and arbitrary session names must never be translated on load.

Dated sections of WORK_LOG, VALIDATION, ROADMAP, PRODUCT_CONTEXT, UI_AUDIT,
IMMERSIVE_VIEWER, DIDGELAB_STUDY, ACOUSTIC_VALIDATION and screenshot evidence
indexes retain historical Spanish (G). They document previous versions and are
not current implementation instructions. Current English summaries identify
the active sources of truth. Translating or silently modernizing those records
would obscure their original evidentiary context. Historical screenshots also
remain authentic originals in Git history and dated directories.

Current source and guides take precedence over old descriptions of pitch
semantics, seek sliders, gesture ownership or recording lifecycle. See
[Session explorer](SESSION_EXPLORER.md), [Development](DEVELOPMENT.md),
[Architecture](ARCHITECTURE.md) and the latest validation entries.

## Validation discipline

Check localized XML, positional placeholders, plurals and arrays with
`python tools/check_localization.py`. Review real rendered English and Spanish
screens. Compare Spanish resource bytes against the starting revision when
they are outside the change scope. Review source diffs to separate translated
comments/assertion text from executable changes. Screenshot fixtures may change
test setup; they must not alter production behavior or use private recordings.


## Technical terminology reference (current interface and exports)

| Quantity or operation | English | Spanish | Meaning |
|---|---|---|---|
| First inverse-design target | First passive resonance | Primera resonancia pasiva | Input-impedance model target, not a guaranteed played fundamental |
| Circular air passage | Internal bore diameter | Diámetro interior | Full opening; distinct from radius or groove depth |
| Semicircular groove | Groove depth per half | Profundidad por mitad | From the joining plane to the bottom of each groove |
| Prepared rectangular stock | Prepared width | Ancho preparado | Full width, not half the numerical value |
| Prepared log section | Joint plane to outer surface (derived) | Plano de unión a superficie exterior (calculado) | Derived circular-cap depth after supplied preparation losses |
| Wood below the groove | Remaining wall thickness | Espesor de pared restante | Below-groove thickness; minimum wall separately considers board sides |

Display values follow the selected locale; interchange/catalog numeric input and
CSV values retain decimal points so delimiters and parsing remain stable. Note
labels use musical sharp/flat glyphs; persisted pitch identifiers are untouched.
YIN confidence/periodicity is not a probability, spectral evidence is not a confirmed
fundamental, and passive bore resonances are not guaranteed played notes or toots.
Unknown platform error details deliberately remain verbatim in DiagnosticText;
do not translate saved names, diagnostic matching keys or scientific identifiers.
