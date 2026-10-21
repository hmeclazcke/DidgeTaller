# Evidencia Acoustic Studio 3.2 — 21/10/2026

Capturas reales, revisadas visualmente. Ninguna es una maqueta. Las del Samsung usan la misma sesión existente de 7,72 s, sin grabar ni reproducir audio. Las de emulador usan fixtures sintéticos aislados.

## Samsung SM-G990E / R5CT21KTQ8W

- [Espectrograma horizontal ANTES, 3.1](../v32-samsung/before-3.1-spectrogram-landscape.png).
- [Espectrograma horizontal DESPUÉS, 3.2](../v32-samsung/after-3.2-spectrogram-landscape.png).
- [Fundamental horizontal, 3.2](../v32-samsung/after-3.2-fundamental-landscape.png).
- [Espectrograma vertical, 3.2](../v32-samsung/after-3.2-spectrogram-portrait.png).

Altura aproximada de los datos coloreados, sin ejes: **306 / 1080 px (28.3%) → 773 / 1080 px (71.6%)**. El denominador es la captura completa de 1080 px de alto, conservador respecto de la altura útil. Se detectaron filas de paleta y se cotejaron con la imagen; no se confunde contenedor con área de datos. Hierarquías XML originales junto a cada PNG. Resultado numérico: `checkpoints/v32-samsung-layout.json`.

## Emulador: 420 dp, fuente 1,0

- [Espectrograma horizontal](spectrogram-landscape.png) y [vertical](spectrogram-portrait.png).
- [Fundamental horizontal](fundamental-landscape.png) y [cursor Hz/nota/cents](fundamental-cursor-landscape.png).
- [Forma de onda](wave-landscape.png) y [espectro/armónicos](harmonics-landscape.png).
- [Medir: Flauta nativa](measure-flute.png), [detalle de sesión de flauta](flute-detail.png).
- [Melodía con silencios, fundamental](flute-fundamental-portrait.png) y [espectrograma exclusivamente Hz](flute-spectrogram-portrait.png).

## Adaptación a tamaño/fuente

| Ventana / fuente | Contenedor del gráfico / altura útil | Datos sin ejes / altura útil | Captura |
|---|---:|---:|---|
| 360 dp / 1,0 | 85,7% | 74,6% | [Horizontal](360-f100-spectrogram-landscape.png) |
| 360 dp / 1,3 | 85,7% | 71,2% | [Horizontal](360-f130-spectrogram-landscape.png) |
| 420 dp / 1,0 | 75,8% | 66,4% | [Horizontal](spectrogram-landscape.png) |
| 420 dp / 1,5 | 87,9% | 73,8% | [Horizontal](420-f150-final-spectrogram-landscape.png) |

Fuente 1,5: [espectrograma vertical con tiempo completo](420-f150-final-flute-spectrogram-portrait.png), [selector/nota](420-f150-final-measure-flute.png). Medidas de Compose (`PlotDataArea`) en `checkpoints/v32-layout-metrics.log`; excluyen ejes y leyenda. La línea base de emulador tenía solo 51,3% de contenedor, antes de descontar sus ejes. No comparar ese porcentaje de contenedor con el porcentaje de datos del Samsung.

Las iteraciones previas y capturas de fallos están en `checkpoints/v32-visual-iterations/`, fuera de esta selección final. Las limitaciones acústicas y resultados de pruebas están en [VALIDATION](../../VALIDATION.md).
