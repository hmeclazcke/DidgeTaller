# Análisis de audio — fase A de Didge Taller 3.0

## Interfaz 3.1

El visor reutiliza fase A y AudioVisuals: no se cambiaron fórmulas, detector, FFT, armónicos ni caché de potencia. La referencia de color se conserva durante la exploración y el espectro identifica grabación completa o fragmento. La inspección usa ventanas/celdas reales y distingue coordenadas de un pico medido. Arquitectura y límites de representación/sincronización: [IMMERSIVE_VIEWER.md](IMMERSIVE_VIEWER.md). Evidencia y pruebas físicas pendientes: [VALIDATION.md](VALIDATION.md).

Estado al 20/10/2026: fase A se conserva sin reescribir sus algoritmos. El explorador 3.0 reutiliza este pipeline para toda la sesión o fragmentos, ofrece espectro/onda/espectrograma y guarda derivados en caché versionada por SHA-256. WAV, índice y resumen histórico no se reemplazan. Especificación vigente de visualización, sincronía, comparación, rendimiento y límites: [SESSION_EXPLORER.md](SESSION_EXPLORER.md).

## Arquitectura y dependencia entre fases

```text
WAV privado → Pcm16WavReader (bloques mono)
              → WavAnalysis (intervalo en frames, cancelación, progreso)
                  → SpectralAnalysis → Radix2Fft → PSD promedio
                  → PitchDetector existente → SessionAnalysis
                  → HarmonicAnalysis → candidatos y métricas
              → TallerViewModel → RecordingAnalysisPanel
```

La FFT radix-2 se extrajo del detector existente; no se introdujo otra implementación ni dependencia. `PitchDetector.analyze(includeVisuals=false)` omite solo las listas de visualización y su FFT adicional. El cálculo de periodicidad conserva sus ecuaciones y umbrales. El espectro en vivo sigue siendo su representación visual anterior; no debe interpretarse como dB ni usarse como PSD.

`WavAnalysis.analyze` admite un `MonoPcmSource`, intervalo `[startFrame, endFrame)`, configuración, cancelación y progreso. Los tiempos del resultado son centros de ventanas en la escala del WAV original. La UI actual selecciona toda la grabación, un fragmento o una ventana alrededor de una posición; los tiempos mantienen la escala del WAV original. Una fundamental por ventana no describe un instante matemático ni un transitorio más corto que la ventana.

## Lectura y memoria

- RIFF/WAVE PCM16 little-endian, mono o estéreo, frecuencia real de 8–192 kHz. Estéreo se reduce por promedio de canales; fases opuestas pueden cancelarse. No es análisis independiente L/R.
- Busca bloques `fmt ` y `data`, respeta bloques desconocidos y padding impar; comprueba RIFF, longitudes, alineación, bits, canales y byte rate. Rechaza compresión, float, WAV extensible, múltiples `data`, audio vacío o truncado con mensajes explícitos. No intenta reparar ni sobrescribe un original dañado.
- Acceso `RandomAccessFile` de solo lectura. No carga todo el archivo ni toda la Biblioteca. PCM, bytes y buffers FFT son del tamaño de una ventana; se guarda una PSD promedio y una lectura pequeña por ventana, nunca una matriz de espectrograma.
- Presupuesto configurable `maxWindows=10000`: evita crecimiento ilimitado de metadatos y trabajo. Equivale a aproximadamente una hora a 44,1 kHz con el hop inicial; incluye holgadamente 15 minutos. Superarlo produce error, no truncamiento silencioso.
- Trabajo en executor propio. Cancelación cooperativa entre ventanas y antes de publicar resultados; el token descarta callbacks viejos. Progreso entero limitado a cambios porcentuales. Salir del detalle, detener la app o limpiar el ViewModel cancela; `use` libera el lector.

## Espectro y unidades

Valores iniciales: FFT de **32768**, Hann periódica, hop **16384** (50% de solapamiento), eliminación de media por ventana. A 44,1 kHz son 0,743 s de ventana, 0,372 s entre inicios y **1,3458 Hz por bin**. La separación entre bins no equivale a exactitud garantizada ni a la separación mínima entre dos tonos. La ventana Hann ensancha los picos y pierde resolución temporal. La frecuencia de muestreo del WAV modifica estos valores.

Para PCM normalizado a escala completa digital:

`PSD[k] = lado[k] × |FFT(Hann × (x − media))|² / (fs × suma(Hann²))`

