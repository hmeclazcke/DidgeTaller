# Registro de trabajo

## 2026-10-20 — Cierre de Didge Taller 3.1 beta1

- Implementados visor NavHost, matemáticas puras y gestos compartidos, cámaras por sesión, cuatro adaptadores, seguimiento de posición real y conservación al rotar. Laboratorio con portada/destinos y barras fijas; Biblioteca/detalle/acciones/formatos/permiso revisados. No se rehicieron algoritmos ni modelos persistidos: 26 módulos anteriores idénticos al ZIP inicial.
- Auditoría antes/después en emulador: corregidos Marcar fin vertical, herramientas ocultas, escala de F0, cabeceras excesivas, fechas repetidas, falta de pausa en espectro y permiso obsoleto. También se reprodujo un fling corto que eliminaba por velocidad: nuevo veto por distancia al soltar, reversión y Deshacer probados.
- Gates: 57 JVM y APK aprobados. Suite 22/22 más focos completan 24 casos funcionales distintos; matriz 360/420 dp normal/ampliada, horizontal, gesto de borde y tres botones. TalkBack solo smoke parcial. WAV sintético de 900 s con cancelación/zoom real. Precisión audible y rendimiento físico pendientes.
- APK **3.1.0-beta1 / 5**, SHA `a53dacb774288740967e2244214b2684db49997ba6e8d55f9399afd1516d259e`, instalado Samsung SM-G990E mediante install -r/Success, arranque sin fatal. Respaldo fresco en `backups/latest.txt`; 7 WAV/índice/ajustes/relaciones intactos. Dos metadatos automáticos identificados y comprobados. Sin borrado/restauración de datos del usuario ni instrumentación física.
- Archivos principales: ui/ImmersiveViewer, PlotSurface, PlotGestures, ExplorerState, SessionExplorer, TallerApp, TallerNavigation; core/PlotViewport; audio/SessionWorkspace; TallerViewModel/MainActivity; pruebas de viewport, navegación, rotación y auditoría. Documentación de arquitectura en IMMERSIVE_VIEWER, resultados en VALIDATION y UI_AUDIT.
- Recuperación: checkpoint validado indicado en `checkpoints/latest.txt`. Git ausente, sin commits inventados. Próximo paso exacto: prueba manual del usuario en Samsung descrita en VALIDATION; no rehacer fase A ni ejecutar instrumentación sobre el teléfono.

## 2026-10-20 — Checkpoint 3.1: auditoría e integración del visor

- Leídos AGENTS, contexto/documentación, fuentes y pruebas. Sin Git en esta carpeta; respaldo previo ZIP en checkpoints. Línea base: 44 JVM y assembleDebug aprobados.
- Inventario real a 360 dp/fuente 1,3: Medir, denegación, captura silenciosa, foco, diseñador/perfil/simulación, espectro vacío, búsqueda/progreso, guía, listas y detalle. Reproducidos el botón «Marcar fin» vertical y la dificultad de alcanzar pestañas dentro de formularios largos.
- Implementados viewport/proyección puros, gestos compartidos en dos ejes y adaptadores gráficos; visor como destino NavHost, cámara/selección retenidas, navegación de herramientas del Laboratorio, resumen compacto, acciones adaptables, referencia de color fija por exploración. Motores originales y almacenamiento de sesiones sin cambios de esquema.
- Primeras capturas posteriores: 4 gráficos a pantalla completa y «Marcar fin» horizontal. Auditoría visual llevó a compactar la barra para que las herramientas permanezcan visibles.
- Pruebas nuevas encontraron redondeo de ticks logarítmicos en borde (corregido), fixture de outlier fuera del rango válido (corregido), orden incorrecto de pinch en test (corregido) y cierre al dibujar un viewport transitoriamente colapsado durante rotación (corregido, pendiente foco).
- En curso: focos de visor y rotación real de MainActivity, luego matriz visual. No se instaló todavía esta versión en Samsung ni se ejecutaron allí pruebas. Resultados finales se registrarán por separado; no contar las ejecuciones fallidas como aprobadas.


