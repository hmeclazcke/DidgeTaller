# Stability & Precision 3.2.1 — 21/10/2026

Capturas reales de emulador con fixtures sintéticos propios, revisadas visualmente. Los gestos multitáctiles se inyectaron en emulador; no se afirma validación táctil física en Samsung.

- [Medir normal](measure-normal.png), [Vista amplia](measure-wide.png), [grabando en Vista amplia](measure-wide-capturing.png).
- [Espectrograma horizontal](spectrogram-landscape.png), [vertical](spectrogram-portrait.png), [lectura completa](spectrogram-reading-tools.png).
- La misma serie de prueba: [Biblioteca](library-same-session.png), [detalle con fuente 1,3](fundamental-detail.png), [Fundamental general](fundamental-general.png), [ajustada](fundamental-fitted.png), [herramientas/lectura completa](tools-complete-reading.png).
- Flauta: [Medir](regression-measure-flute.png), [sesión](regression-flute-detail.png), [fundamental](regression-flute-fundamental-portrait.png), [espectrograma](regression-flute-spectrogram-portrait.png).
- 360 dp / fuente 1,3: [horizontal](360-large-spectrogram-landscape.png), [lectura](360-large-tools-complete-reading.png).
- 420 dp / fuente 1,5: [horizontal](420-large-spectrogram-landscape.png), [lectura](420-large-tools-complete-reading.png).

El contenedor conserva barras de 48 dp y los márgenes de STFT de 3.2. La prueba verifica ≥65% de altura útil para datos sin ejes en las configuraciones recorridas. Medida automatizada 3.2.1: 66,4% de datos en 420 dp/fuente 1,0; contenedor con ejes 75,8% (v321-regression-close-runner.log). Las variantes con fuente grande también superaron el mínimo del 65%. En Samsung 3.2 se midió 71,6% de altura total; esa cifra no es una nueva medición física de 3.2.1.

Un hueco en Fundamental es ausencia de lectura; no se interpola a través de silencio. La vista ajustada muestra variaciones de ±0,15 Hz que parecen grandes por el zoom; la general conserva contexto. Los círculos de baja confianza no se unen. La STFT conserva Hann32768/hop16384: la resolución acústica no aumenta por ampliar.

Las imágenes Medir y gráficos se recapturaron en cuatro casos focalizados con espera del dibujo nativo después de la transición (v321-evidence-instrumentation.log y v321-evidence-plots-instrumentation.log); esos casos ya están incluidos en los 32 distintos.

Iteraciones fallidas/transitorias archivadas en `checkpoints/v321-visual-iterations/`. [Resultados completos](../../VALIDATION.md).
