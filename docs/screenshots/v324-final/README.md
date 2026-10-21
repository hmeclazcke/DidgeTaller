# Reparación selectiva de Fundamental 3.2.4 — evidencia visual

Capturas auténticas de emulator-5554/API37, revisadas visualmente. Comparación en1080×2400,densidad411(≈420dp),fuente1,0: APK3.2.2/8 previo a la regresión;3.2.3/9 regresivo;3.2.4-beta1/10 reparado, SHA-256 `6fe6bcdcfb976acd5124e1f91b43fabddefb41a534677fd6f6ec8c8611c1c1a6`.

Las series **Re·D2 y Mib·Eb2 son las lecturas exactas guardadas de las sesiones reportadas**, extraídas del respaldo local en solo lectura. Fecha/duración/resumen conservados, IDs nuevos y WAV **silenciosos** de igual duración en sandbox de pruebas del emulador. No son nuevos análisis acústicos ni reproducción del audio privado. Las dos sesiones originales permanecen intactas en el Samsung. Otros fixtures se identifican en la tabla.

| Evidencia | Revisión |
|---|---|
|Biblioteca [3.2.2](pre323-library.png) / [3.2.3](before-library.png) / [reparada](after-library.png)|Ambas tarjetas reales; se recuperan trazos de tendencia sin los períodos bajos de3.2.2 ni vaciado de3.2.3|
|Re·D2 [3.2.2](pre323-D2-detail.png) / [3.2.3](before-D2-detail.png) / [reparada](after-D2-detail.png)|Enlaces35→15→33; conserva huecos de ausencia real, no promete línea durante toda la sesión|
|Mib·Eb2 [3.2.2](pre323-Eb2-detail.png) / [3.2.3](before-Eb2-detail.png) / [reparada](after-Eb2-detail.png)|Enlaces23→5→19; tendencia principal continua cerca de75Hz, sin período bajo dominando la escala|
|Fullscreen [Re·D2](after-D2-fullscreen.png) / [Mib·Eb2](after-Eb2-fullscreen.png)|Misma clasificación/rango que tarjeta y detalle, originales disponibles en herramientas|
|[360dp/fuente1,3](360-f130-D2-detail.png)|Re·D2, eje legible/nota secundaria, sin superposición|
|[420dp/fuente1,5 detalle](420-f150-Eb2-detail.png) / [tarjetas](420-f150-library.png)|Lecturas reales; textos acomodados, curvas no fragmentadas artificialmente|
|[Leyenda compacta](after-legend.png)|Fixture controlado de regresión; unidad Hz única, «Línea: tendencia», sin párrafo permanente ni «N fuera»|
|[Toot respaldado](short-toot.png) / [melodía de flauta](flute.png)|Fixtures controlados205/207Hz y392/440/523/440Hz; cambios genuinos del fixture conservados|
|[Restablecer](after-reset.png)|Fixture controlado: General alrededor de la tendencia tras zoom; original60Hz conserva cursor/razón sin imponer su escala|
|[Eje0–1Hz](zero-hz-no-note.png)|Rango exacto explícito del test; cero sin nota imposible. No es General de una sesión70Hz|
|[Espectrograma horizontal](spectrogram-landscape.png)|Audio sintético del test existente; barras compactas y datos dominantes conservados, eje solo Hz|

Reserva izquierda del detalle916px:115,55→74,55px(−35,48%); datos769,625→810,625px(+5,33%), medidos mediante `PlotDataArea`. No son tamaños fijos del código: se miden números/notas reales,4dp exteriores+4dp de separación, ancho retenido durante gesto. General reparada: Re·D2≈60,58–85,68Hz; Mib·Eb2≈63,77–90,18Hz.

20 imágenes representativas conservadas aquí; capturas redundantes/corridas previas en checkpoints locales. Manifiesto de archivos y SHA-256 `checkpoints/v324-screenshot-manifest.json`. Ver [VALIDATION](../../VALIDATION.md) para umbrales, límites, tests y verificación no destructiva del Samsung. Un cambio físico de una única lectura puede clasificarse incierto: los originales siguen accesibles. Estas capturas no sustituyen prueba musical, afinador independiente ni pinch con dedos físicos.