## 2026-10-20 — Implementación integral 3.0 beta1 cerrada

- Se completaron herramientas funcionales de B–F reutilizando A: STFT/onda/espectro interactivos, selección/seek/bucle, comparación A/B, asociaciones diseño/sesión, marcadores manuales, papelera durable/Deshacer y captura progresiva con límite inicial 15 min. No hay Git; checkpoint previo en `checkpoints/`. Comparación binaria confirma 11 archivos de análisis/detector/motores sin cambios.
- Archivos principales nuevos: core AudioVisuals/ProgressiveWavWriter/SpectralPalette; audio SessionPlayer/SessionWorkspace; data AnalysisCache/TrashStore/SessionToolsStore; UI AcousticCharts/SessionExplorer/ComparisonScreen/LibraryTools. Se integraron ViewModel, navegación, tarjetas, persistencia compatible y exportación SAF por bloques en IO. No se añadieron dependencias ni permisos. Claves históricas y fórmulas acústicas/evolución conservadas.
- **44/44 JVM**, APK app/test compilados; **18 casos instrumentados distintos aprobados solo en emulador** mediante suite + focos, no una sola corrida final de 18. Casos de captura real AudioRecord y onStop, navegación/scroll/release, legado, papelera/Deshacer/recarga, cache/hashes, análisis/gráficos/zoom/fragmento/seek/pausa/bucle/comparación, asociación/simulación/marcadores al reabrir. Historial exacto de ejecuciones/fallos en VALIDATION y logs `app/build/`.
- Fallos corregidos: acceso al Application en helper de espacio y tipos de una aserción; error duplicado entre análisis y visualización, scroll de aserción en detalle ampliado; encadenamiento de seeks/bucle corregido coordinando OnSeekCompleteListener con un solo salto pendiente (regresión pasó); onStop no marca como cancelada una visualización terminada. La prueba de asociación espera el cierre del snackbar antes de tocar un control que quedaba detrás; verificó diseño y persistencia real.
- WAV 900 s: 79.380.044 bytes escritos/releídos sin PCM completo en RAM. STFT virtual 900 s: viewport de 262.144 bytes de celdas y ~3,8 s PC; no es medición de batería/AudioRecord sostenido en Samsung. Captura real del emulador guardada en `docs/screenshots/v3-spectrogram-synthetic-66hz.png`, señal sintética identificada.
- Samsung SM-G990E/R5CT21KTQ8W actualizado de forma no destructiva con `install -r`: **Success**; firma/ID compatibles, versión **3.0.0-beta1/4**, arranque Status ok, PID **24724**, sin AndroidRuntime fatal en la consulta. APK final **11287565 bytes**, SHA-256 **0EE9820862E51DEF416859576DC2339E592C409A410E96A69F6A765044650C87**; base.apk instalado coincide. Índice local_data, ajustes y **5 WAV** conservaron SHA-256. Solo cambió IDSCount de una preferencia interna de Android; no se confundió con datos del usuario. Respaldo local verificable en `backups/latest.txt` y DATA_SAFETY. No se instrumentó/desinstaló/limpió/restauró el Samsung ni se inició audio real.
- Documentación actualizada: AGENTS, README, ROADMAP, VALIDATION, arquitectura, AUDIO_ANALYSIS, HARMONICS, DIDGELAB_STUDY y capturas. Nuevas SESSION_EXPLORER/DATA_SAFETY documentan algoritmos, escalas, memoria, cache, reproducción/papelera, respaldo y límites.
- **Continuación exacta:** no hay un fallo de compilación/test pendiente ni una fase B–F esperando implementación básica. Priorizar la prueba manual del WAV físico ~69,6 Hz y PVC, sincronía audible/seek/bucle, grabación sostenida 15 min y accesibilidad; después contraste independiente de audio/simulador y estudio con grabaciones etiquetadas. El bucle no es gapless/sample-accurate; marcadores no son clasificador; reanálisis F0 35–220 Hz. No declarar esas validaciones físicas completadas ni reescribir los motores sin evidencia. Instrumentación siempre y solo emulador; preservar el respaldo y datos del teléfono.

