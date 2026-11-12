# Construction workflow evidence

## Persistent Laboratory wizard

Unedited task-owned API37 emulator PNGs from the connected construction journey:

| Language | Design | Scrolled plan/export controls |
|---|---|---|
| English | [Laboratory, normal font](wizard-en-design.png) | [150% font](wizard-en-plan.png) |
| Spanish | [150% font](wizard-es-design.png) | [150% font](wizard-es-plan.png) |

The English Laboratory capture includes the real four-tab application shell.
The large-font captures mount the same constructor with localized resources and
safe insets in its synthetic test host; the host's white system-bar margins are
not screenshots of a physical device. Export views are intentionally scrolled to
the PDF action. Plans use calculated geometry, never private recordings. Separate
landscape captures and full logs remain local under checkpoints/workshop-final;
the final workflow tests exercise all exports and re-opening saved designs.

## Previous bilingual editorial captures

Earlier task-owned API37 editorial captures, before the persistent wizard:

| Language | Candidates | Workshop/export screen |
|---|---|---|
| English | [Current capture](editorial-en-candidates.png) | [Current capture](editorial-en-wood-plan.png) |
| Spanish | [Captura actual](editorial-es-candidates.png) | [Captura actual](editorial-es-wood-plan.png) |

The captures use current localized dimensions and uncertainty wording. Historical
images below retain their original dated provenance and are not new-run evidence.

These are unchanged PNGs from the clean API35 Pixel2 instrumentation artifact of
[run 42917003540](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42917003540),
production source `992dadc`. The four base tests and all EN/ES portrait cases passed;
the overall run failed only during landscape navigation automation. That failure
is not presented as a full-suite pass. Every shown scenario passed individually.
Fixtures, design names and pipe measurements are synthetic, not commercial specs.

The same production code subsequently passed the targeted landscape case in
[run 42993552622](https://github.com/hmeclazcke/DidgeTaller/actions/runs/42993552622),
test-only revision `70e744d`. That synchronization fix closes the active IME before
navigation and explicitly checks the selected stage. The landscape images below
come unchanged from that successful run.

| Capture | Evidence |
| --- | --- |
| [English candidates](en-normal-candidates.png) | Ranked predictions, selection and ideal/manufactured overlay |
| [Spanish candidates](es-normal-candidates.png) | Same workflow in Spanish |
| [English wood plan](en-normal-wood-plan.png) | Manufactured dimensions and export actions |
| [Spanish wood plan](es-normal-wood-plan.png) | Same workflow in Spanish |
| [PVC plan](default-pvc-plan.png) | Actual measured stepped/reducer interpretation versus ideal |
| [Spanish PVC editor at 150%](es-150-pvc-editor.png) | Header remains visible above the keyboard; editor scrolls |
| [Spanish target](es-normal-target.png) | Target entry and progressive advanced constraints |
| [PDF overview](en-normal-pdf.png) | Native PdfRenderer output, dimensions, cross-sections and ruler |
| [PDF first tile](en-normal-pdf-tile.png) | 1:1 profile, 150 mm alignment mark and 100 mm print reference |
| [Landscape at 150%](landscape-150-candidates.png) | Visible candidate selection with accessible font scaling |
| [Spanish PDF overview](landscape-150-pdf.png) | Native renderer output after the complete landscape workflow |

PDF images are actual Android renders, not drawings reconstructed for documentation.
The tests measure the PDF raster ruler and registration location and compare the
exact tiled/untiled page counts. No printer or physical instrument was calibrated.
