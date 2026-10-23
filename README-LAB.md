# Didgeridoo laboratory

The **Laboratory / Laboratorio** is an experimental companion to recorded-audio analysis. It provides a bore editor, acoustic simulation, and a search tool for exploring didgeridoo geometry. It does not model Native American flutes.

For installation and recording, start with the [project README](README.md). This guide covers design tools only.

## Workflow

1. Open **Laboratory → Design / Laboratorio → Diseñar**. Choose a preset or edit the instrument length and internal diameters. The point editor describes the bore with positions and diameters in millimeters.
2. Inspect the profile and simulated resonance results. Save useful designs to **Library → Designs / Biblioteca → Diseños**.
3. Use **Spectrum / Espectro** to examine the simulated input-impedance curve and its resonance peaks. This is a model output, distinct from the power spectrum of a recording.
4. Use **Search / Buscar** to enter three target resonance notes and run the evolutionary search. Progress and cancellation are available; the result can be applied to the editor.
5. Optionally associate a saved design with a didgeridoo session. The session presents simulated resonances and recorded results separately. Manual vocalization or possible-toot markers annotate observations without claiming automatic detection.

**Guide / Guía** introduces the musical and acoustic terms used by the tools.

## What the model represents

[`AcousticModel`](app/src/main/java/ar/didgetaller/core/AcousticModel.kt) implements a segmented, one-dimensional acoustic transmission-line approximation. It includes approximate thermoviscous losses and bell radiation and calculates input impedance and resonance peaks from the bore profile.

[`BoreOptimizer`](app/src/main/java/ar/didgetaller/core/BoreOptimizer.kt) searches profiles using a population, mutation, elitism, and a loss function based on three target resonances. A reproducible search seed supports deterministic experiments. A lower search error means a closer match within this model, not a better physical instrument.

## Interpretation limits

- Input-impedance resonances, harmonics of radiated sound, and played registers or toots are different quantities. Matching their indices is not a valid physical comparison.
- Geometry alone does not establish actual tuning, timbre, playing resistance, or a musician's response to the instrument.
- Simulated resonances and comparisons with recordings are exploratory. The model has not been established as a validated substitute for physical prototypes or reference measurements.
- The tools do not identify vowels, vocalizations, or toots automatically, and a successful optimization is not a fabrication specification.

[Acoustic validation](docs/ACOUSTIC_VALIDATION.md) preserves the comparison protocol and outstanding evidence. [Session exploration](docs/SESSION_EXPLORER.md) explains how to inspect recordings independently of a design prediction.

## Research context and attribution

The project studied DidgeLab and CADSD as conceptual references. The included engines are independent Kotlin implementations; neither reference engine is bundled as a dependency or presented as a port.

The historical [DidgeLab study](docs/DIDGELAB_STUDY.md) records research, attribution, and licensing considerations. It is not the current feature list. Any future incorporation of third-party code requires a separate review of its license and attribution requirements. This repository's existing license is [MIT](LICENSE).