## 2026-10-20 — Validación de A e instalación física autorizada

- Se revisó la integración existente de Pcm16WavReader, SpectralAnalysis, HarmonicAnalysis, WavAnalysis y RecordingAnalysisPanel. Sin errores comprobados en algoritmos: no se cambió producción ni se avanzó fase B. No hay Git para consultar diff.
- Se añadieron dos tests numéricos de 70 Hz y mezcla 70/140/210 con H1 débil, niveles relativos y métricas; se amplió la prueba de WAV recuperado con reproducción/pausa posterior y se añadió prueba UI de progreso/Cancelar sobre archivo sintético largo aislado. 33/33 JVM pasaron; APK debug y de pruebas compilaron. Tres pruebas focalizadas pasaron únicamente en emulator-5554; no se repitieron suites sin una hipótesis nueva.
- Samsung SM-G990E, R5CT21KTQ8W `device`: `install -r` → Success. Inicio Status ok, proceso activo, sin AndroidRuntime en el intervalo inspeccionado. SHA del APK instalado coincide con el local y SHA del índice + 4 WAV sin cambios antes/después. Sin instrumentación, limpieza, desinstalación, grabación ni cambios en sesiones físicas. El equipo estaba en reposo/NotificationShade; interacción visual y audio audible quedan para el usuario.
- Se documentaron tolerancias, comandos/hash y pasos para abrir su grabación de 69,6 Hz, encontrar Analizar grabación debajo de Audio y reproducir después. Véase VALIDATION. El APK sigue 2.1.0-beta1/3, ahora con fase A instalada en Samsung.
- Esta tarea termina en validación/instalación. B queda pendiente y requiere una tarea posterior; no iniciar nuevas visualizaciones durante esta entrega.

## 2026-10-20 — Didge Taller 3.0, fase A cerrada

- Se inspeccionó el proyecto existente, incluyendo la navegación ya corregida; línea base Gradle aprobada. La carpeta no tiene `.git`: no hubo diff ni commits. Se organizó A–F y sus dependencias al inicio de ROADMAP; se conserva el límite de captura hasta implementar WAV progresivo.
- Núcleo nuevo: `Radix2Fft` extraído de la FFT propia existente, `Pcm16WavReader` de solo lectura por bloques, `SpectralAnalysis`/`PowerSpectrum`, `HarmonicAnalysis` y `WavAnalysis`. Hann periódica, PSD digital de un lado, promedio ponderado con cola parcial, picos interpolados, candidatos con tolerancias y métricas explícitas. No se portó código externo ni se añadieron dependencias. `PitchDetector` conserva su método de periodicidad y puede omitir gráficos; `SessionAnalysis` admite rango explícito manteniendo defaults/formatos.
- Integración real: `TallerViewModel` ejecuta reanálisis cancelable y rechaza callbacks obsoletos; `RecordingAnalysisPanel` ofrece Analizar grabación, resultados/errores/progreso sobre el WAV completo, incluso recuperado. Centroide y bandas se precalculan en worker. Resultado solo en memoria; WAV, índice y resumen anterior se conservan. No hubo migraciones ni cambios en simulación/evolución.
- Pruebas: **31 JVM** y **10 instrumentadas solo en emulador**; después del ajuste de métricas en worker se repitió el caso recuperado (1/1). Compilación final APK app/test exitosa. Se capturó UI real sobre seno sintético de 66,1 Hz / 20 s, identificado como tal. Hash/comandos/tolerancias en VALIDATION. Esta fase no instaló ni ejecutó nada en Samsung.
- Documentación nueva: AUDIO_ANALYSIS (pipeline/unidades/rendimiento), HARMONICS (criterios/ambigüedad), ACOUSTIC_VALIDATION (protocolo y referencias pendientes). Actualizados arquitectura, README, AGENTS, roadmap, estudio del motor y capturas. SciPy no está instalado; no se atribuye comparación externa ejecutada.
- **Reanudación exacta:** empezar fase B por un espectro interactivo grande desde `recordingAnalysis.result.spectrum` y sus candidatos, sin recomputar en Compose; ejes Hz/dB relativos, selección de pico, zoom/pan y accesibilidad. Después envelope min/max de onda y STFT por tiles con caché limitada. Mantener el flujo de un botón, Navigation Compose y almacenamiento. Para 15 minutos, implementar F1 antes de tocar el límite; no aumentar solo MAX_RECORD_SECONDS. No correr instrumentación en el Samsung.

