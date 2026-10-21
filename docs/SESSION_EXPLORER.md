# Explorador acústico 3.0 — implementación y límites

## Actualización de interfaz 3.1

Las herramientas y formatos de datos descriptos aquí se conservan. La interacción actual, cámaras, gestos, orientación, reproducción y referencia de color del visor se documentan en [IMMERSIVE_VIEWER.md](IMMERSIVE_VIEWER.md), que prevalece sobre descripciones de presentación de 3.0. Evidencia vigente en VALIDATION y UI_AUDIT.

Fecha: 20/10/2026. Todo el procesamiento y almacenamiento es local. No se agregaron bibliotecas ni permisos. La implementación es original; no se portó código de DidgeLab/CADSD ni de las referencias gráficas.

## Componentes

| Responsabilidad | Implementación |
| --- | --- |
| Captura progresiva | `MicrophoneEngine` → `ProgressiveWavWriter` en el hilo de audio |
| Lectura y análisis | `Pcm16WavReader`, `WavAnalysis`, `SpectralAnalysis`, `HarmonicAnalysis` de fase A, sin reescribir sus algoritmos |
| Datos para gráficos | `AudioVisuals`: STFT y envolvente mínimo/máximo del intervalo visible |
| Trabajo cancelable | `SessionWorkspace`, coroutines del ViewModel; fase A conserva su executor y token de cancelación |
| Reproducción | `SessionPlayer`, MediaPlayer asíncrono; consultas de posición real |
| Caché regenerable | `AnalysisCache`, binaria versionada, huella SHA-256 del WAV y parámetros |
| Relaciones y anotaciones | `SessionToolsStore`, preferencias aditivas `session_tools` |
| Eliminación reversible | `TrashStore`, tombstones durables en nuevas claves; WAV/índice sin cambios hasta vaciado explícito |
| Interfaz | `SessionExplorer`, `AcousticCharts`, `ComparisonScreen`, `LibraryTools` |

## Captura y recuperación

El límite inicial es **15 minutos**, configurable en Medir (1, 5, 15 o 30 minutos). Audio PCM16 mono a 44100 Hz: 15 minutos son 79.380.044 bytes, aproximadamente 75,7 MiB. Solo el bloque actual y el buffer de detección permanecen en RAM. El contador sigue visible y el botón principal inicia o detiene/guarda todo.

Cada captura usa un UUID nuevo con extensión `.part`. El escritor actualiza longitudes RIFF/data y sincroniza una vez por segundo y al cerrar. Se reserva un margen de 2 MiB y se comprueba espacio por bloque. Ante falta de espacio se finaliza lo capturado y se comunica el error. El guardado renombra a `.wav` en el mismo directorio y luego persiste resumen/serie; si falla el índice el WAV sigue recuperable, sin inventar resumen.

Al iniciar se revisan exclusivamente parciales propios `UUID.part`: se repara la cabecera según los bytes realmente presentes, se descarta únicamente un byte incompleto de PCM y se renombra si no existe el destino. WAV históricos nunca se reparan ni reescriben. Una captura recuperada carece honestamente de las lecturas que se perdieron al cerrar; se puede reanalizar. Un corte de energía puede perder lo que el sistema no alcanzó a escribir/sincronizar; no se garantiza recuperar muestras que nunca llegaron al archivo. Los parciales vacíos o inválidos no se convierten en sesiones falsas.

Al cambiar de pantalla o ejecutar `onStop`, se detiene y guarda; **no hay grabación ni servicio oculto en segundo plano**. No se incorporó captura con pantalla bloqueada. El WAV se exporta mediante SAF copiando por bloques en IO, sin convertir los 76 MiB en un ByteArray.

## Espectrograma

