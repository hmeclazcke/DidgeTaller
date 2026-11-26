# Detección de tono en didgeridoo y flauta

**Decisión.** Conservar el detector YIN de `fa4aeee` para la medición en vivo. La correlación normalizada recuperó más frecuencias conocidas con ruido, pero también atribuyó un tono a más señales de ruido resonante en el perfil de didgeridoo. En flauta duplicó los errores de octava en el barrido de balances armónicos. Acortar la ventana adelantó la detección de cambios reales y empeoró algunos tonos sostenidos. Estos resultados permiten reproducir los tres comportamientos sin grabaciones privadas.

## Qué mide cada parte de la aplicación

En vivo, el perfil de didgeridoo analiza 16.384 muestras a 44,1 kHz y actualiza cada 8.192. Busca normalmente entre 35 y 140 Hz, con una opción de hasta 220 Hz. Promedia grupos de cuatro muestras y calcula diferencias normalizadas para distintos períodos. El primer mínimo por debajo de 0,14 tiene prioridad. Si no aparece, prueba el mínimo global. Refina el período por interpolación parabólica y exige energía suficiente y periodicidad de al menos 0,65. Para flauta usa 8.192 muestras, un salto de 4.096, resolución temporal sin decimación, un intervalo de 200–1.600 Hz y un umbral final de 0,80. El suavizado de pantalla conserva por separado la frecuencia aceptada de cada lectura. [Implementación de YIN](../app/src/main/java/ar/didgetaller/core/YinPeriodSearch.kt) · [Aceptación en vivo](../app/src/main/java/ar/didgetaller/core/LivePitch.kt)

En una sesión guardada, la traza Dron requiere evidencia de una componente grave y de su familia armónica en ventanas vecinas. Usa ventanas de 320 ms, salto de 186 ms y FFT de 65.536 puntos. La separación de la grilla es 0,67 Hz a 44,1 kHz. Rellenar con ceros permite muestrear el pico más densamente, pero la ventana de audio sigue limitando la resolución. El análisis armónico y el espectrograma describen energía acústica. El cálculo de resonancias pasivas usa la geometría del instrumento. Ninguno establece por sí solo la frecuencia de vibración de los labios. [Componente del dron](../app/src/main/java/ar/didgetaller/core/DroneComponentAnalysis.kt) · [Análisis armónico](HARMONICS.md)

## Ensayo reproducible

