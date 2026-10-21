# Fundamental Precision 3.2.3 — evidencia visual

Capturas auténticas de emulator-5554/API37, inspeccionadas visualmente. Antes: APK3.2.2/8. Después: APK3.2.3/9, SHA-256 `319f1223326e60ea34f0c357a39639c1725b2304f1260627933aa1df05ba5787`. Misma dimensión1080×2400, densidad411 (≈420dp), fuente1,0, salvo matriz indicada.

La comparación usa **timeline controlado**, equivalente al reporte: drone70,6–71,8Hz con retornos a60/65/77/82,5Hz, confianza0,82 frente a0,97 del contexto. Los WAV sintéticos de soporte a navegación son70Hz: estas imágenes verifican representación de metadatos, no detección acústica de esos eventos. El toot visual usa dos lecturas205/207Hz; YIN sobre tonos sintéticos205/207 se prueba separadamente en JVM. No se copió audio privado a documentación ni se presentan estas capturas como interpretaciones reales del Samsung.

| Evidencia | Qué se revisó |
|---|---|
| [Antes](before-detail.png) / [Después](after-detail.png) | Cuatro V/picos naranjas pasan a huecos; etiquetas cerca del eje y leyenda breve |
| [Biblioteca](after-card.png) / [Fullscreen General](after-general.png) | Mismo drone y discontinuidades; tarjeta conserva toot/flauta y cámara propia |
| [Ajustada](after-fitted.png) | Variación fina sin reintroducir los picos; aviso discreto de originales fuera |
| [Originales](after-originals.png) / [Inspección](after-inspection.png) | Círculos sin segmentos;60Hz/confianza0,820 accesible con motivo y sin nota confirmada |
| [Reset](after-reset.png) | Tiempo completo y General; cursor/lectura preservados según política existente |
| [Toot breve](short-toot.png) |205/207Hz visibles, sin eliminación por duración/percentiles |
| [Flauta](flute.png) |392/440/523/440Hz conservados |
| [360dp/fuente1,3](360-f130-general.png) | Etiquetas y datos legibles, sin superposición |
| [420dp/fuente1,5](420-f150-detail.png) / [Horizontal](420-f150-landscape.png) | Eje compacto, notas secundarias y transporte horizontal conservados |

Medida del detalle comparable:916px totales; eje189,825→115,550px; datos695,350→769,625px: **+74,275px /+10,68%**. Reserva−39,13%. Fullscreen:1080px totales, datos859,350→933,625px (+8,64%). Medidas de `PlotDataArea`, no estimación a ojo; logs `checkpoints/v323-before-layout-metrics.log` y `v323-fundamental-layout-metrics.log`. La mejora depende de textos reales/densidad/fuente.

Ver [VALIDATION](../../VALIDATION.md) para reglas, diagnóstico privado de PCM real, resultados y límites. Solo13 imágenes útiles conservadas aquí; corridas completas y descartes en checkpoints locales.