## 2026-10-20 — Atrás desde el detalle de sesión

- Causa corregida: `selectedId` reemplazaba visualmente Biblioteca sin crear back stack. Se retiró y se agregó Navigation Compose 2.9.0 con rutas de las tres secciones y `session/{id}`; NavController reemplaza `vm.tab` como fuente de navegación. Abrir un diseño sigue llevando al diseñador.
- Lista y detalle tienen scroll propio guardado por la entrada de navegación. La flecha de la barra superior y Atrás del sistema quitan el mismo detalle. Los cambios de destino detienen captura/reproducción; MediaPlayer libera recursos incluso ante un error de `stop()`. Cambiar de sección descarta el detalle abandonado y conserva el estado de la lista.
- La prueba ampliada a la última tarjeta detectó un salto de 6 px por las alturas distintas de las barras superiores. Se unificaron con `TopAppBar` Material y se mantuvo ese caso en la prueba de regresión.
- Se agregaron tres pruebas de UI con WAV sintético y datos aislados: Atrás Android, flecha, y cambio de sección con reproducción. Verifican offset de scroll, pila, actividad activa y reproductor detenido. Primer intento falló por Espresso 3.5.0 y API 37; se fijó Espresso 3.7.0 con el arreglo oficial. Resultado final: 10/10 JVM, 7/7 instrumentadas solo en emulador, APK debug compilado. Captura real nueva en `docs/screenshots/sesion-navegacion-emulador.png`.
- No se modificaron motores acústicos, modelos persistidos ni registros existentes. Pendiente validación manual del gesto predictivo y audio audible en Samsung. Detalles y hash del APK en `VALIDATION.md`.
- Se actualizó el Samsung con `install -r`, usando la autorización previa y verificando primero que la app no estuviera en primer plano. `Success`, hash del APK instalado idéntico al compilado y checksum de `local_data.xml` sin cambios. Ninguna prueba instrumentada se ejecutó en el teléfono.

## 2026-10-20 — Medir y sesiones integradas