- STFT con la FFT y Hann periódica de fase A: N = 32768, salto = 16384 (50% de solapamiento), muestra real del WAV; cola parcial con muestras válidas.
- A 44100 Hz: separación de bins **1,345825 Hz**, ventana de **743,04 ms**, paso **371,52 ms**. La Hann tiene un lóbulo principal mayor que un bin: ampliar la imagen no aumenta la resolución física. Transiciones más cortas pueden quedar mezcladas en la ventana. No se afirma distinguir dos tonos arbitrariamente próximos por tener píxeles separados.
- Cada ventana aporta densidad de potencia digital a una celda temporal; las celdas que acumulan ventanas promedian potencia, antes de convertir a dB. Las filas agrupan bins por su frecuencia real. Si una fila es más fina que un bin usa el bin más cercano, sin interpolar nuevas mediciones. Las celdas temporales de borde sin centro de ventana usan la ventana real más próxima y representan su soporte temporal, no observaciones adicionales.
- Lineal: desde 0 Hz; logarítmica: el menor bin positivo es el límite inferior, porque log(0) no existe. Rangos iniciales 500/2000/Nyquist, deslizadores y entrada numérica. Sin recorte fijo a 2000 Hz.
- `10 log10(PSD / máximo visible)`; rango dinámico 30–100 dB. Silencio sin energía significativa se distingue del máximo. La referencia cambia al cambiar de intervalo/rango: colores de dos vistas diferentes no equivalen a intensidades absolutas comparables.
- Paleta original de cinco anclas violeta/azul/verde/amarillo, interpolada en RGB e inspirada en paletas ordenadas de magnitud. Es una aproximación visual, **no** una afirmación de uniformidad perceptual exacta. La leyenda incluye extremos en dB. No se usa verde de espectrograma para afirmar buena confianza.
- Un viewport tiene como máximo 256 × 256 floats en la configuración de UI, más bitmap del mismo tamaño y buffers FFT. No se conserva una matriz de todas las ventanas de 15 minutos en Compose. Todas las ventanas del intervalo visible contribuyen; no se saltan para simular detalle. Se puede cancelar y reintentar. El procesamiento sucede fuera del hilo principal y se espera 160 ms al modificar controles antes de iniciar otra solicitud.

## Onda, espectro e interacción

La onda agrupa mínimos/máximos reales en hasta 1024 columnas y preserva picos. Al ampliar a 1024 muestras o menos se dibujan las muestras originales conectadas; no se pierden los originales. Leer el intervalo usa bloques de 32768 muestras. No se guarda todo el audio en un estado Compose.

El espectro utiliza el promedio de fase A. Eje horizontal Hz, vertical 0 a −80 dB relativos. Al comprimir se preserva el máximo por columna; al ampliar se conectan los bins medidos. Los puntos y Hn? corresponden a picos detectados; guías tenues son múltiplos teóricos. El toque consulta Hz, nivel, orden candidato y desvío. Sin F0 confiable no se asignan órdenes. La resolución, la ventana y el procesamiento del teléfono limitan la interpretación.

Los gráficos aceptan pellizco y arrastre horizontal; un arrastre vertical queda para el scroll de pantalla. Hay botones de zoom/desplazamiento, entrada de Hz y selector temporal como alternativas. Un toque selecciona posición/frecuencia; en modo Buscar mueve MediaPlayer, y en Marcar inicio/fin delimita el fragmento. La UI muestra el intervalo analizado, no mezcla sus métricas con las originales. La estimación alrededor de un punto analiza hasta ±750 ms; no es una frecuencia de un instante matemático.

## Reproducción y bucle

MediaPlayer se prepara asíncronamente. El cursor se actualiza consultando `currentPosition` cada 100 ms; no integra un reloj propio. Buscar usa `seekTo(..., SEEK_CLOSEST)` y conserva reproducción/pausa cuando ya está preparado. La posición pausada sigue procediendo del motor. Al finalizar se conserva cursor al final; reiniciar vuelve a cero. Navegar fuera libera el reproductor y cancela el trabajo.

Los seeks se coordinan con `OnSeekCompleteListener`: solo hay uno en curso y se conserva la última solicitud pendiente. El control del bucle espera esa confirmación para no encolar saltos repetidos al consultar un fin todavía no actualizado. Con un bucle activo los saltos se limitan al fragmento; habilitarlo fuera de la región busca su inicio. Este caso de búsquedas encadenadas tuvo un fallo reproducible de timeout en emulador y se verificó otra vez después de la corrección.

El bucle consulta posición cada 40 ms y busca el inicio cuando alcanza el fin. **No es sample-accurate ni gapless**: el planificador, el decodificador y el seek añaden latencia; la sincronización fina/audible en Samsung está pendiente. Un cursor puede caer entre dos ventanas STFT y no debe interpretarse como un espectro instantáneo. El viewport es independiente del reproductor: el cursor solo aparece cuando su tiempo queda dentro de la vista actual, sin saltar el zoom que eligió el usuario.

