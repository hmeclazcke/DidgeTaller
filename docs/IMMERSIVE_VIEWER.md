# Visor acústico 3.1

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