- A pedido explícito del usuario se instaló el APK final en Samsung SM-G990E con `adb -s R5CT21KTQ8W install -r`: `Success`. La versión pasó de `2.0.0-beta1`/2 a `2.1.0-beta1`/3; la actividad inició y quedó un proceso activo. No hubo instrumentación ni limpieza de datos en el teléfono; la interacción musical sigue pendiente.
- Continuación tras la interrupción: se revisó el código y se añadió recuperación no destructiva de WAV privados con UUID que no tenían entrada en el índice. Biblioteca los muestra como «Audio recuperado», sin nota ni serie temporal inventadas; renombrarlos incorpora sus metadatos al índice. Se añadió cobertura instrumentada de recuperación y compatibilidad.
- La primera ejecución instrumentada detectó una expectativa errónea del test sobre el orden por fecha ante archivos de prueba anteriores. Se seleccionó el registro legado por ID y se aisló la limpieza a los datos propios de los tests. Resultado final: 10/10 pruebas JVM, 4/4 instrumentadas solo en emulador y APK debug compilado (`2.1.0-beta1`, SHA-256 en `VALIDATION.md`).
- Se tomó línea base Windows: siete pruebas JVM y `assembleDebug` aprobados con JBR 21/SDK 35. `git status` confirmó que esta carpeta no contiene `.git`, por lo que no se pueden hacer commits aquí.
- Se reemplazó Afinar por Medir: un único botón inicia AudioRecord, lectura y WAV; el mismo detiene y guarda automáticamente. También se cierra a los 60 s, al navegar fuera y en `onStop`. Se quitó la escucha automática. Se mantuvo Laboratorio.
- `SessionAnalysis.kt` calcula nota predominante y mediana de Hz desde lecturas válidas, rango percentil 10/90 y estabilidad. `RecordingStore` conserva WAV/índice anteriores y añade resumen/serie temporal a sesiones nuevas; las lecturas antiguas quedan separadas y visibles.
- Biblioteca ahora presenta Sesiones y Diseños; el detalle suma gráfico de Hz en el tiempo, cursor aproximado de reproducción, controles de audio y acciones de archivo. No se cambiaron fórmulas de `AcousticModel` ni `PitchDetector`.
- El APK `2.1.0-beta1` y 10 pruebas JVM pasaron; cuatro pruebas instrumentadas aisladas pasaron **solo en emulador**. Se recorrió una sesión de silencio de 14 s con WAV privado y detalle, cierre automático de 60 s, guardado al salir, persistencia tras reapertura, Vista amplia y 900×1800 con fuente 1,3×. Se verificó renombrar, exportar por SAF y borrar conservando la copia. El emulador no permitió validar nota ni audio físico; pendiente contraste en teléfono con didgeridoo, sonido audible, accesibilidad, rotación y sincronía precisa.
- Política: jamás instrumentación ni operaciones que puedan borrar datos sobre el Samsung real sin autorización explícita. Las capturas nuevas son reales del emulador y muestran silencio.

## 2026-10-20 — Navegación, afinador, grabaciones y biblioteca

- Se leyó la documentación, los módulos Kotlin y las pruebas; se estableció línea base con siete tests JVM y APK debug.
- `TallerApp.kt`: navegación Material 3 de tres secciones; afinador de gran lectura con estados y modo de máxima legibilidad; Biblioteca dividida por tipo; Laboratorio conserva Diseñar, Espectro, Buscar y Guía. Se preservó el motor acústico.
- `MicrophoneEngine.kt`: grabación manual hasta 60 s y entrega del WAV al detener; `RecordingStore.kt`: WAV privado e índice persistente. `TallerViewModel.kt`: estado de escucha, guardado contextual, reproducción, pausa y gestión de registros. `LocalStore.kt`: IDs/cents nuevos y lectura de JSON antiguo. `MainActivity.kt`: reanudación optativa, cese en `onStop` y exportación SAF tras rotación.
- El editor conserva puntos personalizados al mover longitud, boquilla, diámetro intermedio o campana. La lista de diseños no recalcula simulaciones.
- Se añadieron dos pruebas instrumentadas aisladas para persistencia; pasaron en Samsung SM-G990E. Las siete pruebas JVM y el APK debug también pasaron. El primer intento instrumentado falló por contexto de escritura incorrecto; se corrigió.
- Incidencia: `connectedDebugAndroidTest` dejó la app desinstalada; al reinstalarla, no había `local_data`. El usuario confirmó que las dos mediciones reales estaban en este teléfono. No repetir pruebas instrumentadas allí. Se buscaron exportaciones por nombre en Descargas/Documentos sin resultados; el usuario eligió no reconstruir los valores. Detalles en `VALIDATION.md`.
- Quedan pendientes la interacción visual con el teléfono desbloqueado, audio físico, exportación real, accesibilidad, rotación y contraste con afinador independiente. Ninguno se declara validado.
- En emulador Android se verificaron navegación, estados de permiso y silencio, recorrido WAV completo, persistencia tras cierre, exportación a Descargas y optimización visible. La pantalla chica con fuente 1,3× reveló que el control de escucha quedaba fuera de vista; ahora escucha y guardar medición quedan fijos sobre la barra de navegación. Se reinicia el scroll al cambiar de sección.
- El APK actual tiene 10.485.510 bytes y se instaló con `adb install -r` en el Samsung; la versión visualmente idéntica anterior se probó en el emulador. El Samsung seguía bloqueado, por lo que no se obtuvieron capturas suyas. Hash y evidencia en `VALIDATION.md`. Se limpiaron los XML temporales de inspección y se conservaron capturas PNG reales en `docs/screenshots/`.
- `LocalStore` ya no descarta automáticamente las mediciones antiguas después de 200 entradas ni los diseños después de 100; las siete pruebas JVM y `assembleDebug` pasaron tras el cambio. Las dos pruebas de persistencia instrumentadas pasaron antes de este ajuste; no se repitieron en el teléfono de uso.
- `git status` devolvió “not a git repository” en esta carpeta; no fue posible crear commits pequeños desde este entorno.