## Caché, comparación y relaciones

La caché usa versión de algoritmo/formato, SHA-256 de bytes originales, límites temporales y parámetros visuales. Límite global de archivos `.bin`: **64 MiB**, eliminando primero los menos recientemente usados. Solo hay un viewport activo por sesión abierta. Datos corruptos se regeneran; un fallo de escritura de caché no descarta el análisis obtenido. Cambiar versión o audio invalida la identidad; «Volver a analizar» fuerza el recálculo. La caché es prescindible, no es el único ejemplar del audio ni una migración del índice histórico.

Comparar permite A/B completas o dos intervalos ingresados en segundos, también dos fragmentos de una misma sesión. Muestra F0/nota, rango habitual, porcentaje de estabilidad, duración, centroide de potencia, bandas y candidatos. A es continua verde, B discontinua naranja. Normalizar refiere cada espectro a su máximo; desactivarlo usa la mayor PSD digital de A/B como referencia común. No es una comparación SPL ni prueba que diferencias provengan del material. Cambiar selección o límites invalida los resultados visibles anteriores.

Los marcadores de «Posible toot» y «Vocalización» son manuales y persistentes. La app no clasifica vocales ni toots automáticamente. Los cambios de bandas/centroide se exploran con espectrograma y comparación de fragmentos; no hay una calificación de calidad sonora ni THD.

La fundamental de reanálisis conserva el rango **35–220 Hz** de fase A; el medidor en vivo conserva sus opciones de rango. El espectro/espectrograma pueden mostrar componentes hasta Nyquist, pero eso no amplía automáticamente el rango validado del detector de fundamental. Para registros superiores se ofrecen observación y marcadores manuales, sin atribuirles una F0 validada fuera del rango.

Una sesión puede asociarse opcionalmente con un diseño existente. Se simula en background con el motor original sin modificarlo. Se presentan por separado resonancias de impedancia simuladas, F0 medida y candidatos armónicos. La diferencia frente a la primera resonancia es **exploratoria**, no validación de equivalencia. No se empareja el enésimo armónico con la enésima resonancia. Faltan referencias físicas independientes para validar CADSD/DidgeLab y grabaciones etiquetadas para investigar toots.

## Papelera y compatibilidad

Deslizar hacia la izquierda sigue el dedo, cambia el fondo/aviso al superar 38% y solo confirma después de soltar y alcanzar el estado final. Volver o soltar antes cancela. El menú/botón Mover a papelera es la alternativa accesible. Deshacer quita el tombstone; Biblioteca → Papelera permite restaurar después del timeout o de reiniciar. **No hay borrado irreversible por temporizador.** Diseños usan la misma estrategia.

WAV, índice, resumen original, derivados y relaciones siguen presentes mientras están en papelera. Vaciar requiere confirmación explícita y elimina exclusivamente UUID/rutas propias, sus derivados y relaciones. Los tombstones se quitan al final; si un vaciado se interrumpe puede completarse desde la papelera aunque falte ya el registro/archivo. Tras confirmar el vaciado no se promete Deshacer. Las claves históricas `measurements`, `designs`, `recordings` y el ID/firma se conservan; las nuevas preferencias son aditivas. Véase [DATA_SAFETY.md](DATA_SAFETY.md).

## Referencias técnicas consultadas

- [Swipe to dismiss de Material 3](https://developer.android.com/develop/ui/compose/touch-input/user-interactions/swipe-to-dismiss): comportamiento del componente nativo; se añadió observación de dedo apoyado para no confirmar durante el arrastre.
- [MediaPlayer](https://developer.android.com/reference/android/media/MediaPlayer): prepareAsync, currentPosition y seek. La documentación no garantiza bucle sin cortes mediante seek.
- [Paletas de magnitud y percepción](https://matplotlib.org/stable/users/explain/colors/colormaps.html): criterio visual; no se copió su código ni una tabla de colores.

Resultados y tolerancias numéricas: [VALIDATION.md](VALIDATION.md). Limitaciones de fase A: [AUDIO_ANALYSIS.md](AUDIO_ANALYSIS.md).
