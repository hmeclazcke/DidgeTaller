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

## Fase 2 · Armónicos dominantes, respuesta temporal y redes neuronales

**Protocolo previo a la evaluación.** El ensayo nuevo usa diez grupos de desarrollo y 28 grupos reservados por perfil. Cada grupo tiene una frecuencia y fases propias. Incluye armónicos 2, 3 y 5 con fundamental débil, fundamental ausente, envolvente variable y controles sin fuente identificable. Para didgeridoo se sortean frecuencias entre 65 y 75 Hz. Los saltos incluyen cambios de octava y registros de factor 2,8 dentro del rango extendido. El PCM, los grupos y los umbrales son reproducibles. También se contrastan los candidatos con las señales de 44,1 kHz del conjunto reservado anterior, el barrido de balances, los resonadores y la flauta pública. [Generador](../tools/pitch-study/PhaseTwo.kt)

Evaluamos el YIN de producción, una ventana adaptativa y un selector que contrasta los mínimos de YIN con un peine armónico. Contrastamos además CREPE tiny y full y PESTO. Los tres modelos conservan sus salidas nativas para inspección. Para la tabla principal se aplica el rango del perfil, RMS mínimo de 0,005 y umbral de puntuación neuronal de 0,5. No se ajustan los umbrales con el conjunto reservado. La fuente latente de las señales ambiguas queda fuera del denominador de exactitud. Se informan aparte las lecturas que cada método emitiría en esas condiciones.

Para considerar un cambio general buscamos ganancias reproducibles en aciertos sobre todos los casos o en latencia, sin aumentar falsas detecciones en ruido, perjudicar un perfil ni introducir subarmónicos en controles simples. La viabilidad en Android necesita medición en Android. Una mejora limitada a mezclas sintéticas justifica contrastarla con una referencia instrumental independiente. La calibración ajusta intervalos de puntuación únicamente en desarrollo. Los intervalos de incertidumbre remuestrean grupos completos de frecuencias, evitando tratar todas las ventanas como independientes.

### Por qué se pierde una fundamental de 68 Hz

Con un quinto armónico dominante cerca de 340 Hz, la onda se parece a sí misma tras unos 2,94 ms. Los componentes más débiles distinguen esa repetición parcial del período completo de 14,71 ms. El primer mínimo aceptable de YIN puede favorecer una recurrencia parcial o alguno de sus múltiplos dentro del rango de búsqueda. El resultado también puede errar por una quinta musical. En el control analítico, una línea de 340 Hz y otra de 408 Hz permitieron recuperar 68 Hz. Una línea aislada de 340 Hz tiene exactamente las mismas muestras tanto si se genera directamente como si representa el único armónico audible de una fuente de 68 Hz. La oscilación física de esa fuente es indeterminable a partir de esas muestras.

