# Didgeridoo laboratory

The **Laboratory / Laboratorio** is an experimental companion to recorded-audio analysis. It provides a bore editor, acoustic simulation, and a search tool for exploring didgeridoo geometry. It does not model Native American flutes.

For installation and recording, start with the [project README](README.md). This guide covers design tools only.

## Workflow

1. Open **Laboratory** to start the construction wizard. Define the target passive resonance, length and internal bore profile; advanced controls retain higher targets and point editing.
2. **Choose material:** measured natural log, two boards, a block to split, or a measured modular PVC assembly. Catalog dimensions are user-provided, not commercial assumptions.
3. **Validate and adjust:** inspect dimensional readiness and predictions for the actual manufactured profile. Candidate search belongs here. A stepped or cylindrical PVC alternative never silently replaces a widening target bore: inspect the difference and explicitly accept it.
4. **Construction plan:** preview dimensions and export PDF, SVG, CSV or the separate tiled 1:1 template. Editing earlier inputs revokes the prior plan and approval.
5. Save from any stage without leaving the wizard. **Library → Designs → Open** resumes the same identity and stage; repeated saves update it rather than append copies. Incomplete drafts remain editable, and plans are recalculated after restoration.

Closing an edited unsaved draft offers Save, Keep editing or Leave without saving. Android Back first returns to the preceding stage. The live microphone spectrum, the older three-resonance evolutionary search and Guide remain optional Laboratory tools after closing the wizard. The spectrum does not record and does not represent simulated impedance. Session/design association remains available independently.

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


Construction plans on the current PR #5 feature branch support longitudinal split
wood and measured modular PVC. Existing transverse-layer projects remain readable
and exportable, but that method is not offered for new designs. See the bilingual
[README](README.md) and [sample plans](docs/examples/construction/README.md).
