# Evidencia nativa 3.2.6 — 21/10/2026

21 PNG originales del emulador, revisados visualmente. APK3.2.6-beta1/12, fuente `4330e71`, SHA-256 `50be9eeb17e90ce0054fa26256628037995964f9243a0761fdd771f670609a72`. No hay imágenes generadas ni audio personal en estos fixtures. Las capturas de las tres series privadas quedan locales, fuera de Git.

| Caso | Capturas |
|---|---|
| Medir, PCM sintético por el callback real; no micrófono físico | [Drone70](measure-drone.png), [silencio](measure-drone-silence.png), [señal incierta](measure-drone-uncertain.png), [flauta440](measure-flute.png) |
| Misma secuencia sintética de variación y periodicidad, sin alterar originales | [Biblioteca](colours-library.png), [detalle](colours-detail.png), [General](colours-fullscreen.png), [Ajustada](colours-fitted.png) |
| Título completo después de desplazarlo a la vista | [Explorar el audio](session-heading.png) |
| STFT calculada sobre WAV sintético | [Vertical](spectrogram-portrait.png), [horizontal](spectrogram-landscape.png) |
| Visor y lectura conservados | [Fundamental horizontal](fundamental-landscape.png), [cursor](fundamental-cursor-landscape.png), [onda](wave-landscape.png), [armónicos](harmonics-landscape.png), [melodía de flauta](flute-fundamental-portrait.png) |
| Fixture aislado de ejes47,9s, mismo1080px/420dpi/fuente1,0 | [Antes3.2.5](axis-before.png), [después3.2.6](axis-after.png) |
| Resolución y fuente ajustadas explícitamente | [360dp/fuente1,3, horizontal](matrix360-spectrogram.png), [≈420dp/fuente1,5](matrix420-fundamental.png), [Medir360dp/fuente1,0](matrix360-uncertain.png) |

El fixture de colores alterna70/71,4Hz y70/74Hz intencionalmente, incluye un silencio y un tramo de periodicidad0,73: sus zigzags son datos de prueba. Los tres colores aparecen en píxeles reales. No representa una vocalización física.

El benchmark de ejes usa el mismo renderer sin señal dibujada. STFT: margen147→69px, datos817,5→895,5px (+9,54%); X pasa de solo0s a marcas útiles. Fundamental ya medía75px y conserva ese ancho. La matriz usa945×2100 y1103×2100px con420dpi, equivalentes a360 y≈420dp de ancho; dpi y dp no se confunden. En capturas desplazadas, el recorte de contenido superior pertenece al scroll normal.

128 JVM y44 casos UI distintos aprobados, más8 repeticiones de matriz/datos. Hubo dos expectativas de texto antiguas en la primera regresión completa; se actualizaron y ambos casos pasaron por separado con el mismo APK. Detalles, diagnóstico acústico y límites en [VALIDATION](../../VALIDATION.md).