Comparamos la producción, YIN sobre la mitad más reciente de la misma ventana y una implementación experimental de la función de diferencia cuadrática normalizada de McLeod y Wyvill (**NSDF**). Los tres recibieron el mismo PCM, el mismo instante de decisión y la misma política final por instrumento. El código de investigación se ejecuta en la JVM de escritorio. El criterio de adopción quedó registrado en `9014db3` antes de abrir la evaluación reservada. [Protocolo y comandos](PITCH_RESOLUTION.md#paired-window-and-periodicity-study) · [Generador de señales](../tools/pitch-study/StudySignals.kt) · [Candidatos](../tools/pitch-study/PeriodCandidates.kt)

La frecuencia sintética proviene de la fase del oscilador. Variamos la amplitud, la frecuencia instantánea, la envolvente espectral, el predominio de los armónicos 2, 3 y 5, la ausencia de la fundamental y la relación señal/ruido (20, 6 y 0 dB). Incluimos tonos por debajo del umbral de amplitud, ruido blanco, silencio, impulsos y mezclas de dos osciladores independientes. Estas últimas carecen de una única etiqueta de tono. La modulación de amplitud imita una caída de presión, y el cambio de registro es un salto sintetizado. No constituyen muestras de respiración circular ni de toots reales.

Las frecuencias y semillas de desarrollo y evaluación son distintas. Tras el primer conjunto reservado, repetimos la comparación a 44,1 kHz con 30 frecuencias logarítmicas aleatorias y los dos extremos de cada perfil. Después exploramos siete balances de armónicos y ruido filtrado. Estos dos barridos son análisis de sensibilidad posteriores. Los candidatos y sus umbrales permanecieron fijos. Las familias de síntesis se comparten entre conjuntos. Las proporciones siguientes describen el ensayo y no estiman una tasa de aciertos en músicos.

### Exactitud y cobertura

Réplica de 448 señales periódicas por perfil a 44,1 kHz. Las tres columnas de tolerancia cuentan los aciertos sobre las **448** señales. La mediana y el percentil 95 del error absoluto usan solo las lecturas emitidas. Una octava errónea incluye múltiplos enteros de octava con tolerancia de 50 cents.

| Perfil y método | Emitidas / 448 | Sin lectura | ≤10 / ≤25 / ≤50 cents | Octavas erróneas | Error Hz mediana / p95 | Error cents mediana / p95 |
|---|---:|---:|---:|---:|---:|---:|
| Didgeridoo · YIN | 416 | 32 | 323 / 341 / 341 | 19 | 0,01 / 68,37 | 0,36 / 1.585,97 |
| Didgeridoo · ventana corta | 416 | 32 | 308 / 328 / 338 | 21 | 0,03 / 68,27 | 0,56 / 1.585,69 |
| Didgeridoo · NSDF | 416 | 32 | 333 / 353 / 353 | 13 | 0,01 / 55,90 | 0,21 / 1.585,29 |
| Flauta · YIN | 361 | 87 | 248 / 255 / 272 | 32 | 0,20 / 799,82 | 0,34 / 1.902,24 |
| Flauta · ventana corta | 362 | 86 | 250 / 273 / 273 | 35 | 0,20 / 771,52 | 0,34 / 1.901,97 |
| Flauta · NSDF | 364 | 84 | 267 / 273 / 290 | 23 | 0,07 / 728,33 | 0,18 / 1.900,75 |

La pequeña mediana convive con errores grandes: un detector puede resolver el período de un armónico con mucha precisión y aun así equivocarse de nota. En el primer conjunto reservado a 44,1 kHz, los aciertos a 50 cents de YIN, ventana corta y NSDF fueron **42/43/44 de 56** para didgeridoo y **33/35/37 de 56** para flauta. A 32 kHz fueron 43/43/44 y 35/36/36. A 48 kHz, 44/43/44 y 37/37/37. Las otras tasas prueban la API del detector. El micrófono de la aplicación usa 44,1 kHz.

| Condición de la réplica | YIN | Ventana corta | NSDF |
|---|---:|---:|---:|
| Didgeridoo, fundamental ausente y armónicos 2 + 3 | 32/32 | 32/32 | 32/32 |
| Didgeridoo, domina el armónico 2 / 3 / 5 (32 por caso) | 17 / 8 / 5 | 16 / 8 / 5 | 19 / 9 / 5 |
| Didgeridoo, ruido a 0 dB | 23/32 | 21/32 | 32/32 |
| Flauta, domina el armónico 2 / 3 / 5 (32 por caso) | 7 / 5 / 1 | 7 / 5 / 1 | 9 / 7 / 4 |
| Flauta, ruido a 6 dB | 9/32 | 9/32 | 16/32 |
| Flauta, ruido a 0 dB | 0/32 | 0/32 | 0/32 |

Por bandas, YIN y NSDF acertaron respectivamente **100/100 de 140** por debajo de 50 Hz, **146/152 de 196** entre 50 y 100 Hz y **95/101 de 112** a partir de 100 Hz en didgeridoo. En flauta, fueron **86/86 de 140** por debajo de 400 Hz, **106/109 de 182** entre 400 y 800 Hz y **80/95 de 126** a partir de 800 Hz. El CSV conserva cada decisión y cada banda para los tres métodos.

**Qué impide reemplazar el detector.** En el barrido de flauta donde dominaba el segundo armónico, YIN acertó 32/42 y NSDF 22/42. En el barrido completo hubo 10 y 20 errores de octava, respectivamente. Para didgeridoo, NSDF sí mejoró el barrido completo (64/126 frente a 47/126). Sin embargo, ante un filtro resonante excitado únicamente con ruido, YIN emitió 44/144 lecturas y NSDF 48/144. En cinco señales NSDF añadió una lectura ausente en YIN, y en una ocurrió lo contrario. En flauta ambos emitieron 10/144. En los controles simples de silencio, ruido blanco e impulso, los tres emitieron **0/96 por perfil**. Un pico resonante puede persistir aun cuando la excitación carece de período.

**Confianza.** Entre las lecturas YIN con periodicidad ≥0,95, solo 264/329 de didgeridoo y 262/342 de flauta quedaron a 50 cents del objetivo en la réplica. En el intervalo [0,80; 0,95) fueron 70/77 y 10/19. La periodicidad no está calibrada como probabilidad de acertar la nota. En las mezclas de dos osciladores, todos emitieron lecturas en 19/32 casos de didgeridoo y 21/32 de flauta. No les asignamos una exactitud ficticia.

### Respuesta y recursos

Ante un salto de registro de factor 2,13, medimos el tiempo desde el cambio de la señal hasta la primera lectura correcta. Con cuatro alineaciones distintas respecto del bloque de captura, la mediana bajó de **440 a 255 ms** en didgeridoo y de **162 a 119 ms** en flauta al acortar la ventana. Para dos lecturas correctas consecutivas bajó de 626 a 440 ms y de 255 a 212 ms. NSDF dio las mismas medianas que producción. El ensayo conserva la cadencia y la fase inicial de captura. No mide latencia física ni dibuja una transición entre dos muestras.

JDK 21, Windows, Intel Core i7-12700KF. Nueve lotes medidos tras cuatro de calentamiento. Tiempos por decisión, CPU del hilo y bytes asignados por decisión; la última columna es la mediana de los p95 de cada lote. No se incluyen síntesis, E/S, arranque, interfaz ni controlador de audio.

| Perfil y ruta | Tiempo medio ms | CPU media ms | Asignación kB | p95 por llamada ms |
|---|---:|---:|---:|---:|
| Didgeridoo · YIN / corta / NSDF | 0,357 / 0,277 / 0,357 | 0,380 / 0,271 / 0,380 | 35,8 / 35,8 / 23,3 | 0,383 / 0,294 / 0,398 |
| Flauta · YIN / corta / NSDF | 0,251 / 0,225 / 0,236 | 0,271 / 0,217 / 0,217 | 67,8 / 43,2 / 21,8 | 0,255 / 0,231 / 0,261 |
| Didgeridoo · flujo actual con formas de onda y FFT | 0,673 | 0,651 | 176,6 | 0,740 |
| Flauta · flujo actual con formas de onda y FFT | 0,557 | 0,543 | 207,0 | 0,643 |

El flujo completo en esta JVM asignó aproximadamente 0,95 MB/s para didgeridoo y 2,23 MB/s para flauta a sus cadencias reales. El contador de CPU de Windows tiene resolución limitada. Las asignaciones no miden memoria residente ni consumo eléctrico. En el análisis **offline**, con dos segundos de PCM sintético en memoria, la repetición YIN costó 0,377 ms y 43,7 kB por ventana; la búsqueda de la componente, 1,991 ms y 393,5 kB. Se comprobó la frecuencia de 68 Hz en ambas rutas. No medimos batería, memoria máxima ni latencia de micrófono en Android.

### Referencia real independiente

Usamos la escala cromática grabada por Sonja Feig para la Universidad de Iowa: doce notas de flauta traversa, sin vibrato, en dinámica mezzoforte. La etiqueta procede de la partitura nominal C5–B5. Se tomaron tres ventanas centrales no superpuestas por nota a partir de su envolvente de amplitud. Los tres métodos emitieron 36/36 lecturas y todas quedaron a 25 cents de la nota nominal. Las medianas fueron 8,0, 8,6 y 8,0 cents para YIN, ventana corta y NSDF. La afinación de la intérprete forma parte de esa distancia. Una sola escala de flauta traversa no valida la flauta nativa americana ni proporciona la frecuencia instantánea de los labios de un didgeridoo. [Grabación, intérprete y condiciones](https://theremin.music.uiowa.edu/MISflute.html)

## Qué aporta la literatura

- **YIN y pYIN.** YIN usa la recurrencia temporal. pYIN explora umbrales y decodifica trayectorias probabilísticas. En el ensayo histórico del proyecto, YIN acertó 250/260 referencias reservadas y pYIN, 238/260, con doce errores de octava y diez falsos positivos en 35 ventanas negativas para pYIN. La corrida de pYIN procesó clips completos con información futura. La referencia Python de librosa tiene licencia ISC. [YIN](https://pubmed.ncbi.nlm.nih.gov/12002874/) · [pYIN](https://www.eecs.qmul.ac.uk/~simond/pub/2014/MauchDixon-PYIN-ICASSP2014.pdf) · [librosa](https://github.com/librosa/librosa) · [Evaluación histórica](PITCH_RESOLUTION.md#quantitative-results)
- **SWIPE′, plantillas y cepstrum.** SWIPE′ contrasta familias espectrales usando la primera componente y armónicos primos. Tras el arreglo del extremo del rango, el SWIPE′ integrado acertó 96/135 referencias reservadas de didgeridoo frente a 125/135 de YIN. El proyecto ya evaluó una combinación de recurrencia y peine armónico que producía consensos falsos. El cepstrum también puede revelar el espaciado armónico, pero por sí solo no identifica cuál de dos fuentes lo produjo. En esta ronda no se ejecutó un cepstrum nuevo. La implementación SPTK se conserva como análisis local opcional, con avisos MIT/BSD. [SWIPE′](https://pubmed.ncbi.nlm.nih.gov/19045655/) · [Implementación y límites](SWIPE_COMPARISON.md)
- **McLeod y Wyvill.** La normalización compensa la energía de ambos segmentos al medir recurrencia. Nuestra implementación directa permite comparar esta hipótesis sin importar una biblioteca, código de Tartini ni un rastreador temporal. Sus costes solo describen esta implementación. [Artículo original](https://www.cs.otago.ac.nz/graphics/Geoff/tartini/papers/A_Smarter_Way_to_Find_Pitch.pdf)
- **CREPE y PESTO.** CREPE estima tono monofónico desde la onda; su referencia usa un modelo entrenado y código MIT. PESTO ofrece modelos pequeños y exportación ONNX, con código LGPL-3.0. La inferencia local en Android requeriría integrar el modelo y su runtime, revisar las condiciones de los pesos y medir energía y latencia. Los resultados publicados sobre voz e instrumentos no resuelven la identificación de la vibración labial durante una vocalización de didgeridoo. Se revisaron ambos métodos, sin ejecutarlos ni incorporarlos al APK. [CREPE](https://arxiv.org/abs/1802.06182) · [Referencia CREPE](https://github.com/marl/crepe) · [PESTO](https://arxiv.org/abs/2508.01488) · [Referencia PESTO](https://github.com/SonyCSLParis/pesto)
- **Articulación.** La impedancia del tracto vocal altera selectivamente la amplitud de los armónicos del didgeridoo. Durante la vocalización pueden coexistir excitaciones de labios y glotis. Cambiar de vocal no implica necesariamente cambiar la frecuencia de los labios. La comparación decisiva sería audio sincronizado con una referencia independiente de vibración labial, con anotaciones de A/E/I/O/U, respiración, toots y silencios. En este estudio no se dispuso de ese corpus. [Mediciones de UNSW](https://newt.phys.unsw.edu.au/jw/didjeridu.html) · [Fletcher y colaboradores](https://phys.unsw.edu.au/music/people/publications/Fletcheretal2006a.pdf)

Para reproducir la réplica y ambos barridos, repetir los dos comandos del protocolo con `replication`, `balance` y `resonators`. En Windows se usa `.\gradlew.bat`. El conjunto `public` verifica la huella de la grabación publicada antes de medir. Los controles de exactitud del informe se ejecutan con `python -m unittest discover -s tools -p test_pitch_study.py`.
