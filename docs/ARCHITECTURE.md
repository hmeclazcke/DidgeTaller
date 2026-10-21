# Arquitectura

## Presentación y ciclo de vida 3.1

[IMMERSIVE_VIEWER.md](IMMERSIVE_VIEWER.md) describe los módulos nuevos. PlotViewport/PlotProjection/PlotScales son matemática de visualización pura; ExplorerState retiene estado pequeño en ViewModel. PlotSurface comparte ejes/cursor y cuatro adaptadores; PlotGestures consume la región inmersiva sin scroll contenedor. SessionWorkspace conserva cálculo/caché y difiere nuevos renderizados al fin del gesto. Los destinos viewer y lab/tool pertenecen al NavHost; el ID de sesión determina cuándo liberar recursos. MainActivity distingue configuración del paso al fondo. No hay cambios de esquema ni algoritmos acústicos.

```text
app/src/main/java/ar/didgetaller/
  MainActivity.kt            permisos runtime, documentos SAF, lifecycle
  TallerViewModel.kt         estado Compose, audio, simulación asíncrona, persistencia
  audio/MicrophoneEngine.kt  AudioRecord, hilo de lectura, captura WAV
  core/Music.kt             afinación, etiquetas y cents (Kotlin/JVM puro)
  core/PitchDetector.kt     detección YIN-like + FFT (Kotlin/JVM puro)
  core/SessionAnalysis.kt   resumen robusto de las lecturas de una sesión (Kotlin/JVM puro)
  core/Radix2Fft.kt         FFT existente compartida por vivo y reanálisis
  core/Pcm16WavReader.kt    MonoPcmSource y acceso WAV por bloques, solo lectura
  core/SpectralAnalysis.kt PSD Hann, escala digital, centroide de potencia y bandas
  core/HarmonicAnalysis.kt picos, órdenes candidatos y desvíos
  core/WavAnalysis.kt      análisis determinista de intervalos, cancelación y progreso
  core/AcousticModel.kt     modelo aproximado de impedancia 1D (Kotlin/JVM puro)
  core/WavCodec.kt          formato PCM WAV (Kotlin/JVM puro)
  data/LocalStore.kt        SharedPreferences+JSON, compatibilidad Java v1
  data/RecordingStore.kt    sesiones: WAV privados + índice JSON en SharedPreferences
  ui/TallerApp.kt           UI Compose Material 3, Canvas, controles
  ui/TallerNavigation.kt    rutas y navegación entre secciones con Navigation Compose
  ui/RecordingAnalysisPanel.kt resultados reales de reanálisis (fase A)
app/src/test/.../CoreTest.kt pruebas de núcleo con JUnit 4
app/src/test/.../AudioAnalysisTest.kt DFT/PSD/armónicos/periodicidad/cancelación
app/src/test/.../WavReaderTest.kt lectura por bloques y formatos dañados
app/src/androidTest/.../LocalPersistenceTest.kt pruebas aisladas de persistencia Android
app/src/androidTest/.../SessionNavigationTest.kt navegación/reproducción y reanálisis UI aislados
```

## Flujo principal vigente — 3.0 beta1

El mapa anterior conserva los módulos originales. Se añadieron `core/AudioVisuals`, `ProgressiveWavWriter`, `SpectralPalette`; `audio/SessionWorkspace`, `SessionPlayer`; `data/AnalysisCache`, `TrashStore`, `SessionToolsStore`; `ui/AcousticCharts`, `SessionExplorer`, `ComparisonScreen`, `LibraryTools`. No hay dependencias/permisos nuevos. Arquitectura, algoritmos, unidades y límites completos en [SESSION_EXPLORER.md](SESSION_EXPLORER.md).

Medir abre AudioRecord solo por acción del usuario. El hilo de audio escribe bloques PCM a UUID.part, actualiza cabecera, calcula periodicidad existente y publica lecturas agregadas. Detener/límite inicial de 15 minutos/navegar/onStop finaliza y renombra WAV; el executor de sesión persiste resumen y serie. No guarda todo el PCM en RAM. La recuperación solo repara parciales propios, sin inventar lecturas.

El reanálisis de fase A conserva executor/token cancelable; pasa por AnalysisCache (identidad SHA-256/versión/intervalo) y produce un resultado con métricas precalculadas. SessionWorkspace usa coroutines Default/IO para leer el viewport, generar min/max o STFT por bloques y publicar una imagen/datos reducidos. No se recalcula FFT al actualizar el cursor. Comparación y simulación asociada también corren fuera del hilo principal.

SessionPlayer prepara MediaPlayer asíncronamente. La UI consulta currentPosition (100 ms); el bucle revisa esa misma posición (40 ms) y realiza seek, sin reloj de avance independiente ni promesa gapless. Se libera al cambiar destino/onStop. Exportar usa SAF con copia de archivo por bloques en IO.

NavHost mantiene `measure`, `lab`, `library`, `session/{id}` y `compare`. La entrada de Biblioteca conserva rememberScrollState; detalle/comparador se apilan encima. Flecha y Atrás del sistema hacen pop de la misma entrada. El listener de destinos detiene audio/cancela análisis y limpia el workspace. No existe BackHandler añadido como parche.

Las claves históricas se preservan. Papelera añade tombstones: oculta, permite Deshacer/restaurar tras reinicio y solo purga ante confirmación explícita. Las relaciones/ajustes/anotaciones usan preferencias separadas aditivas. Un purgado interrumpido conserva el tombstone para completar limpieza. WAV sin índice siguen accesibles como recuperados; su duración se lee de cabecera real, no se asume 44100 Hz. [DATA_SAFETY.md](DATA_SAFETY.md) describe respaldo y protocolo de dispositivos.

## Limitaciones declaradas

- No correlacionar picos de impedancia con respuestas exactas de ejecución.
- Los valores acústicos modelados no son certificaciones de afinación.
- El RMS no es nivel dB calibrado.
- El espectro **en vivo** de 8192 puntos a 44,1 kHz tiene separación ~5,38 Hz; no usar ese gráfico por sí solo para medir notas precisas. El nuevo reanálisis usa otros parámetros/escalas descritos en AUDIO_ANALYSIS.
- El detector temporal YIN-like usa downsampling por promedio, no antialias FIR; futuras mejoras pueden añadir filtros y calibración.
- `SharedPreferences` es suficiente para prototipo; considerar Room/DataStore en posteriores versiones tras migración testeada.
- No conectar endpoints ni pedir permiso INTERNET.
