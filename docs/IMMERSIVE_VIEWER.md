# Visor acústico 3.1

## UX lifecycle clarification — 2026-10-23

The current measurement and wide-measurement destinations share the same retained
capture owner. A configuration change must preserve that capture, its timeline,
start time and progressive WAV. Actual backgrounding or leaving measurement still
stops and saves; there is no background-recording service. This supersedes older
statements below that capture stops during Activity recreation. The navigation
back stack remains the owner of Back; no replacement BackHandler is introduced.

Session detail and the immersive viewer now reuse the same position-polling helper
(80-ms polling of MediaPlayer). The detail card includes a labeled seek slider;
its drag preview does not replace the actual player cursor or reset chart cameras.
Full screen is a header action. Experimental estimator controls live under
Advanced diagnostics, including in the viewer tools sheet. Native pitch kernels,
classification, captured samples, discontinuities and persisted data are unchanged.

## Recuperación 3.2.5 — 21/10/2026

Se conserva cámara/gestos/insets/transporte. `PitchTrace` separa estimaciones originales, conexiones y calidad visual; las tres superficies comparten color por variación y periodicidad, con valores consultables en el cursor. Las conexiones que omiten estimaciones son discontinuas. Los saltos débiles sin apoyo temporal pueden quedar como puntos originales, sin implicar un barrido. General limita el peso de puntos débiles aislados; mantiene cambios/toots respaldados y acceso explícito al rango completo. No hay decimación de F0, interpolación ni reescritura de datos. Reglas, métricas y límites físicos: [VALIDATION](VALIDATION.md).

## Reparación de Fundamental 3.2.4 — 21/10/2026

La política visual3.2.3 queda reemplazada: umbral0,70 y enlaces limitados entre extremos medidos coherentes tras incertidumbres breves, sin muestras interpoladas. Silencios reales/pausas largas cortan. Tarjeta/detalle/fullscreen usan la misma tendencia; originales y razones disponibles en herramientas. Criterios completos y comparación Re·D2/Mib·Eb2 en VALIDATION. General incluye toda la tendencia respaldada y contexto; no se cambia zoom, Reset ni propiedad de cámaras.

Fundamental muestra unidad Hz una sola vez en el margen izquierdo, números/notas medidos; margen fijo durante gesto. Leyenda «Línea: tendencia», sin aviso «N fuera» ni párrafo permanente bajo el gráfico. Conteos/ayuda conservados en herramientas. STFT/onda/transporte/insets/orientación no modificados.

## Graph Intelligence & UX Stability 3.2.2 — 21/10/2026

Reglas vigentes que reemplazan las descripciones históricas: `explorer(id)` es fullscreen y `preview(id)` es detalle. Cámaras, gráfico, lectura y seguimiento independientes; selección/fragmento/bucle pertenecen a la sesión activa. Salir completamente elimina ambos estados; rotar o volver al detalle no contamina cámaras. La tarjeta deriva su rango de datos, sin ExplorerState.

Fundamental limita el dominio manual a General robusta. Mostrar todos/rango exacto habilitan explícitamente dominio extendido; Reset lo desactiva y recupera tiempo completo + General. Ajustada muestra dispersión pequeña sin suavizar. Series inmutables y rangos memoizados, sin auto-fit durante gesto/pan.

PitchTrace clasifica estimaciones contextualmente inciertas sin cambiar sus Hz: ver criterios/tests en VALIDATION. Marcadores opcionales y cursor con Hz/confianza originales, sin confirmar nota. Valores confiables recortados por percentiles se avisan y pueden verse por completo. Music no asigna nota a cero/casi cero/fuera de MIDI0–127. Las transformaciones radiales/STFT/insets de3.2.1 se conservan.

## Stability & Precision 3.2.1 — 21/10/2026

`measure/wide` se desapila hacia `measure`; ambas presentaciones conservan captura. Navegación de sesión/visor mantiene su propietario de audio anterior. No BackHandlers.

Pinch usa razón radial de centroides, no separaciones X/Y. El ancla es el centro actual; pan normalizado lo lleva al centro anterior antes de calcular la nueva ventana. Solo al final se aplica el dominio. Se conservan coordenadas de datos al redimensionar; el margen Y de Fundamental no depende de los ticks del momento.

STFT fija posiciones al origen del WAV y hop16384, pondera celdas por soporte temporal y mantiene referencia de color. La transformación del bitmap usa float. Render al soltar, sin180ms de espera; durante gesto conserva la tesela completada en sus coordenadas reales. VISUAL_VERSION3 es independiente de análisis VERSION2.