`lado=2` en bins interiores y 1 en DC/Nyquist. Unidad: **escala completa digital²/Hz**. `suma(PSD) × fs/N` estima potencia media digital de la señal sin DC. Se promedian periodogramas lineales, nunca dB. Una última ventana parcial usa Hann sobre las muestras disponibles, padding a N y peso `muestras/N`; es una variante explícita de Welch para cubrir la cola, no una llamada exacta a SciPy. Esa cola tiene menor resolución efectiva. No se añaden ventanas parciales repetidas al terminar.

Nivel mostrado: `10 log10(PSD[k]/máximo PSD)`; el máximo es 0 dB **relativos**. No es dB SPL, ni amplitud absoluta calibrada, ni permite comparar volumen de dos tomas sin más. Silencio/DC no producen un pico de 0 dB: se devuelven métricas ausentes. Umbral numérico de referencia: PSD máxima ≤1e−16 se considera sin energía significativa para picos/centroide.

Métricas calculadas una vez en el worker:

- **Centroide de potencia**, ponderado por PSD en Hz; difiere del centroide ponderado por magnitud usado en otras herramientas.
- Fracción de energía en bandas `[0,100)`, `[100,250)`, `[250,500)`, `[500,1000)`, `[1000,2000)`, `[2000,5000)` y resto hasta Nyquist; bordes que exceden Nyquist se omiten. No son formantes ni notas detectadas.
- Picos interpolados y fracción de energía en bandas de candidatos: ver [HARMONICS.md](HARMONICS.md).

## Fundamental y estadística

Se conserva el detector YIN-like (función de diferencia, CMNDF, mínimo de lag e interpolación), incluido downsampling por promedio de 4 muestras, RMS mínimo 0,005 y sus limitaciones de aliasing. No se reemplazó por el pico FFT mayor. El reanálisis exige confianza de periodicidad ≥0,80 para admitir Hz; ese índice no es una probabilidad estadísticamente calibrada.

Rango inicial de F0: 35–220 Hz, configurable en `WavAnalysis.Settings`; debe quedar debajo de fs/4 por el detector actual. Se probó también 440 Hz con rango configurado hasta 1000 Hz y fs=48 kHz. Eso no clasifica toots. `SessionAnalysis.summarize` admite un rango explícito nuevo, manteniendo 35–220 Hz por defecto y compatibilidad de datos.

El resumen requiere ≥3 ventanas válidas en una misma nota de semitono y mayoría estricta sobre las válidas. Usa mediana de esa nota, rango percentil 10/90 y estabilidad como fracción de esas ventanas a ±25 cents de la mediana. Además se muestran mediana y media aritmética de **todas** las ventanas válidas, cobertura válida e índice mediano de periodicidad. Estas cantidades responden preguntas distintas. Un fragmento corto o mixto puede tener picos reales y ningún resumen confiable.

## Origen, referencias y límites

Implementación Kotlin propia; ninguna fuente de DidgeLab, CADSD o SciPy fue copiada/adaptada en esta fase. No hay nuevas bibliotecas. Se reutiliza nuestra FFT existente y métodos matemáticos estándar:

- [SciPy/Welch: segmentación, Hann, solapamiento y escalas](https://docs.scipy.org/doc/scipy/reference/generated/scipy.signal.welch.html).
- [de Cheveigné y Kawahara, YIN (2002)](https://pubmed.ncbi.nlm.nih.gov/12002874/): referencia del enfoque, no afirmación de implementación completa del artículo.
- [Especificación WAVE y referencias Microsoft reunidas por McGill](https://www.mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/WAVE.html).

La variación observada del PVC (66,1–73 Hz) no se resuelve imponiendo una frecuencia esperada. Faltan audio etiquetado, micrófono/posición controlados y referencia independiente. El procesamiento automático del teléfono puede cambiar graves y componentes espectrales. Los ensayos sintéticos no validan precisión profesional. El rendimiento completo de 15 minutos en Samsung está pendiente. Ahora hay pruebas JVM de escritura/lectura WAV completa y STFT virtual de 900 s, además de la cancelación de fase A en emulador; no equivalen a una captura física sostenida ni a un benchmark del teléfono.

Las herramientas de B–F ya reutilizan este núcleo: espectro interactivo, onda min/max, STFT de viewport con caché, selección/reproducción/comparación y captura progresiva inicial de 15 minutos. No se guarda una matriz completa de todas las ventanas ni todo el PCM en Compose. Próximo trabajo: validación manual física/externa, accesibilidad y rendimiento sostenido según [ROADMAP.md](ROADMAP.md); no rehacer estas funciones.