El selector experimental divide cada pico espectral significativo por órdenes de 1 a 12 y contrasta el resultado con **todos** los mínimos locales de YIN. Refina la frecuencia con los picos concordantes y suma su amplitud. Para corregir una lectura exige al menos dos órdenes cuyo máximo común divisor sea 1, un 90 % de amplitud explicada entre los picos retenidos y una ventaja de al menos 3,5 puntos porcentuales frente a la hipótesis rival. Si falta evidencia conserva la decisión original; su puntuación permite estudiar qué ocurriría al abstenerse. Son dos descripciones de la misma onda, no dos mediciones físicas independientes. [Implementación](../tools/pitch-study/HarmonicHypotheses.kt) · [Suma subarmónica de Hermes](https://pubmed.ncbi.nlm.nih.gov/3343445/)

**Contraejemplo reproducible.** Dos componentes impuestas a 332,57 y 406,21 Hz casi coinciden con los órdenes 9 y 11 de 36,94 Hz. El selector asignó 36,94 Hz con puntuación 0,913. Ninguno de los dos osciladores tenía esa frecuencia. En 28 de 84 mezclas desafinadas de didgeridoo y 33 de 84 de flauta, el selector cambió la lectura más de un 4 % y le dio al resultado una puntuación ≥0,9. Por eso el hallazgo útil para diagnóstico es **qué picos sostienen una hipótesis**. La puntuación no identifica al oscilador de los labios durante una vocalización.

### Métodos neuronales ejecutados

| Implementación exacta | Entrada y decisión en este ensayo | Alcance |
|---|---|---|
| CREPE tiny y full, torchcrepe 0.0.24 | Pesos originales convertidos a PyTorch. Remuestreo de la ventana observada a 16 kHz, últimas 1.024 muestras (64 ms), promedio local ponderado alrededor del máximo, sin Viterbi. Búsqueda entre 32 y 2.006 Hz. | Una lectura por ventana entregada. Su centro está unos 32 ms antes del final. La semilla fija incluye el dithering del conversor de bins. |
| PESTO 2.0.1, `mir-1k_g7` | Pesos incluidos en la distribución oficial. PCM a 44,1 kHz, CQT centrada de 8.192 muestras y salto de 441, lectura más cercana al centro de la ventana. El banco desplegado va aproximadamente de 27 a 3.322 Hz; la salida usa una grilla MIDI más amplia. | Variante offline sobre un clip disponible completo. Su respuesta se contabiliza al entregar el clip y su tiempo de CPU incluye todos los pasos de 10 ms que calcula en él. |

El código de CREPE y torchcrepe declara MIT. El paquete oficial de PESTO declara LGPL-3.0. Se comprobaron los pesos contra SHA-256 antes de ejecutar. En la rama de pesos original de CREPE y en el directorio de pesos de PESTO no encontramos un aviso adicional específico de redistribución. Las pruebas se ejecutaron localmente, sin distribuir pesos. El uso offline es posible tras obtenerlos. Una integración Android necesitaría validar la conversión del modelo, el frontend espectral o remuestreador, su licencia de distribución y el presupuesto de memoria y energía. [CREPE y pesos originales](https://github.com/marl/crepe/tree/models) · [Procedencia de la conversión](https://github.com/maxrmorrison/torchcrepe) · [PESTO y pesos](https://github.com/SonyCSLParis/pesto) · [Runner y huellas](../tools/pitch-study/neural.py)

### Reproducción de la fase 2

1. Usar JDK 21 y el wrapper del proyecto. Para cada valor de `SPLIT` en `dev`, `test`, `old` y `flute`, ejecutar `./gradlew :engineering-tools:pitchStudy -PpitchSplit=phase2-SPLIT`. El último usa el AIFF público fijado en la fase 1. El control analítico se ejecuta con `-PpitchSplit=phase2-check`. Los archivos locales de PCM contienen únicamente los estímulos del generador y, en el conjunto `flute`, las ventanas de esa referencia pública.
2. En un entorno virtual de investigación con Python 3.12, instalar `torch==2.8.0 torchaudio==2.8.0` desde el índice CPU de PyTorch y luego `torchcrepe==0.0.24 pesto-pitch==2.0.1 psutil==7.2.2`. La corrida usó NumPy 2.5.5 y SciPy 1.18.1. Ninguna dependencia se incorpora a Gradle ni al APK.
3. Desde la raíz ejecutar `python tools/pitch-study/neural.py MODELO dev` y, en otro proceso, `python tools/pitch-study/neural.py MODELO test old flute`, para cada modelo (`tiny`, `full`, `pesto`). Así también se reproduce la semilla del decodificador de CREPE. Ejecutar `python tools/pitch-study/streaming.py` para el ensayo causal de PESTO. Para cada conjunto, ejecutar `python tools/phase_two_scores.py SPLIT`. Los CSV de decisiones, subgrupos, cobertura, errores de razón, calibración, latencia y costes quedan en `build/reports/pitch-study/phase2/`.

### Exactitud y cobertura sobre frecuencias conocidas

Cada perfil tiene 252 señales reservadas con una fundamental inferible, 252 controles con fuente ambigua y una serie aparte de cambios de registro. Las siguientes columnas cuentan aciertos sobre **las 252 señales inferibles**, incluidas las abstenciones en el denominador. Los errores en cents se calculan solo para las lecturas emitidas. Estas señales se eligieron para exponer el fallo armónico. Sus proporciones de acierto no estiman la exactitud de la app durante una práctica musical.

| Perfil | Detector | Emitidas | Aciertos a 10 / 25 / 50 cents | Error mediano / p95 (cents) | Errores de octava |
|---|---|---:|---:|---:|---:|
| Didgeridoo | YIN actual | 252 | 45 / 45 / 45 | 701,4 / 1205,1 | 49 |
| | Adaptativo | 252 | 45 / 45 / 45 | 701,4 / 1205,1 | 49 |
| | Selector armónico | 252 | 224 / 224 / 224 | 0,2 / 694,5 | 3 |
| | CREPE tiny | 67 | 20 / 27 / 27 | 1190,9 / 1214,2 | 40 |
| | CREPE full | 131 | 77 / 99 / 107 | 8,5 / 1217,6 | 24 |
| | PESTO centrado | 66 | 10 / 27 / 33 | 616,7 / 1232,8 | 33 |
| Flauta | YIN actual | 244 | 107 / 107 / 107 | 380,7 / 1937,5 | 8 |
| | Adaptativo | 244 | 133 / 133 / 133 | 0,7 / 1924,0 | 8 |
| | Selector armónico | 244 | 243 / 243 / 243 | <0,1 / 0,1 | 0 |
| | CREPE tiny | 162 | 67 / 101 / 102 | 11,3 / 2769,2 | 23 |
| | CREPE full | 180 | 97 / 130 / 132 | 9,5 / 1914,7 | 23 |
| | PESTO centrado | 147 | 68 / 105 / 113 | 12,4 / 1919,6 | 15 |

Para el selector frente a YIN, el incremento de aciertos a 50 cents fue de **71,0 puntos porcentuales** en didgeridoo (intervalo bootstrap pareado del 95 %: 67,5–74,2) y **54,0 puntos** en flauta (48,4–59,5). El error mediano absoluto de didgeridoo bajó de 36,45 Hz a menos de 0,05 Hz, pero el p95 del selector siguió en 34,3 Hz. En flauta bajó de 103,6 Hz a menos de 0,05 Hz. Los intervalos reflejan variación entre frecuencias de este generador.

| Armónico dominante del didgeridoo | Fundamental débil | Fundamental ausente | Envolvente variable |
|---|---:|---:|---:|
| Segundo | 4 → 28 | 1 → 28 | 23 → 24 |
| Tercero | 1 → 28 | 0 → 28 | 16 → 21 |
| Quinto | 0 → 28 | 0 → 28 | 0 → 11 |

Cada celda indica aciertos de YIN → selector sobre 28 frecuencias. Con el quinto armónico reforzado y envolvente variable quedaron **17 fallos de 28**. En el conjunto de didgeridoo los errores cerca de las razones 3:2 y 5:3 pasaron de 67 y 63 a 7 y 5. El desglose reproducible incluye las demás razones, Hz y percentiles por categoría.

**Comprobación externa al nuevo generador.** En los estímulos congelados a 44,1 kHz hubo 182 ventanas con frecuencia conocida y 156 negativas por perfil. YIN → selector pasó de 89 a 178 aciertos en didgeridoo y de 77 a 166 en flauta. Ambos emitieron en las mismas 44 y 10 ventanas negativas, respectivamente, todas de ruido resonante. CREPE tiny, full y PESTO produjeron respectivamente 25, 17 y 58 falsos positivos en didgeridoo y 15, 3 y 92 en flauta. La frecuencia y la periodicidad de YIN coincidieron con los resultados archivados en las 735 ventanas anteriores y las 36 de la flauta pública. En esa grabación, los seis métodos acertaron las 36 notas nominales a 50 cents. A 25 cents, tiny acertó 35 y PESTO 28; los otros cuatro acertaron las 36. No hay una referencia independiente equivalente de labios de didgeridoo.

### Cuándo confiar en una lectura

| Puntuación mínima | Cobertura / acierto entre lecturas, didgeridoo | Cobertura / acierto entre lecturas, flauta |
|---|---:|---:|
| YIN, sin filtro adicional | 100 % / 17,9 % | 96,8 % / 43,9 % |
| YIN, 0,95 | 50,4 % / 35,4 % | 76,6 % / 55,4 % |
| Selector, sin filtro adicional | 100 % / 88,9 % | 96,8 % / 99,6 % |
| Selector, 0,75 | 81,3 % / 100 % | 89,7 % / 100 % |
| Selector, 0,95 | 35,3 % / 100 % | 64,7 % / 100 % |

La tabla solo incluye las señales inferibles. Al calibrar por intervalos sobre desarrollo, la puntuación de Brier en didgeridoo pasó de 0,720 a 0,090 para YIN y de 0,022 a 0,010 para el selector (menor es mejor). En flauta, el selector calibrado dio 0,009, peor que usar la proporción constante aprendida en desarrollo (0,004). La calibración se condicionó a que hubiera lectura y referencia conocida.

En los controles con una línea o solo armónicos pares, YIN y el selector recuperaron la **repetición observable** en todos los casos dentro del rango nominal (64/64 y 87/87). Aun así, en 54/84 ejemplos de armónicos pares de didgeridoo y 51/84 de flauta, el selector dio una puntuación ≥0,9 aunque esos armónicos admitían distintas fuentes físicas. Las mezclas desafinadas descritas arriba también atravesaron ese umbral. La puntuación permite ordenar hipótesis en un conjunto controlado; comunicarla como probabilidad de haber identificado la vibración labial sería injustificado.

### Cambios de registro y coste

La ruta adaptativa comprueba si el período largo sigue explicando las últimas tres oscilaciones (al menos 31 ms). Si la diferencia normalizada supera 0,28, contrasta la última media ventana y exige periodicidad ≥0,85. En un salto exacto de octava, el período antiguo sigue siendo múltiplo del nuevo. La discrepancia puede permanecer pequeña y el detector no cambia de ventana. La mejora histórica al acortar **todas** las ventanas no se reprodujo con esta regla adaptativa.

| Primera detección correcta tras el salto, mediana a la cadencia de la aplicación | Didgeridoo ×2 | Didgeridoo ×2,8 | Flauta ×2 |
|---|---:|---:|---:|
| YIN / adaptativo | 483 / 483 ms | 426 / 426 ms | 165 / 165 ms |
| Selector armónico | 450 ms | 404 ms | 195 ms |
| CREPE tiny / full | 113 / 113 ms | 139 / 117 ms | 95 / 96 ms |
| PESTO centrado | 298 ms | 269 ms | 165 ms |

Son tiempos de disponibilidad del audio desde el cambio conocido, antes del cómputo. Cada método detectó 14/14 cambios en cada grupo de didgeridoo y 28/28 en flauta, salvo PESTO centrado (27/28). El CSV también exige dos lecturas consecutivas para una segunda medida. CREPE usa solo los últimos 64 ms y responde antes, pero pierde cobertura sobre los armónicos dominantes. La CQT centrada de PESTO analiza el clip completo.

| Coste por ventana, didgeridoo / flauta | Mediana de reloj (ms) | Media de CPU (ms) | Asignaciones (kB decimales) |
|---|---:|---:|---:|
| YIN | 0,40 / 0,29 | 0,53 / 0,43 | 36 / 68 |
| Adaptativo | 0,39 / 0,28 | 0,41 / 0,32 | 37 / 69 |
| Selector armónico | 1,11 / 0,65 | 0,97 / 0,67 | 110 / 107 |
| CREPE tiny | 6,57 / 6,65 | 5,78 / 6,63 | 1926 / 1831 |
| CREPE full | 107,64 / 108,14 | 103,96 / 104,67 | 11340 / 11263 |
| PESTO centrado | 128,25 / 67,01 | 123,16 / 65,22 | 16198 / 8739 |

Medido en el equipo de escritorio de la fase 1. Para Kotlin se midieron bytes asignados por el hilo. Para PyTorch se sumaron asignaciones positivas de tensores en una ventana representativa por tamaño. La memoria residente del proceso calentado fue de 362 MB para tiny, 539 MB para full y 393 MB para PESTO. Los tamaños incluyen el entorno Python y sus bibliotecas. Quedan sin medir el consumo energético y el coste de un runtime optimizado en Android.

**PESTO causal.** También se ejecutó el modelo oficial en modo streaming, sin reflexión de muestras futuras, en bloques consecutivos de 441 muestras. Se reconstruyó el inicio de cada estímulo sintético y se verificó su coincidencia muestra por muestra con el tramo anterior al cambio. El modelo recibió al menos 928 ms de la nota inicial. Se exigió puntuación ≥0,5 y amplitud eficaz centrada ≥0,005 por bloque. [Ensayo continuo](../tools/pitch-study/streaming.py)

| Registro | Nota anterior correcta durante los últimos 40 ms | Llegó a la nota nueva | Primera / segunda lectura correcta, cadencia 10 ms | Primera / segunda, cadencia de la app |
|---|---:|---:|---:|---:|
| Didgeridoo ×2 | 0/14 | 14/14 | 98 / 108 ms | 157 / 343 ms |
| Didgeridoo ×2,8 | 0/14 | 14/14 | 93 / 103 ms | 185 / 371 ms |
| Flauta ×2 | 28/28 | 27/28 | 116 / 126 ms | 165 / 258 ms |

PESTO consumió alrededor de 3,5 ms de CPU por bloque de 10 ms. Reconoció pronto el registro alto de didgeridoo, pero en ninguna de las 28 pruebas siguió correctamente el dron durante los 40 ms anteriores. Informar solo la latencia posterior daría una impresión equivocada de su capacidad para seguir la transición completa.

**Decisión de la fase 2.** Mantener YIN en producción. El selector armónico merece conservarse como hipótesis de análisis offline: resuelve muchos casos sintéticos con armónicos dominantes, pero puede atribuir con seguridad aparente una fundamental ajena a los dos osciladores presentes. La ventana adaptativa ensayada no adelantó la mediana de las transiciones. CREPE full respondió rápido y mejoró la exactitud de algunos casos difíciles frente a YIN, con menor cobertura y mayor coste. El resultado tampoco justifica desplegar PESTO para el dron grave. La siguiente evidencia decisiva sería un corpus consentido con referencia independiente y sincronizada de vibración labial y vocalizaciones, además de mediciones de coste en Android si se propone un candidato para producción. No se accedió a sesiones privadas ni se instaló una aplicación nueva.
