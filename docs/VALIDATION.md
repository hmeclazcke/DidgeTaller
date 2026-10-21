# Registro de validación

## 20/10/2026 — Cierre de Didge Taller 3.1 beta1

### APK e instalación

- **3.1.0-beta1 / código 5**, paquete `ar.didgetaller`, mismo certificado debug SHA-256 `fbf54fbe12161bdbf5adbfb9d906961511e9b49a715be8eac8f886e01292b7b2`, comprobado contra el APK instalado respaldado.
- APK: `app/build/outputs/apk/debug/app-debug.apk`, **11.136.831 bytes**, SHA-256 `a53dacb774288740967e2244214b2684db49997ba6e8d55f9399afd1516d259e`.
- Samsung **SM-G990E / R5CT21KTQ8W** actualizado mediante `adb -s R5CT21KTQ8W install -r ...`: **Success**. `am start -W` correcto, proceso **22054**, sin FATAL EXCEPTION en su buffer crash. No se iniciaron grabaciones ni reproducciones reales.
- Respaldo fresco: `backups/samsung-before-3.1-20261020-203046/`, tar de **9.607.168 bytes**, **24 archivos / 7 WAV** verificados antes de instalar. Después: **22 archivos idénticos**, incluidos todos los WAV, índice, ajustes, relaciones y cachés anteriores. Cambiaron solo `shared_prefs/android.app.ActivityThread.IDS.xml` y `files/profileInstalled`, metadatos automáticos del runtime. Se decodificó el marcador de 24 bytes: esquema 1, fecha **21:03:39**, coincidente con la actualización del paquete. [AndroidX ProfileVerifier](https://raw.githubusercontent.com/androidx/androidx/androidx-main/profileinstaller/profileinstaller/src/main/java/androidx/profileinstaller/ProfileVerifier.java) documenta ese archivo. No se restauró, borró ni recreó dato del usuario.

El último retoque de borde/formatos tuvo una segunda actualización no destructiva (5→5), después de un nuevo respaldo en `backups/samsung-before-3.1-20261020-210152/`. También devolvió Success; el APK/PID anteriores corresponden al cierre final. Frente al respaldo original de 3.0 siguen intactos 22/24 archivos y los 7 WAV; frente al respaldo inmediatamente previo, 23/24 (solo cambió el marcador ProfileVerifier).

### Pruebas ejecutadas

| Objetivo | Resultado | Evidencia local |
|---|---|---|
| Línea base | 44 JVM y assembleDebug aprobados | Checkpoint anterior y registro de inicio |
| Gate final `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | **57 JVM**, 0 errores/fallos; APK y APK de pruebas compilados | `checkpoints/v31-visual-close-build.log`, XML en `app/build/test-results/testDebugUnitTest/` |
| Suite SessionNavigation + LocalPersistence + ViewerRotation | **22/22**, 268,462 s, emulador | `v31-regression-final.log` |
| Pan/pinch, selección/bucle, recreación/horizontal | **3/3** foco | `v31-viewer-focus.log` |
| Rango 100 Hz/log, referencia fija, inspección H1/dB y audio del espectro | Aprobada tras esperar estado de referencia inicializado | `v31-final-state-focus.log` |
| Permiso concedido al regresar/recrear, sin arranque oculto | Aprobada | Caso `returningWithPermissionGrantedUpdatesFeedbackWithoutStartingCapture` en `v31-swipe-permission-focus.log`; esa ejecución tenía un fallo distinto de swipe, corregido después |
| Swipe corto lento/rápido, reversión, mantener/suelta y Deshacer/WAV; Medir/foco/formularios | **2/2** después de correcciones | `v31-safety-fix-focus.log` |
| Último retoque visual de diseños/resonancias y regresión swipe | **2/2**, cero píxeles rojos en tarjeta en reposo | `v31-visual-close.log`, 40,686 s |
| Atrás con modo de tres botones | **1/1**, además del gesto real desde el borde aprobado en suite | `v31-threebutton.log` |
| Matriz 360 dp / fuentes 1,0 y 1,3; 420 dp / 1,0 y 1,5 | Cuatro recorridos aprobados; capturas reales inspeccionadas | `v31-matrix-*.log`, `v31-last-foci.log`, `docs/screenshots/v31-final/` |
| TalkBack | Smoke parcial: servicio ligado, exploración táctil activa, 14 nodos etiquetados y acción principal identificable; ajustes restaurados | `v31-talkback-dumpsys.txt`, `v31-accessibility-smoke.json`; no lectura audible ni recorrido completo |

**24 casos funcionales instrumentados distintos** aprobados entre suite y focos; los recorridos de capturas son adicionales. No se afirma una suite única final de 24 ni cobertura completa. Todas las invocaciones instrumentadas usaron `adb -s emulator-5554 shell am instrument ...`, nunca `connectedDebugAndroidTest` ni el Samsung. No hubo desinstalaciones, pm clear o borrado privado sobre el teléfono.

Las 13 JVM nuevas prueban proyección píxel/dato, inversión vertical, anclaje de zoom, pan, extremos, límites lineales/logarítmicos, ticks y escala robusta con valores atípicos/silencio. Tolerancias: **1e-9** para coordenadas/fracciones y **1e-6 Hz** en transformaciones logarítmicas. Se compararon **26 archivos anteriores** de core/data/captura/reproducción contra el ZIP inicial: idénticos byte a byte.

WAV largo sintético: **900 s / 39.690.000 muestras / 79.380.044 bytes**. Se abrió el visor, inició/canceló STFT y generó onda con UI operativa en **10.513 ms para el recorrido completo**; zoom a 10 ms recuperó 441 muestras. Se conservó el original. JVM también limita la STFT a 65.536 celdas y onda a 1024 puntos. No es un benchmark aislado de FFT/Samsung y no se midieron FPS, batería o memoria total con perfilador.

### Fallos investigados y corregidos

- Redondeo de tick logarítmico apenas fuera del borde: clamp de presentación y prueba. Un fixture de outlier estaba fuera del rango válido del detector; se corrigió el fixture.
- Pinch inicial de prueba tenía argumentos en orden incorrecto; corregido según API oficial, sin adaptar la matemática a una prueba errónea.
- Cierre al rotar por altura transitoria casi nula del gráfico y límites inválidos para etiquetas; se evita dibujar hasta tener espacio útil. Recreación y rotación real posteriores aprobadas.
- Herramientas fuera del ancho y selección comprimida: barra compacta/FlowRow. El espectro ocultaba pausa: reproductor disponible en los cuatro gráficos.
- La prueba de referencia espectral leía antes del efecto de inicialización; espera explícita del estado completo. Conservación al cambiar rango/log aprobada.
- Gesto corto rápido de 80 ms descartaba por velocidad antes del umbral: reproducido y corregido exigiendo distancia al soltar. Foco posterior con WAV/Deshacer aprobado.
- Snackbar de permiso seguía visible durante captura válida: se retira cuando el permiso está disponible, comprobado por aserción y nueva captura.
- El primer script local de verificación tenía un alias de encoding inválido y falló antes de operar sobre Samsung. Tras instalar, el control estricto de hashes detectó el marcador ProfileVerifier; se investigó en modo de lectura y se comprobó su estructura/fecha. No se reinstaló como remedio ni se ignoraron diferencias sin identificar.

### Límites y prueba manual

Pendientes: didgeridoo PVC, lectura desde 1–2 m con nota activa, micrófono/AGC, sincronía audible y latencia de seek/bucle, gesto predictivo Samsung, TalkBack completo, fuentes personalizadas del teléfono y captura física sostenida de 15 minutos/almacenamiento/temperatura. Los sintéticos no demuestran precisión profesional. Cámara/selección sobreviven a recreación de Activity; no se promete restaurarlas tras muerte del proceso.

Prueba manual: Biblioteca → sesión de ~69,6 Hz → Pantalla completa → alternar gráficos → arrastrar/pellizcar/mantener → Girar → reproducir/pausar/buscar → Seguir y explorar manualmente → Atrás a sesión y Biblioteca. En Herramientas, marcar inicio/fin, analizar fragmento y probar bucle. Para borrar, usar una sesión prescindible y Deshacer; conservar la papelera si se desea recuperar el audio.

Fecha inicial: 19-10-2026.

## 20/10/2026 — Entrega integral 3.0 beta1 (estado vigente)

Se implementaron herramientas B–F reutilizando A. Se inspeccionaron AGENTS, README, documentación/arquitectura/código/pruebas, dispositivos y estado Git antes de modificar. Git sigue ausente; se creó `checkpoints/before-3.0-20261020-160736.zip`. Comparación binaria contra ese checkpoint confirmó **11 archivos de fase A/motores sin un solo byte cambiado**: PitchDetector, FFT, lector, PSD, armónicos, WavAnalysis, SessionAnalysis, Music, WavCodec, AcousticModel y BoreOptimizer. No se añadieron permisos/dependencias ni red.

### Resultados ejecutados

| Verificación | Resultado real |
| --- | --- |
| Línea base `gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain` | Éxito antes de modificar, suite previa de 33 JVM/código A |
| Gate final `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | **BUILD SUCCESSFUL in 16s**, 44 JVM sin fallas: Core 10, AudioAnalysis 20, WavReader 3, VisualAnalysis 11. Logs `app/build/gradle-v3-final.log`, XML JUnit en `app/build/test-results/testDebugUnitTest/`. |
| Primera suite instrumentada completa, solo `emulator-5554` API 37 | 16 casos en 118,176 s: 15 pasaron y falló el selector de error duplicado. No se presenta esta ejecución como suite limpia. |
| Focos posteriores de UI | Se corrigió el contexto del error y el scroll de la aserción; `damagedAudioReportsErrorWithoutChangingMetadata` pasó en la corrida focalizada de 2 casos (43,018 s), cuyo otro caso detectó un timeout de bucle. La corrida previa de 3 casos (68,108 s) ya había aprobado captura y explorador/zoom, pero todavía fallaba visibilidad del error. |
| Regresión de seeks/bucle | Se reprodujo timeout al encadenar seek previo al inicio y cerca del final. Se coordinó un solo seek pendiente con OnSeekCompleteListener. `explorerRendersRealGraphsSeeksPausesLoopsAndCompares` pasó (45,396 s); tras ajustar onStop para no marcar cancelada una vista terminada, volvió a pasar **37,631 s** con esa comprobación añadida. |
| Asociación/anotación manual | `designAssociationAndManualMarkersSurviveReopening`: **OK (1 test), 27,686 s**, selector real de diseño, simulación vs audio separadas y relación/marcador conservados al reabrir. Su primer intento tocaba detrás del snackbar de guardado; la prueba espera ahora su cierre antes de tocar, y verifica que exista el diseño. |
| Total de cobertura instrumentada de esta entrega | **18 casos distintos aprobados**, 7 de persistencia y 11 UI, mediante suite y repeticiones focalizadas; **no una única corrida final de 18**. Todos los comandos y guardas apuntaron al emulador; nunca Samsung. Logs en `app/build/instrumentation-v3*.log`. |
| Integración Medir con AudioRecord real del emulador | Un botón inicia captura, el mismo detiene/guarda WAV legible; segunda captura finaliza/guarda con onAppStopped sin seguir escuchando. Se verificaron duración/tamaño, reapertura e incremento del listado. La entrada del emulador no representa al PVC. |
| Explorador del emulador | WAV sintético → STFT/bitmap con energía real, onda, zoom/desplazamiento, inicio/fin 5–10 s, análisis de 5 s y F0 66,1±0,3 Hz, seek fuera/dentro del bucle, retorno al inicio, pausa/cursor estable (≤80 ms de tolerancia en la comprobación), consulta H1, comparación A/B y Atrás a Biblioteca. No se evaluó audio audible. |
| Papelera/compatibilidad | Swipe corto cancela; mantener dedo más allá del umbral no elimina; soltar elimina del listado y Deshacer restaura bytes. Prueba de stores reabiertos conserva WAV, índice original, caché, marcador y relación de diseño. También pasaron legado Java/diseños, WAV anterior/recuperado, renombrado y recuperación de captura parcial propia. |
| Cancelación/errores/integridad | Cancelación STFT/onda/core; fase A/progreso/Cancelar sobre fuente de 900 s en emulador; callbacks descartados al salir; silencio, WAV dañado/incompleto/no compatible. Caché reutiliza intervalos y cambia al modificar WAV sintético; originales/índice sin cambios por analizar. Espacio insuficiente simulado conserva audio previo válido. |
| Captura/lectura de 15 minutos en JVM | WAV escrito en bloques y releído: **79.380.044 bytes / 39.690.000 muestras / 900.000 ms**, memoria PCM acotada. No es AudioRecord físico durante 15 minutos. |
| STFT de 900 s en JVM | Fuente virtual completa, 256×256 floats = **262.144 bytes de celdas**, ~3,8 s en esta PC (varía por ejecución); no se conserva la matriz de todas las ventanas. No mide consumo/batería del Samsung ni todo el pipeline YIN de 15 minutos. |
| Captura de pantalla | `docs/screenshots/v3-spectrogram-synthetic-66hz.png`, captura real Compose/Canvas de la prueba sobre seno de 66,1 Hz/20 s. No es maqueta ni grabación real del PVC. |

Comandos de instrumentación: instalar app/test con `adb -s emulator-5554 install -r <APK>` y ejecutar `adb -s emulator-5554 shell am instrument -w [-e class ar.didgetaller.ui.SessionNavigationTest#metodo] ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner`. Se usó una lista separada por comas para los focos de varios métodos. No se ejecutó `connectedDebugAndroidTest`, para no permitir selección automática del teléfono.

### Tolerancias y algoritmos

Fase A conserva las tolerancias ya descritas abajo: tonos 65,41/69,30/73,42/70 Hz, fundamental débil/ausente, armónicos ajenos/ruido/silencio y niveles analíticos. Las nuevas pruebas STFT admiten **±3 Hz** en el pico rasterizado (bins ~1,35–1,46 Hz y filas de ~1,95 Hz, Hann finita), **±3 dB** para relación entre filas de 140/210 Hz reducidas; no sustituyen el ensayo más estricto de PSD de fase A (±0,4 dB sintéticos). Los extremos/muestras de onda se preservan exactamente a 1/32768 de amplitud digital; cabecera, frames y duración se comprueban con enteros exactos. Las tolerancias de UI/reproductor son funcionales, no certifican sincronía audible fina ni un bucle entre muestras sin separación.

La STFT reutiliza FFT/Hann de fase A (N32768, hop16384), promedia potencia de ventanas reales por celda de viewport y expresa dB respecto al máximo visible. Onda min/max/muestras, espectro con máximos por columna y candidatos medidos, caché SHA-256/versionada/64 MiB. MediaPlayer aporta currentPosition, seeks confirmados y bucle con polling sobre posición real; no integra un cronómetro ficticio. Papelera aditiva persiste tombstones sin borrar WAV antes del vaciado explícito. Especificación completa: [SESSION_EXPLORER.md](SESSION_EXPLORER.md).

### APK y Samsung físico

- APK final: `app/build/outputs/apk/debug/app-debug.apk`, **11,287,565 bytes**, `3.0.0-beta1` / versionCode **4**, paquete `ar.didgetaller`.
- SHA-256 del APK final e instalado: **0EE9820862E51DEF416859576DC2339E592C409A410E96A69F6A765044650C87**.
- `apksigner verify --print-certs` comparó APK anterior y nuevo: certificado SHA-256 **FBF54FBE12161BDBF5ADBFB9D906961511E9B49A715BE8EAC8F886E01292B7B2**, idéntico. Se verificó paquete/versionCode y que no hubiera RECORD_AUDIO en ejecución.
- Samsung **SM-G990E / R5CT21KTQ8W**, estado ADB device. Actualización autorizada `adb -s R5CT21KTQ8W install -r app/build/outputs/apk/debug/app-debug.apk` → **Success**. Se aplicó primero la versión probada y después la corrección final del estado de vista al volver de segundo plano, ambas con la misma actualización no destructiva.
- Último inicio: `am start -W -n ar.didgetaller/.MainActivity` → **Status: ok**, COLD, 1808 ms; PID **24724**. Consulta `logcat -d --pid=24724 -t 300 AndroidRuntime:E '*:S'`: sin entradas. `dumpsys package` confirma 3.0.0-beta1/4; hash del base.apk instalado idéntico al archivo local. Esto verifica arranque/proceso, no una inspección visual/acústica completa en el teléfono.
- Respaldo anterior verificado: [DATA_SAFETY.md](DATA_SAFETY.md). **5 WAV, local_data.xml y tuner_settings.xml conservan sus SHA-256** antes/después de las actualizaciones y arranques. Una comparación global inicial difirió por la preferencia interna `android.app.ActivityThread.IDS.xml`: se identificó el único campo `IDSCount`, actualizado por Android al iniciar; no contiene el índice de sesiones. No se promete igualdad de ese contador del sistema ni se restauró sobre él. La comparación de datos de usuario se hizo por hashes normalizados, sin depender de saltos CRLF de los logs de Windows.
- No se desinstaló, limpió, restauró ni instrumentó el teléfono. No se borraron/modificaron sesiones/WAV reales, no se inició captura/análisis/reproducción de ellos, no se cambió firma/ID ni se usaron install -d/-t. El reporte local está en `app/build/samsung-v3-validation.json` y el respaldo privado en `backups/latest.txt`.

### Pendientes y prueba manual recomendada

1. Samsung → Biblioteca → sesión anterior de aproximadamente **69,6 Hz / Do♯2** → desplazarse a **Análisis del WAV → Analizar grabación**. Leer F0/mediana/media, incertidumbre, candidatos Hn y dB. El resultado puede diferir del resumen anterior; no se fuerza una nota esperada.
2. Volver al explorador, alternar Fundamental / Espectrograma / Forma de onda / Armónicos. Probar 0–500 Hz, escala log/lineal y entrada exacta de rango (por ejemplo 50–100 Hz). Tocar para buscar, ampliar y comprobar cursor al reproducir/pausar/reiniciar. Confirmar reproducción audible del WAV original.
3. Marcar inicio y fin, Analizar fragmento y Repetir fragmento. Comparar dos sesiones o dos intervalos y alternar normalización. Probar marcador manual y asociación con un diseño de dimensiones realmente ingresadas; simulación y audio deben seguir distinguidos.
4. Biblioteca: swipe corto/vuelta cancela; largo al soltar mueve a papelera; Deshacer restaura. También se puede restaurar después de reabrir la app. **No vaciar la papelera si querés conservar esa sesión.**
5. Medir: grabación corta del PVC, detener/reabrir/reproducir. Luego probar 15 minutos sostenidos con espacio libre suficiente, detención manual/al salir y contador. Esta captura física no se ejecutó por el agente.

Siguen pendientes precisión acústica frente a referencia independiente, sincronización fina/audible y deriva durante 15 minutos reales, consumo térmico/batería/memoria del teléfono, TalkBack/fuentes grandes/rotación/tamaños físicos y falta real de espacio/interrupción de energía. No se validaron modelos contra CADSD/DidgeLab ni un clasificador de vocales/toots. El bucle no es sample-accurate/gapless. El detector de fundamental de reanálisis mantiene 35–220 Hz, aunque los gráficos llegan a Nyquist. Lo pendiente está marcado en ROADMAP; no se atribuye precisión profesional por pasar señales sintéticas.

## Registro de entregas anteriores (los límites/cantidades siguientes son históricos)

## 20/10/2026 — Validación e instalación de fase A en Samsung

Alcance exclusivo: revisar fase A, completar pruebas, compilar y actualizar el teléfono. No se avanzó B ni se modificaron algoritmos/código de producción, identificador, firma, versión o esquema. Se revisaron AGENTS, AUDIO_ANALYSIS, VALIDATION, ROADMAP, clases de DSP/lector y panel/integración al detalle. Git sigue ausente (`not a git repository`). El panel está después de Audio y antes del gráfico temporal; no depende de que exista resumen/serie histórica.

| Verificación | Resultado |
| --- | --- |
| `gradlew.bat :app:testDebugUnitTest --console=plain` | `BUILD SUCCESSFUL in 3s`; **33/33** JVM: CoreTest 10, AudioAnalysisTest 20, WavReaderTest 3, cero fallas |
| `gradlew.bat :app:assembleDebug --console=plain` | `BUILD SUCCESSFUL in 1s`, APK de fase A actualizado respecto de lo instalado antes en Samsung; tareas `UP-TO-DATE` porque producción no cambió |
| `gradlew.bat :app:assembleDebugAndroidTest --console=plain` | `BUILD SUCCESSFUL in 3s`; solo se modificaron pruebas |
| Instrumentación focalizada `adb -s emulator-5554 shell am instrument -w -e class <lista de tres métodos> ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner` | **OK (3 tests)**, 44,121 s. Métodos: `recoveredAudioCanBeAnalyzedWithoutChangingWavOrIndex`, `damagedAudioReportsErrorWithoutChangingMetadata`, `progressAndCancelButtonWorkOnLongReadOnlyWav` de SessionNavigationTest. No se repitió toda la suite previa de navegación/persistencia. |
| WAV recuperado en UI del emulador | Abrir Biblioteca/detalle, Analizar grabación, resultados H1, bytes/índice idénticos; después Reproducir/Pausar sigue funcionando con MediaPlayer. Sin evaluación audible. |
| Progreso/Cancelar en UI del emulador | WAV sintético de prueba extendido a 900 s en almacenamiento aislado; progreso visible, botón Cancelar, mensaje Análisis cancelado, sin resultado ni cambio de índice/tamaño. No es grabación de 15 minutos ni benchmark completo. |
| Error en UI del emulador | WAV dañado de prueba muestra `No es un WAV RIFF válido.` sin cambiar metadatos. |
| Identificación física | `adb devices -l`: Samsung **SM-G990E**, serial **R5CT21KTQ8W**, estado `device`; también estaba emulator-5554. Todos los comandos de teléfono usaron su serial explícito. |
| Actualización física autorizada | `adb -s R5CT21KTQ8W install -r app/build/outputs/apk/debug/app-debug.apk` → **Success**. Ninguna otra opción de instalación. No hubo incompatibilidad de firma o versión. |
| Arranque y errores | `am start -W -n ar.didgetaller/.MainActivity` → `Status: ok`, `LaunchState: COLD`, 961 ms; a los 2 s PID 9812 activo. `logcat -d --pid=9812 -t 300 AndroidRuntime:E '*:S'` sin entradas. El teléfono estaba `Dozing`/`NotificationShade`: no se validó visualmente el detalle desbloqueado ni se reprodujo/analizó su audio. |
| Conservación de datos físicos | SHA-256 de `shared_prefs/local_data.xml` y de los **4 WAV privados**, mismos nombres, idénticos antes de instalar y después del arranque. SHA del `base.apk` instalado coincide con el APK local indicado abajo. Solo lectura para las comprobaciones. |

Pruebas numéricas añadidas: seno de **70 Hz**, F0 ±0,25 Hz, pico ±0,12 Hz; mezcla **70/140/210 Hz** con amplitudes 1000/9000/6000 PCM (H1 débil), F0 ±0,25 Hz, picos ±0,20 Hz, orden H1/H2/H3, H2 más intensa. Niveles relativos contra `20 log10(A/A_H2)` ±0,4 dB por ventanas finitas/cola y cuantización. Centroide de potencia analítico ±0,25 Hz; energía 0–100 Hz contra 1/118 ±0,002 y suma de fracciones 1 ±1e−12. Las pruebas existentes siguen cubriendo silencio/DC, fundamental ausente, ruido, picos ajenos, PCM16, WAV truncados y cancelación. Estas tolerancias sintéticas no demuestran precisión acústica sobre didgeridoos reales.

APK instalado: `app/build/outputs/apk/debug/app-debug.apk`, **13.742.147 bytes**, SHA-256 **B8B711A0B6ABB7C5B82CE62B1985409B3C468F3455E4BFE3AD5B39E513FCD857**, `2.1.0-beta1`/3. Es el mismo código de producción de fase A; las adiciones de esta tarea están en tests y documentación.

No se ejecutó en Samsung instrumentación, desinstalación, `pm clear`, borrado de archivos, recreación de índice ni migraciones. No se cambió firma/ID ni se usaron `install -d`/`install -t`. No se inició una grabación, análisis o reproducción de sus sesiones. Queda al usuario la prueba acústica manual y la comprobación visual tras desbloquear.

**Prueba manual exacta:** desbloquear → abrir Didge Taller → Biblioteca → Sesiones → abrir su WAV anterior de aproximadamente **69,6 Hz / Do♯2** → desplazarse debajo de Audio hasta **Análisis del WAV** → **Analizar grabación** → esperar el resultado/Volver a analizar → leer frecuencia predominante, mediana/media, picos Hn candidatos, Hz y dB relativos → volver al bloque Audio y pulsar **Reproducir**, luego **Pausar**. No hace falta guardar de nuevo. La F0 puede resultar incierta, y no todos los múltiplos tienen necesariamente un pico real. Precisión física, reproducción audible y sincronía fina siguen pendientes.

## 20/10/2026 — Didge Taller 3.0, fase A: fundamentos de análisis

Entorno Windows 11, JBR 21.0.11, SDK/compileSdk 35; mismas dependencias. Se revisaron AGENTS, README, contexto, arquitectura, roadmap, validaciones, registro de trabajo, estudio DidgeLab y módulos de audio/core/datos/UI/navegación/pruebas. `git status --short` devolvió `fatal: not a git repository`; no se dispone de diff/commits. La navegación corregida y las 7 pruebas instrumentadas previas estaban presentes en código, no solo 4 como decía el pedido.

| Verificación | Resultado | Alcance real |
| --- | --- | --- |
| Línea base `gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain` | `BUILD SUCCESSFUL in 1s`, tareas `UP-TO-DATE` | Suite previa de 10 JVM y APK anterior |
| Primer núcleo `:app:testDebugUnitTest --console=plain` | `BUILD SUCCESSFUL in 5s`; 29 pruebas sin fallas | 10 existentes + 16 DSP + 3 WAV; antes de añadir 2 casos de rango/ambigüedad |
| Compilación final `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | `BUILD SUCCESSFUL in 7s`; **31/31 JVM**, cero fallas | `CoreTest` 10, `AudioAnalysisTest` 18, `WavReaderTest` 3; APK app/test generados |
| `adb -s emulator-5554 shell am instrument -w ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner` | **OK (10 tests)**, 17,245 s | 4 persistencia/audio, 3 navegación y 3 nuevas: WAV recuperado de solo lectura, error WAV dañado y cancelación al salir |
| Tras precalcular centroide/bandas en worker: instrumentación focalizada con `-e class ar.didgetaller.ui.SessionNavigationTest#recoveredAudioCanBeAnalyzedWithoutChangingWavOrIndex` | **OK (1 test)**, 20,061 s | Reanálisis completo de WAV recuperado, resultado y panel, bytes e índice idénticos antes/después; captura real de la UI |

Todos los APK se instalaron explícitamente en `emulator-5554` con `install -r`; ADB también listaba el Samsung, pero **ningún comando se dirigió al teléfono físico en esta fase**. No hubo instrumentación, instalación, borrado, migración ni reproducción en Samsung. Las pruebas usan directorios/preferencias propios y no cambian la Biblioteca existente. No se corrió `connectedDebugAndroidTest`.

### Señales y tolerancias verificadas

- FFT frente a una DFT compleja independiente de 32 puntos: error absoluto ≤1e−10 por componente.
- PSD digital: seno de amplitud 0,5 y frecuencia exactamente en bin → potencia integrada 0,125 ±0,00002; duplicar amplitud → +6,0206 ±0,002 dB. Centroide de mezcla conocida ponderada por potencia ±0,04 Hz; bandas suman 1 ±1e−12.
- Senoides 65,41 / 69,30 / 73,42 Hz: F0 ±0,25 Hz y nota equivalente correcta; picos aislados Hann/interpolados ±0,12 Hz. Son tolerancias sintéticas, no una especificación de precisión del micrófono.
- H1 débil/H1 ausente con H2+H3 dominantes: F0 común recuperada y H2 más intensa; no se fabrica pico H1. Solo componentes pares: se acepta la periodicidad observable 138,6 Hz, no se impone 69,3 Hz. Desvío de H2 a 141,4 Hz y pico ajeno a 333 Hz se distinguen.
- Ruido moderado reproducible y modulación de amplitud: F0 ±0,35 Hz; ruido solo, DC y silencio no producen resumen ni órdenes armónicos confiables. Barrido suave refleja cambio; selección de intervalos antes/después de un cambio de nota no mezcla el otro registro. 440 Hz a 48 kHz funciona con rango configurado hasta 1000 Hz.
- Reanálisis idéntico produce serie/resumen/PSD idénticos; omitir visualizaciones no cambia Hz/confianza del detector en vivo. WAV PCM16 del encoder anterior, estéreo con bloque JUNK impar, muestreo real, lecturas arbitrarias, EOF, formato inválido y truncado cubiertos.
- Fuente virtual de **15 minutos**: se solicitan solo buffers de 32768 muestras y la cancelación se respeta tras tres lecturas. No es grabación real de 15 minutos ni benchmark completo de análisis largo.

Captura real: [`screenshots/analisis-wav-sintetico-emulador.png`](screenshots/analisis-wav-sintetico-emulador.png), obtenida por la prueba sobre un **seno sintético de 66,1 Hz / 20 s** como WAV recuperado. Se observan 53 ventanas, F0/centroide y H1 calculados. No representa al PVC ni un espectrograma implementado.

APK fase A: `app/build/outputs/apk/debug/app-debug.apk`, 13.742.147 bytes, SHA-256 `B8B711A0B6ABB7C5B82CE62B1985409B3C468F3455E4BFE3AD5B39E513FCD857`; continúa `2.1.0-beta1`/3. Documentación científica/limitaciones en [AUDIO_ANALYSIS.md](AUDIO_ANALYSIS.md), [HARMONICS.md](HARMONICS.md) y [ACOUSTIC_VALIDATION.md](ACOUSTIC_VALIDATION.md).

Pendientes de esta fase: contraste con software externo y audio físico etiquetado, precisión/ruido/procesamiento automático del Samsung, fuentes grandes/TalkBack del panel nuevo y rendimiento completo de WAV de 15 minutos. SciPy no está instalado (`ModuleNotFoundError`) y no se usó como oráculo ejecutado. Sí se verificaron fórmulas por DFT/potencia analítica. Fases pendientes: visualizaciones interactivas/tiles STFT (B), selección/seek/bucle/caché persistente (C), comparador (D), asociación a diseños/validación física del modelo (E), swipe/Deshacer persistente/tarjetas y WAV progresivo para 15 minutos (F). Captura real sigue limitada a 60 s y conserva su implementación actual.

## 20/10/2026 — Corrección del back stack del detalle de sesión

Causa: `LibraryScreen` guardaba un `selectedId` local y reemplazaba la lista por el detalle sin crear un destino. Android no tenía una entrada anterior para Atrás. Se reemplazó esa selección por `NavHost`/`NavController` de Navigation Compose 2.9.0: `measure`, `lab`, `library`, `session/{id}`. La lista conserva su entrada y su `rememberScrollState`. La flecha Material de la barra superior hace `popBackStack()`; Atrás lo resuelve el propio NavHost. Un listener de cambios de destino detiene captura y reproducción y libera MediaPlayer. No se agregó un BackHandler, ni se modificaron motores o formatos de persistencia.

| Verificación | Resultado y alcance |
| --- | --- |
| Línea base `gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain` | `BUILD SUCCESSFUL in 1s`; tareas anteriores al cambio actualizadas |
| `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` tras la corrección final | `BUILD SUCCESSFUL in 10s`; 10 pruebas JVM, cero fallas/errores; APK app y test compilados |
| Primer intento instrumentado en API 37 | Las 4 pruebas existentes pasaron; las 3 nuevas fallaron antes de interactuar con UI por `NoSuchMethodException: InputManager.getInstance` en Espresso transitivo 3.5.0. Se fijó `espresso-core:3.7.0`, que elimina esa reflexión según las [notas oficiales](https://developer.android.com/jetpack/androidx/releases/test). Sin cambios de Compose, SDK ni algoritmos. |
| `gradlew.bat :app:assembleDebugAndroidTest --console=plain` tras ajustar Espresso | `BUILD SUCCESSFUL in 6s` |
| `adb -s emulator-5554 shell am instrument -w ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner` | Ejecución final `OK (7 tests)` en 32,903 s. 4 pruebas de persistencia/reproducción y 3 de navegación, incluido el borde inferior tras unificar barras; **solo emulador API 37**. |
| `SessionNavigationTest`: tecla Atrás de Android y flecha superior | Lista desplazada hasta la décima tarjeta para Atrás y hasta la última para la flecha; audio WAV sintético reproduciéndose. Ambos caminos vuelven a Biblioteca → Sesiones con el mismo offset (tolerancia 1 px), actividad `RESUMED`, ID de reproducción nulo y posición cero. Abrir/cerrar de nuevo no duplica entradas. |
| Comprobación del borde inferior | Al ampliar la prueba a la última tarjeta se detectó un salto de 6 px (`4501 → 4495`) debido a alturas diferentes de barra superior durante la transición. Se sustituyó la barra artesanal del listado por la misma `TopAppBar` Material del detalle. La prueba conserva el caso de la última tarjeta. |
| Cambio de sección durante reproducción | Libera reproducción; volver a Biblioteca restaura la lista y el offset sin reabrir el detalle abandonado. |
| Recorrido adicional en MainActivity del emulador | Biblioteca → sesión previa → Atrás mediante `input keyevent 4`; Sesiones visible y MainActivity todavía en primer plano. Captura real de la flecha en `screenshots/sesion-navegacion-emulador.png`; la sesión mostrada contiene silencio. |

Las pruebas nuevas usan `ComponentActivity`, un Application con directorio `navigation-tests` y preferencias `navigation-tests-local_data`, y limpian solo esos datos propios. No alteran la Biblioteca existente. La clase nueva exige hardware de emulador y los comandos siempre especificaron `-s emulator-5554`; no se usó `connectedDebugAndroidTest`.

APK de esta corrección: `app/build/outputs/apk/debug/app-debug.apk`, 13.742.147 bytes, SHA-256 `6C4125780B91E1A9EF6C534D2F34629038DE674809047FA183A80F04E83B0DA2`. Sigue identificado como `2.1.0-beta1`/3. Pendiente: validar manualmente el gesto predictivo Atrás en el Samsung y la reproducción audible; las pruebas cubren la tecla Atrás Android y el estado real de MediaPlayer en emulador.

Instalación en Samsung con la autorización de actualización `install -r` ya otorgada por el usuario: primero se comprobó que Didge Taller no estuviera en primer plano. `adb -s R5CT21KTQ8W install -r app/build/outputs/apk/debug/app-debug.apk` respondió `Success`. El SHA-256 del `base.apk` instalado coincidió con el indicado arriba y el checksum de `shared_prefs/local_data.xml` fue idéntico antes y después (`LOCAL_DATA_CHECKSUM_UNCHANGED=True`). No se lanzó instrumentación ni se borraron datos en el teléfono; la instalación no constituye validación manual del gesto ni del audio.

## 20/10/2026 — Flujo Medir y sesiones (iteración actual)

Entorno: Windows 11, JBR 21.0.11 (`JAVA_HOME`), Android SDK Platform 35 y Build Tools 35.0.0. ADB mostró **Samsung R5CT21KTQ8W** y **emulator-5554**; la validación instrumentada y los recorridos anteriores se hicieron explícitamente en `emulator-5554`. La instalación autorizada posterior en Samsung se registra abajo.

| Verificación | Evidencia | Alcance |
| --- | --- | --- |
| Línea base antes de modificar: `gradlew.bat :app:testDebugUnitTest --console=plain` | `BUILD SUCCESSFUL in 30s`; 7 pruebas JVM previas | Núcleo anterior |
| Línea base antes de modificar: `gradlew.bat :app:assembleDebug --console=plain` | `BUILD SUCCESSFUL in 8s` | APK anterior |
| APK final: `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | `BUILD SUCCESSFUL in 49s`; 10/10 pruebas JVM, APK de app y test, versión `2.1.0-beta1` / código 3 | Compilación y pruebas JVM; no valida sonido real |
| Instrumentación con `adb -s emulator-5554 shell am instrument -w ar.didgetaller.test/androidx.test.runner.AndroidJUnitRunner` | `OK (4 tests)` en 1,348 s | Persistencia aislada de JSON antiguo, WAV/sesión nueva, WAV antiguo, WAV huérfano recuperado y reproducción/pausa/reinicio de WAV sintético; **solo emulador** |
| Recorrido manual en emulador: Medir → iniciar → silencio 14 s → detener → Biblioteca → detalle | Sesión privada `0a231c90-614a-484b-93a6-821fcf266856.wav`, 1.269.804 bytes, duración 14.396 ms; índice JSON con `timeline` de silencios y sin `summary`; UI indicó nota no determinada | Captura y guardado de una sesión sin señal; no hay nota física ni audio audible |
| Detalle: reproducir/pausar | El control cambió a Pausar y volvió a Reproducir; se vio contador/cursor por estado | No se verificó sonido audible ni sincronía exacta |
| Cierre automático a 60 s | La UI volvió a Iniciar; WAV privado de **5.292.044 bytes**, tamaño exacto de 60 s a 44,1 kHz/PCM16 + 44 bytes | Silencio del emulador; no instrumento |
| Cierre al salir | Al tocar Inicio durante una sesión, `onStop` generó WAV privado de **1.081.388 bytes**; la app volvió a reposo al reabrir | Salida normal; no cubre cierre forzado del proceso |
| Persistencia y UX final | Tras `am force-stop` y reapertura, la sesión previa seguía en Biblioteca; Vista amplia mantuvo el botón fijo; a 900×1800 y fuente 1,3× Iniciar/Detener siguió visible | Comprobación visual del emulador; no TalkBack |
| Renombrar, exportar y borrar sesión | Se añadió `_PVC` al nombre; SAF guardó copia de **1.081.388 bytes** en Descargas. Tras confirmar Borrar, desapareció el WAV privado y la copia exportada quedó presente | Recorrido de UI solo en emulador |

Capturas reales del emulador en [`screenshots/`](screenshots/): `medir-reposo-emulador.png`, `medir-vista-amplia-emulador.png`, `medir-pantalla-chica-emulador.png`, `medir-activa-emulador.png`, `sesiones-emulador.png` y `sesion-detalle-emulador.png`. Son capturas de interfaz sobre silencio; ninguna representa una nota medida. El resumen de sesión se verificó con señales sintéticas en JVM y persistencia Android, pero no con un didgeridoo real.

En la primera ejecución de las pruebas instrumentadas tras añadir recuperación de WAV, una aserción falló porque asumía que el registro legado aparecería primero en la lista; la lista se ordena por fecha y había archivos de prueba anteriores. Se corrigió la aserción para buscar por ID y se aislaron/limpiaron solo los archivos y preferencias propios de las pruebas. La compilación final y la segunda ejecución instrumentada pasaron. La recuperación no rellena datos de nota/frecuencia ausentes.

Tras esa compilación se abrió `ar.didgetaller/.MainActivity` con `adb -s emulator-5554 shell am start`; el proceso siguió activo (`pidof ar.didgetaller`). La búsqueda de los rótulos antiguos «Afinar», «Guardar medición», «Grabar audio» y «Activar micrófono» en el código visible no devolvió coincidencias.

Pendiente: accesibilidad/TalkBack, rotación, audio audible, nota física contra afinador independiente, estabilidad con señal real y sincronía exacta del cursor durante reproducción. El cursor está implementado usando la posición de MediaPlayer y los tiempos de lectura, pero solo se inspeccionó el estado del control sobre silencio. No correr `connectedDebugAndroidTest`, `pm clear`, desinstalación, reinstalación ni pruebas instrumentadas en el Samsung sin autorización explícita. Usar el equipo físico solo para pruebas manuales no destructivas.

APK final: `app/build/outputs/apk/debug/app-debug.apk`, 10.553.108 bytes, SHA-256 `73E1B02F43751F6C09AB5F7BB3AEE60BB6998F5D5469135DFFB61C3A08468D0D`. El emulador se restauró a 1080×2400 y escala de fuente 1,0 al terminar.

### Instalación autorizada en Samsung — 20/10/2026

El usuario proporcionó expresamente el comando `adb install -r` para el APK debug. Había dos dispositivos conectados, por lo que se añadió `-s R5CT21KTQ8W` para seleccionar el Samsung. Antes tenía `versionCode=2`, `versionName=2.0.0-beta1`. `adb -s R5CT21KTQ8W install -r .\app\build\outputs\apk\debug\app-debug.apk` respondió `Success`; `dumpsys package ar.didgetaller` confirmó `versionCode=3`, `versionName=2.1.0-beta1`. El primer `am start` arrancó un proceso que dejó de figurar poco después; no se observó excepción fatal en el registro inspeccionado, por lo que la causa no se puede atribuir. Se repitió `am start -W`: `Status: ok`, `LaunchState: COLD`, `TotalTime: 631 ms`; a los 2 s la actividad estaba en primer plano y el proceso activo. No se usó `pm clear`, desinstalación ni instrumentación en el Samsung. La instalación y el arranque del proceso no verifican audio real, datos previos ni reproducción audible.

## 20/10/2026 — Rediseño y validación parcial

Entorno Windows 11: Temurin JDK 17.0.20.1; Android SDK Platform 35 y Build Tools 35.0.0 presentes. AGP 8.13.2, Gradle 8.13, Kotlin 2.2.21; no se cambiaron algoritmos `PitchDetector`, `AcousticModel`, `Music` ni `BoreOptimizer`.

| Acción | Resultado | Alcance |
| --- | --- | --- |
| Línea base `:app:testDebugUnitTest --console=plain` | `BUILD SUCCESSFUL in 43s`, siete tests existentes, todos `UP-TO-DATE` | Suite JVM previa, antes de editar |
| Línea base `:app:assembleDebug --console=plain` | `BUILD SUCCESSFUL in 3s` | APK previo |
| Rediseño `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | `BUILD SUCCESSFUL in 27s`, siete tests JVM aprobados, APK app y test generados | Compilación y pruebas locales, no interacción |
| Primer `:app:connectedDebugAndroidTest --console=plain` | Dos fallas: `FileNotFoundException` en directorio privado de `ar.didgetaller.test` y `SharedPreferences.commit()` fallido; el proceso instrumentado no tenía permiso de escritura allí | Error del aislamiento de pruebas, corregido usando contexto de la app con nombres de prueba separados |
| Segundo `:app:connectedDebugAndroidTest --console=plain` | `BUILD SUCCESSFUL in 13s`; 2/2 pruebas en Samsung SM-G990E, Android 16 | Verificó lectura de JSON antiguo de mediciones/diseños, guardado/borrado y persistencia/renombrado/borrado de WAV aislados; **no probó audio capturado ni la UI** |
| `:app:installDebug --console=plain` | `BUILD SUCCESSFUL in 5s`; APK instalado en SM-G990E | Instalación, no prueba de uso |

**Incidencia grave de conservación de datos.** Después de `connectedDebugAndroidTest`, `adb shell pm list packages ar.didgetaller` no devolvió la app y `adb shell am start -n ar.didgetaller/.MainActivity` devolvió `Error type 3`. Tras instalarla otra vez, `run-as ar.didgetaller ls shared_prefs` devolvió “No such file or directory”; `run-as` mostró solo `cache` y `code_cache`. El usuario confirmó que en este mismo dispositivo estaban guardadas las lecturas 66,9 y 66,1 Hz. Una búsqueda de archivos cuyo nombre contiene “didge” en `Download` y `Documents` no encontró exportaciones. Por lo observado, la tarea instrumentada quitó la instalación y se perdieron los datos privados anteriores. **No repetir `connectedDebugAndroidTest` en el teléfono del usuario.** El usuario eligió **no reconstruir** las lecturas. No se escribieron registros sustitutos ni se inventaron fechas.

El Samsung estaba bloqueado al intentar abrir la app; `uiautomator dump` mostró `com.android.systemui`, por lo que no hay capturas ni validación de navegación del rediseño en ese equipo. Sus permisos reales, micrófono físico, estabilidad, WAV acústico, reproducción audible, exportación, rotación, TalkBack y precisión siguen pendientes. El usuario informó antes que había instalado una versión anterior, probado afinador y guardado mediciones C2 66,9 y 66,1 Hz; esto es evidencia comunicada por el usuario, no una prueba realizada aquí.

### Emulador Android aislado — 20/10/2026

AVD `Medium_Phone_API_37.0`, 1080×2400, densidad 420; también 900×1800 con escala de fuente 1,3. Se instaló el APK con `adb -s emulator-5554 install -r`; no se volvió a usar `connectedDebugAndroidTest` en el Samsung. Capturas reales, sin maquetas, en [`screenshots/`](screenshots/).

| Escenario en el emulador | Evidencia observada |
| --- | --- |
| Navegación principal | Afinar, Laboratorio y Biblioteca visibles y accesibles; pestañas internas Diseñar, Espectro, Buscar, Guía y categorías de Biblioteca observadas mediante `uiautomator dump`. |
| Permiso denegado | Diálogo Android visible; al denegar, Afinar mostró “Permiso denegado” y mantuvo Guardar medición desactivado. |
| Permiso concedido y silencio | Al permitir, Afinar mostró “Escuchando · sin señal”, sin frecuencia ni nota inventada. Tras `am force-stop` y reapertura, la escucha optativa se reanudó. |
| Grabación | Se pulsó Grabar y se observó contador; al detener, apareció “Grabación guardada en Biblioteca”. Archivo privado `files/recordings/<UUID>.wav` de 1.425.452 bytes; en Biblioteca figura 16 s y 1,4 MB. El audio era silencio/entrada del emulador, sin evaluación musical. |
| Reproducción y pausa | Al pulsar Reproducir, el control cambió a Pausar; al pulsar Pausar volvió a Reproducir. No se evaluó sonido audible porque el emulador se inició sin audio. |
| Renombrar y persistir | Se añadió `_PVC` al nombre; tras `am force-stop` y reapertura, el WAV y el nombre siguieron presentes. |
| Exportar WAV | El selector `ACTION_CREATE_DOCUMENT` guardó el WAV en Descargas del emulador; archivo de 1.425.452 bytes y mensaje “Archivo exportado”. Android sustituyó `/` y `:` del nombre propuesto por `_`. |
| Eliminar WAV | Apareció diálogo de confirmación; al confirmar, Biblioteca quedó vacía y `files/recordings` sin el archivo privado. La copia exportada se conserva. |
| Optimización UI | Buscar mostró “Generación 6 / 16”, terminó con candidato y error ponderado 92,4 cents (simulación), y ofreció aplicar el perfil al diseñador. No es una medición física. |
| Pantalla pequeña / letra grande | A 900×1800 y fuente 1,3× se observó que el control de escucha quedaba inicialmente fuera de vista; se movieron Activar/Detener escucha y Guardar medición a una franja fija y se compactó el estado sin señal. La captura posterior muestra ambos controles visibles. |

**Límites de estas pruebas:** no se tocó un didgeridoo, no se comprobó frecuencia real ni calibración, ni sonido de reproducción, TalkBack, dispositivos de otras marcas o pérdida de recursos durante uso prolongado. Las pruebas de persistencia instrumentadas corrieron antes del último ajuste visual; el ajuste visual pasó las siete pruebas JVM y compilación final. Las capturas de emulador no sustituyen una captura del Samsung desbloqueado.

Tras fijar controles y reiniciar el desplazamiento, `:app:testDebugUnitTest :app:assembleDebug --console=plain` terminó `BUILD SUCCESSFUL in 4s`; ese APK se recorrió en emulador. Luego se quitó un límite heredado que descartaba mediciones al superar 200 y diseños al superar 100 entradas. La compilación y siete pruebas JVM finales terminaron `BUILD SUCCESSFUL in 2s`. **APK actual:** `app/build/outputs/apk/debug/app-debug.apk`, 10.485.510 bytes, SHA-256 `DFF0B2B2871392844580B2C54BCC060D2B7762F84D27DA72FB02BF043F5C2742`. El último cambio de retención no se repitió como prueba instrumentada.

El APK actual se instaló en el Samsung con `adb -s R5CT21KTQ8W install -r ...` (`Success`). La versión inmediatamente anterior se recorrió en emulador y generó las capturas (su único cambio posterior fue quitar el límite de retención). El Samsung seguía bloqueado; la instalación no prueba la interfaz ni el audio real. El usuario pidió dejar de solicitar permiso en cada comando ADB; no se hicieron más inspecciones ADB después de instalar el APK actual.

## Comprobado en el entorno de construcción inicial (registro histórico)

- `kotlinc app/src/main/java/ar/didgetaller/core/*.kt docs/PURE_JVM_CHECK.kt -include-runtime -d /tmp/didge-core.jar` — OK, Kotlin 1.9.0 + OpenJDK 21.
- `java -jar /tmp/didge-core.jar` — OK: 50 Hz → 50.00 Hz; 65.4 Hz → 65.40 Hz; 73.416 Hz → 73.42 Hz; 82.407 Hz → 82.41 Hz; 98 Hz → 98.00 Hz; 125 Hz → 125.01 Hz; confianza ≈1 en sonidos sintéticos limpios.
- Simulador, preset cilindro 1500×40 mm — pico inicial 57 Hz y series de resonancias estimadas en frecuencias superiores. No calibrado en un instrumento real.
- Encoder WAV RIFF: cabecera/longitud verificadas sobre muestras sintéticas.
- `./gradlew ...` — **NO ejecutado**: falta Android SDK/entorno Gradle completo, no se dispone de acceso a descargar dependencias en el contenedor.
- Android Studio Sync/APK/UI/emulador/micrófono: **NO verificados**.

## Verificación Windows del 19/10/2026

Entorno: Windows 11; `JAVA_HOME` temporal apuntando a Temurin JDK 17.0.20.1. Android SDK en `C:\Users\Raclima\AppData\Local\Android\Sdk`, con Platform 35 (`android.jar`), Build Tools 35.0.0 (`aapt2.exe`) y Platform Tools (`adb.exe`). El JBR de Android Studio y el Java predeterminado son 25; se eligió JDK 17 para ejecutar Gradle. El proyecto compilado usa AGP 8.13.2, Kotlin 2.2.21, Compose BOM 2025.04.01 y distribución Gradle 8.13. Esto difiere de los números 8.9.2/8.11.1 indicados en `AGENTS.md`; no se cambiaron versiones porque la compilación funcionó. El script de arranque descargó el JAR del wrapper de Gradle 8.11.1 y verificó su SHA-256 `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`; ese JAR arrancó la distribución 8.13 declarada en `gradle-wrapper.properties`.

| Fecha | Entorno/dispositivo | Comando o escenario | Resultado exacto | Evidencia/error |
| --- | --- | --- | --- | --- |
| 19/10/2026 | Windows 11, JDK 17, SDK 35 | `gradlew.bat :app:testDebugUnitTest --console=plain` | `BUILD SUCCESSFUL in 1m 30s`; 7 pruebas, 0 fallas, 0 errores, 0 omitidas | `app/build/test-results/testDebugUnitTest/TEST-ar.didgetaller.core.CoreTest.xml` |
| 19/10/2026 | Windows 11, JDK 17, SDK 35 | `gradlew.bat :app:assembleDebug --console=plain` | `BUILD SUCCESSFUL in 39s`; APK de 10.418.846 bytes | `app/build/outputs/apk/debug/app-debug.apk`; SHA-256 `E253B1AA42527112F4A1DF42B3E9EEB94FC761E3BAC053E2A104686D774D9C42` |
| 19/10/2026 | Windows 11, JDK 17, SDK 35 | `gradlew.bat :app:test --console=plain` | `BUILD SUCCESSFUL in 21s`; 7 pruebas debug y 7 release, todas aprobadas | Informes XML en `app/build/test-results/testDebugUnitTest/` y `testReleaseUnitTest/` |
| 19/10/2026 | APK generado | `aapt.exe dump badging app-debug.apk` | Paquete `ar.didgetaller`, `compileSdk 35`, `minSdk 26`, `targetSdk 35`, actividad `ar.didgetaller.MainActivity` | Lectura del APK empaquetado |
| 19/10/2026 | APK generado | `aapt.exe dump permissions app-debug.apk` | `RECORD_AUDIO` declarado; no se declaró `INTERNET` | Lectura de permisos del APK empaquetado |
| 19/10/2026 | ADB | `adb.exe devices` | Lista vacía: ningún Android conectado | No se instaló ni ejecutó el APK |
| pendiente | Android físico o emulador | Permisos, micrófono, WAV, diseñador, CSV y navegación | No probado | Requiere dispositivo; pedir autorización antes de instalar en uno físico |

Incidencias observadas: el primer intento de `testDebugUnitTest` no pudo descargar el wrapper (`Invoke-WebRequest: Unable to connect to the remote server`); con acceso de red autorizado descargó y verificó el JAR. El primer intento de `assembleDebug` dentro del sandbox no pudo crear `C:\.gradle\wrapper\dists\...\gradle-8.13-bin.zip.lck`; fuera del sandbox terminó correctamente. `adb devices` dentro del sandbox no pudo crear `\.android`; fuera de él mostró la lista vacía. Gradle avisó que `kotlinOptions.jvmTarget` está obsoleto y que `libandroidx.graphics.path.so` se empaquetó sin quitar símbolos; ninguna advertencia impidió la compilación.

Alcance: se verificaron compilación, pruebas unitarias locales y metadatos del APK. El repositorio solo contiene `CoreTest.kt` como suite de pruebas; no hay pruebas instrumentadas en `androidTest`. No se verificaron Android Studio Sync por separado, interfaz en dispositivo, permisos reales, captura de audio, exportación de archivos ni precisión acústica física.

No informar “listo, compila” hasta que ambas tareas Gradle hayan terminado correctamente. No inventar APK si no existe.

## 19/10/2026 — Funciones inspiradas en DidgeLab (sin código copiado)
- Kotlin JVM: `kotlinc core/Music.kt core/AcousticModel.kt core/BoreOptimizer.kt docs/LAB_JVM_CHECK.kt -include-runtime -d /tmp/check.jar` -> OK.
- `java -jar /tmp/check.jar` -> OK (simulación PVC 1500 x 40 mm: base ~56.8 Hz coherente con cuarto de onda; cono 1500 mm ~81.4 Hz; perfiles por puntos; cancelación; búsqueda 8 generaciones x 10 individuos redujo score de 265.7 a 40.9 cents).
- Pruebas Android/Gradle/Compose y micrófono físico: NO ejecutadas aquí por falta de SDK.
- Motor 1D: solo aproximación, sin validación cruzada todavía con DidgeLab ni instrumentos reales.