## 2026-10-19 — Migración inicial Kotlin+Compose

- Se creó nuevo proyecto limpio preservando applicationId `ar.didgetaller`.
- Se portó a Kotlin independiente el núcleo `Music`, `PitchDetector`, `AcousticModel` y `WavCodec`, y se probaron con JVM real.
- Se implementaron cinco pantallas Compose Material 3, ViewModel y captura AudioRecord en hilo dedicado.
- Se preservaron claves JSON y SharedPreferences del antiguo proyecto Java.
- Se agregó bootstrapping de `gradle-wrapper.jar` con verificación SHA oficial, tests JUnit, y workflow CI.
- Bloqueado: SDK/Gradle Android no instalados en este entorno. Primera acción en Windows: sincronizar y compilar.
- Siguiente paso: fase P0 del ROADMAP.

## 2026-10-19 — DidgeLab personal
- Corregido alcance: uso personal NO COMERCIAL.
- Motor acústico: perfil arbitrario, propagación 1D con pérdidas y radiación aproximadas, cálculo de picos.
- Motor de evolución para 3 picos; interfaz Buscar, editor por puntos y retrocompatibilidad JSON de diseños.
- Validado núcleo puro JVM y generación de candidatos. Falta Android Studio build, UI y pruebas físicas.
- Referencia: `docs/DIDGELAB_STUDY.md`, DidgeLab CC BY-NC-SA 4.0; código Kotlin de implementación independiente.

## 2026-10-19 — Primera compilación Android verificada en Windows

- Se comprobó JDK 17.0.20.1, Android SDK Platform 35 y Build Tools 35.0.0. Se usó JDK 17 porque el JBR de Android Studio disponible es 25.
- El wrapper descargó su JAR oficial, verificó el SHA-256 y ejecutó Gradle 8.13. No se cambió ninguna versión ni código fuente: AGP 8.13.2 y Gradle 8.13 compilaron correctamente pese a diferir de las versiones escritas en `AGENTS.md`.
- `:app:testDebugUnitTest` aprobó 7/7 pruebas; `:app:test` aprobó las mismas 7 en debug y release; `:app:assembleDebug` generó el APK. Los primeros intentos tropezaron con restricciones de red/caché del sandbox; el registro exacto está en `docs/VALIDATION.md`.
- Archivos modificados: documentación de validación, hoja de ruta y registro de trabajo. El JAR del wrapper y los artefactos de Gradle se generaron al compilar.
- Siguiente paso P0: conectar o iniciar un Android, pedir autorización antes de instalar en un dispositivo físico y verificar permisos, audio, WAV, CSV, navegación y diseños.