Serie canónica: lecturas originales si existen; reanálisis solo para sesiones sin serie. `PitchTrace` comparte cortes y validez; General usa todos los válidos y contexto de tres semitonos por lado, Ajustada conserva escala robusta y advertencia de recorte. Textos completos en herramientas. Reset conserva selección/lectura/audio y restaura cámara general. Estas reglas reemplazan las descripciones3.1/3.2 que siguen.

## Acoustic Studio 3.2 — 21/10/2026 (prevalece sobre la presentación anterior)

- Un solo propietario de `safeDrawing` en el visor; el Scaffold exterior entrega insets cero. Se oculta únicamente la barra de estado con `WindowInsetsControllerCompat`, se conserva navegación/cutout y se restaura su visibilidad al salir. [API Android](https://developer.android.com/develop/ui/views/layout/immersive).
- Barras de 48 dp. En horizontal bajo (<400 dp de altura de ventana) o fuente ≥1,3, transporte y acciones se reúnen en una única barra. En ventanas más altas hay dos barras. Menú de gráfico en el título; escalas, selección, análisis, zoom alternativo y lectura completa en Herramientas. Con fuente grande se prioriza el tiempo completo; zoom sigue disponible por gesto/panel.
- Leyenda STFT en el margen reservado del eje, nunca sobre los datos. Márgenes de etiquetas medidos con Paint; la misma región `PlotDataArea` gobierna dibujo, inspección y gestos. Las pruebas verifican área de datos y selección temporal después de cambiar tamaño.
- Fundamental conserva eje lineal en Hz. `Music.pitch` devuelve MIDI, referencia A4=440 y cents decimales, o null para entradas inválidas. `PitchAxis` usa notas exactas espaciadas en Hz; en zoom fino vuelve a ticks numéricos con una sola equivalencia «≈». Nota secundaria y ancho medido antes del dibujo. El aviso de lecturas fuera del rango corresponde al viewport actual; Mostrar todos lo elimina cuando efectivamente entran.
- `ExplorerState` toma el instrumento guardado de la sesión. Didgeridoo conserva rangos graves; Flauta nativa usa análisis nominal 200–1600 Hz (guarda de interpolación ±0,5%) y presets espectrales 2000/4000/Nyquist. Espectrograma muestra exclusivamente Hz; onda y procesamiento STFT no se duplican.
- Cámara, selección, reproducción y back stack conservan la propiedad por sesión de 3.1. Ningún BackHandler agregado. La orientación solicitada previamente se restaura al salir.

Evidencia y límites actuales: `VALIDATION.md`, `UI_AUDIT.md`. La descripción histórica de detector 35–220 Hz que sigue corresponde exclusivamente a didgeridoo.

## Navegación y propiedad del estado

`viewer/{id}/{plot}` es un destino de Navigation Compose sobre `session/{id}`. Atrás y la flecha desapilan el mismo destino. El visor tiene su propio Scaffold con áreas seguras, sin navegación inferior ni scroll vertical contenedor. El Laboratorio ofrece una portada y destinos `lab/tool/{tool}` con pestañas fuera del formulario desplazable.

`ExplorerState` pertenece al ViewModel por UUID: gráfico, cámaras, selección, fragmento, bucle y seguimiento. No contiene PCM, matrices STFT ni referencias a Activity. La recreación por orientación conserva ese ViewModel y el MediaPlayer existente. Cambiar detalle↔visor de la misma sesión conserva recursos; abandonar la sesión o pasar la app al fondo detiene y libera reproducción. La captura se detiene y guarda incluso al recrear la Activity; nunca se inicia automáticamente.

«Girar» solicita orientación horizontal solo mientras se explora. Al abandonar el visor se restaura la orientación solicitada anteriormente. En horizontal, el selector de gráfico pasa al título desplegable y las herramientas a iconos; el gráfico ocupa el espacio principal. No se cambió el manifest para fijar orientación global.

La cámara/selección sobreviven a recomposiciones y recreaciones de Activity, pero no se promete restaurarlas después de terminar el proceso de Android. El WAV, índice, relaciones y anotaciones siguen persistiendo con los formatos existentes.

## Coordenadas y gestos

`AxisWindow`, `AxisDomain`, `PlotViewport` y `PlotProjection` son Kotlin puro. Transforman coordenadas de datos y píxeles considerando márgenes para ejes e inversión vertical. El zoom se ancla al centro del gesto y se limita al dominio y a un tamaño mínimo. Tiempo mínimo visible: 2 ms o dos muestras; frecuencia: al menos un bin FFT; amplitud: resolución PCM; en escala logarítmica se limita la proporción mínima y nunca se usa cero.

`PlotGestures` consume punteros dentro del visor: un dedo desplaza en dos ejes, dos dedos amplían cada eje según su separación, pulsación prolongada inspecciona y doble toque restablece. El desplazamiento se detiene al llegar a los datos; una vista que ya abarca todo un eje requiere ampliarse antes de poder recorrerlo. Botones Acercar/Alejar/Restablecer, flechas y entrada numérica ofrecen alternativas a los gestos. Los gráficos resumidos abren el visor al tocarlos; el scroll de la pantalla sigue disponible en el detalle.

## Representación

Los cuatro adaptadores comparten `PlotSurface`, márgenes, grilla, etiquetas, recorte y superposición de cursores. No se comparten artificialmente sus ejes:

| Gráfico | X | Y | Datos |
|---|---|---|---|
| Fundamental | Tiempo | Hz | Lecturas válidas originales o reanálisis del WAV |
| Espectrograma | Tiempo | Hz lineales/logarítmicos | Potencia STFT real |
| Onda | Tiempo | Amplitud PCM normalizada | Mínimos/máximos; muestras al ampliar |
| Espectro | Hz | dB relativos | PSD promedio, picos y candidatos armónicos de fase A |

La escala de fundamental toma percentiles 2,5–97,5% para ≥20 lecturas válidas, con margen máximo entre 0,6 Hz, 1,2% de la mediana y 18% del rango. Se informa cuántas lecturas quedan fuera y se ofrece «Mostrar todos los valores». Los datos originales no cambian. Se respetan los límites de validez del detector existente (35–220 Hz, periodicidad ≥0,70). Ausencias e intervalos de más de 1,2 s interrumpen la línea; no se convierten en cero. La escala calculada se retiene para evitar ordenar las lecturas cada vez que avanza el cursor.

El espectro parte de 0–500 Hz para separar los primeros armónicos, con presets hasta Nyquist. Las guías señalan múltiplos teóricos; Hn? indica candidato, no confirmación. La inspección muestra valores calculados y desviación en cents. El intervalo del espectro se identifica como grabación completa o fragmento.

## Procesamiento y colores

Se reutilizan `AudioVisuals`, `SpectralAnalysis`, `HarmonicAnalysis`, `WavAnalysis` y la caché existentes. No cambian algoritmos, modelos de sesiones ni WAV. STFT: Hann 32768, solapamiento 50%, 1,35 Hz por bin a 44100 Hz y ventana de unos 743 ms. Ampliar el gráfico no agrega resolución física.

Las solicitudes de visualización esperan 180 ms y el fin del gesto. La última celda calculada permanece en sus coordenadas originales mientras llega el reemplazo; no se estira para aparentar nuevos datos. Renderizado y FFT ocurren fuera del hilo principal. El estado mantiene un viewport de hasta 256×256 celdas y una envolvente de hasta 1024 puntos, con caché de disco regenerable de 64 MiB. No se almacena un WAV completo en Compose.

El espectrograma fija la referencia al máximo de la primera vista con energía de esa exploración. Cambiar de rango conserva esa referencia, la paleta y el rango dinámico; las magnitudes mayores saturan en el extremo superior y eso se explica en las herramientas. Es una referencia relativa de visualización, no el máximo garantizado del WAV completo ni SPL calibrado. La caché guarda potencia, por lo que este cambio de presentación no invalida sus cálculos.

## Reproducción y límites

El cursor lee `MediaPlayer.currentPosition` aproximadamente cada 80 ms. Pausa, seek y bucle usan el reproductor existente. «Seguir» encuadra el cursor al llegar al borde; cualquier exploración manual desactiva el seguimiento. El usuario puede activarlo nuevamente. No hay cronómetro paralelo que estime la posición del audio.

MediaPlayer, el sondeo y los seeks no garantizan precisión entre muestras ni bucles sin separación. La sincronía audible, el gesto predictivo y el rendimiento sostenido deben contrastarse físicamente en el Samsung. Las señales sintéticas y las capturas del emulador no validan exactitud acústica con un didgeridoo.

Pruebas y capturas: `VALIDATION.md`, `UI_AUDIT.md` y `screenshots/README.md`.
