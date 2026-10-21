# AGENTS.md — Didge Taller (Kotlin / Android)

## Continuidad 3.1 beta1 — validada, 20/10/2026 (prioridad sobre 3.0)

- Entrega UX/UI implementada y validada al nivel de `docs/VALIDATION.md` y `docs/UI_AUDIT.md`. APK **3.1.0-beta1 / 5**, mismo paquete/firma. Samsung actualizado por install -r/Success y arranque sin fatal; 7 WAV, índice, ajustes y relaciones con hashes intactos.
- Nuevos `PlotViewport`, `PlotProjection`, `PlotScales` (solo presentación), `ExplorerState` retenido por sesión en ViewModel, `PlotGestures`, `PlotSurface`, `ImmersiveViewer`. Destino `viewer/{id}/{plot}` sobre `session/{id}`; herramientas `lab/tool/{tool}` sobre Laboratorio. No agregar BackHandlers.
- Se preserva el reproductor al pasar detalle↔visor y durante cambios de configuración; salir de la sesión libera audio. MainActivity distingue `isChangingConfigurations`; la captura sigue deteniéndose/guardándose, nunca se reinicia sola.
- Chips/acciones en FlowRow, selección desplegable, resumen compacto, barras de herramientas fuera del formulario. Comparador y papelera existentes se conservan.
- Referencia espectral de color fijada durante la exploración; STFT/YIN/armónicos/motores no reescritos. Solo renderizar nuevos intervalos después de terminar el gesto; caché derivada existente.
- Checkpoint anterior: `checkpoints/before-3.1-20261020-195852.zip`; cierre recuperable indicado en `checkpoints/latest.txt`. Git ausente. Logs `checkpoints/v31-*.log`. Instrumentación exclusivamente en `emulator-5554`; Samsung `R5CT21KTQ8W`: no uninstall, pm clear, borrado privado ni restauración automática. Actualizar solo mediante install -r con firma/versión compatibles y respaldo fresco.
- Validación: **57 JVM** (13 nuevas de viewport/proyección), suite de **22 instrumentadas** más focos que completan **24 casos funcionales distintos**. Matriz 360 dp/fuentes 1,0 y 1,3; 420 dp/fuentes 1,0 y 1,5; horizontal real de MainActivity, gesto de borde y tres botones. Capturas reales en `docs/screenshots/v31-final/`. Hubo ejecuciones fallidas investigadas; no contarlas como aprobadas.
- Swipe exige 38% **de distancia al soltar** además del fin de animación. Material podía descartar un gesto corto rápido por velocidad; el veto nuevo está probado. No quitarlo. Deshacer/papelera conservan audio y relaciones hasta vaciado explícito.
- Permiso concedido al volver de Ajustes actualiza feedback sin arrancar captura. Se retira un snackbar obsoleto al empezar correctamente. Armónicos mantiene controles de audio sin dibujar un cursor temporal sobre Hz.
- **26 módulos anteriores de core/data/captura/reproducción idénticos** al ZIP inicial. Leer `docs/IMMERSIVE_VIEWER.md` antes de modificar cámara/gestos/ciclo de vida. La cámara/selección sobreviven a recreación de Activity, no se promete restaurarlas tras muerte del proceso.
- Respaldo vigente en `backups/latest.txt`: 24 archivos, 7 WAV. Tras actualizar cambiaron solo Android ActivityThread.IDS y `files/profileInstalled`; se verificaron estructura/fecha del marcador de AndroidX. No ignorar otras diferencias de hashes automáticamente.
- Próximo paso: prueba manual del usuario con PVC, sincronía audible, gesto predictivo Samsung, lectura a distancia y captura sostenida de 15 minutos/batería. TalkBack: smoke de servicio ligado y etiquetas aprobado; lectura/recorrido completo pendientes. No atribuir precisión física a los sintéticos.


## Continuidad 3.0 beta1 — 20-10-2026 (prioridad sobre registros históricos)

- Solicitud vigente: implementación integral, no detenerse en A. B–F ya tienen implementación funcional: espectrograma/onda/espectro, selección/seek/bucle, comparación A/B, relaciones diseño/sesión, marcadores manuales, papelera/Deshacer y captura progresiva hasta 15 minutos iniciales (ajustes hasta 30). No rehacer A ni volver al buffer PCM completo de 60 s.
- Leer `docs/SESSION_EXPLORER.md`, `DATA_SAFETY.md`, `VALIDATION.md`, `ROADMAP.md`. Nuevos módulos core AudioVisuals/ProgressiveWavWriter/SpectralPalette; audio SessionPlayer/SessionWorkspace; data AnalysisCache/TrashStore/SessionToolsStore; UI separada para explorador, comparador y herramientas de Biblioteca. No se agregaron dependencias/permisos ni se alteraron motores de periodicidad/simulación/evolución.
- Validación cerrada: **44 JVM** y **18 casos instrumentados distintos** aprobados mediante suite/focos solo en emulador; detalles de fallos corregidos en VALIDATION. Samsung actualizado con install -r/Success, arranque correcto; local_data, ajustes y 5 WAV con hashes intactos. Prueba física/audible queda al usuario.
- APK actual `3.0.0-beta1`/4, mismo `ar.didgetaller` y certificado debug. Resultados exactos de pruebas/instalación al inicio de VALIDATION. Las validaciones de 33 JVM y cuatro WAV corresponden a la entrega anterior; no confundirlas con la nueva.
- Protección: **instrumentación exclusivamente en emulator-5554**, con guardas de hardware y almacenamiento aislado. No `connectedDebugAndroidTest` con Samsung conectado, no uninstall, pm clear, borrado privado ni remedios destructivos por instalación fallida. Actualizar solo `adb -s SERIAL install -r`, verificando firma/versión y respaldo. Autorización de actualizar ya dada en la sesión; no pedirla nuevamente para la misma actualización segura.
- Respaldo local previo verificado de preferencias/archivos del Samsung: `backups/latest.txt`. No publicar audio ni respaldos. No restaurar encima del teléfono automáticamente. Papelera conserva WAV/índice/relaciones hasta vaciado explícito; caché es derivada y regenerable.
- Navegación: NavHost mantiene lista debajo de detalle/compare; Atrás y flecha hacen pop y liberan MediaPlayer. Preservar scroll y altura de barra superior. No introducir BackHandler para suplir destinos.
- Captura al salir se detiene y guarda; no grabación oculta. Parciales propios `.part` se reparan según longitud real al abrir; nunca reparar/reinventar WAV históricos. Reanálisis usa SHA-256/versionado sin reemplazar resúmenes históricos. Incrementar AnalysisCache.VERSION si cambian los algoritmos/normalizaciones o formato derivado.
- Límites: cursor/polling y bucle MediaPlayer no son sample-accurate/gapless; falta validación física/audible y 15 minutos sostenidos. STFT 32768 prioriza graves a costa de resolución temporal. Manuales de toot/vocalización no son detecciones automáticas. Resonancias de impedancia no equivalen a armónicos del sonido emitido.
- Git no existe en esta carpeta; no afirmar commits/diffs disponibles. Checkpoint ZIP previo en `checkpoints/`, logs de compilación/tests en `app/build/`, continuidad en WORK_LOG.

## Registro histórico — rediseño de sesiones (sustituido por la continuidad 3.0 anterior)

- Navegación actual: **Medir / Laboratorio / Biblioteca**. Medir tiene un solo botón principal: inicia micrófono, análisis y WAV juntos; al detener, a los 60 s o al salir de la app, cierra y guarda una sesión. No se reanuda la escucha automáticamente.
- El detalle de sesión es un destino real `session/{id}` en Navigation Compose, encima de Biblioteca. Atrás y la flecha superior hacen pop de la misma entrada, restauran el scroll de Sesiones y detienen/liberan el reproductor. El NavController es la fuente de navegación; no volver a reemplazar destinos mediante una selección local ni añadir un BackHandler para suplir el back stack. Corrección compilada, 10 pruebas JVM y 7 instrumentadas aprobadas solo en emulador; APK actualizado en Samsung con `install -r` autorizado y checksum de `local_data.xml` intacto. Gesto predictivo en Samsung pendiente de prueba manual.
- Biblioteca muestra **Sesiones / Diseños**. Cada sesión nueva usa WAV privado y añade a la clave `recordings` una serie temporal y, si hay lecturas suficientes, un resumen robusto. Las grabaciones antiguas y los WAV privados sin índice se muestran como audio sin análisis; las mediciones antiguas como lecturas sin audio. `measurements` y `designs` conservan su formato.
- La curva temporal usa lecturas YIN existentes y el cursor azul sigue aproximadamente la posición de MediaPlayer. No se calibró precisión, audio audible ni sincronía fina con un instrumento real. Véase `docs/VALIDATION.md`.
- Con autorización explícita del usuario, el APK `2.1.0-beta1` (código 3) se instaló en el Samsung SM-G990E mediante `adb -s R5CT21KTQ8W install -r` y la actividad arrancó. No se probaron allí captura ni reproducción. La instalación conservó la app existente; verificación de datos anteriores pendiente.
- **Política obligatoria de pruebas físicas:** no ejecutar `connectedDebugAndroidTest`, instrumentación, desinstalación, `pm clear`, reinstalación ni ninguna operación que pueda borrar datos en el Samsung real sin autorización explícita. Ejecutar pruebas instrumentadas solo en emulador; usar el teléfono físico únicamente para validación manual no destructiva. Especificar siempre `adb -s emulator-5554` (o el serial correcto del emulador) cuando ambos dispositivos estén conectados.
- Las descripciones históricas de Afinar, guardado manual y flujo WAV separado que aparecen debajo documentan versiones anteriores. El código y este estado vigente prevalecen.

> LEER PRIMERO. Codex CLI, Codex integrado en Android Studio y cualquier agente que trabaje en este repositorio deben leer este archivo y `docs/PRODUCT_CONTEXT.md`, `docs/ROADMAP.md` y `docs/VALIDATION.md` ANTES de hacer modificaciones.
> Actualizado: 20-10-2026. Idioma de comunicación: español rioplatense, claro y conciso.

## Estado actual — 20-10-2026 (leer antes de los registros históricos)

- La app pasó a tres secciones Material 3: **Afinar**, **Laboratorio** (Diseñar, Espectro, Buscar, Guía) y **Biblioteca** (Mediciones, Grabaciones, Diseños). El código fuente es la fuente de verdad si una sección histórica contradice este estado.
- Afinar muestra nota grande, estados de escucha, medidor de cents, modo de máxima legibilidad y guardado solo con señal estable. Tras el primer permiso, la escucha automática queda habilitada hasta que el usuario la detiene; `onStop` libera el micrófono.
- La grabación es explícita, se detiene manualmente o a los 60 s, y guarda WAV en almacenamiento privado. Biblioteca permite reproducir/pausar, renombrar, borrar con confirmación y exportar. No se encontró un archivo WAV persistente en la versión anterior: solo se exportaba voluntariamente mediante SAF. Las mediciones y diseños anteriores mantienen las claves `measurements` y `designs`.
- El usuario informó pruebas manuales previas de afinador y guardado, con dos lecturas iniciales del PVC: C2 66,9 Hz y C2 66,1 Hz. No son validación científica ni pruebas ejecutadas por este agente.
- En Windows, las siete pruebas unitarias y `assembleDebug` pasaron el 20/10/2026. Dos pruebas instrumentadas de persistencia pasaron en un Samsung SM-G990E (Android 16). **Incidencia importante:** tras `connectedDebugAndroidTest` la app no figuraba instalada; tras `installDebug` no apareció `shared_prefs/local_data.xml`. El usuario confirmó que este equipo contenía las dos lecturas previas y eligió no reconstruirlas. No volver a correr esa tarea sobre un teléfono de uso sin respaldo verificable. Véase `docs/VALIDATION.md`.
- El rediseño se recorrió en emulador API 37: navegación, permiso denegado/concedido, silencio, grabación WAV privada, persistencia, reproducción/pausa a nivel de control, renombrado, exportación SAF, borrado y optimización visible. Hay capturas reales en `docs/screenshots/`. Pendientes en el Samsung: interacción visual y musical, grabación con sonido real, reproducción audible, accesibilidad y precisión contra afinador independiente. No atribuirles validación por haber compilado.

## 0. Contexto y meta

**Didge Taller** es una app Android nativa, privada, gratuita/offline, hecha para alguien que toca el didgeridoo todos los días (actualmente uno de PVC) y quiere aprender a afinar, estudiar el sonido y, en el futuro, diseñar didgeridoos de madera/agave. No da por sentado que el usuario conozca teoría musical. El usuario es desarrollador con experiencia en Java; agradece explicaciones de Kotlin, Compose, Gradle y cada comando antes de usarlo. No suponer presupuesto para servicios, suscripciones, APIs o hardware.

Este repo ES la migración desde un prototipo Android Java con vistas clásicas a **Kotlin + Jetpack Compose**. No es una envoltura de WebView. El usuario trabaja en Windows 11 y desea usar **Android Studio Rabbit 1 y Codex CLI** para compilar/ejecutar/iterar.

## 1. Estado de verificación — leer y actualizar en cada iteración

- **Implementado en código fuente**: 6 pantallas Compose (Afinar, Espectro, Diseñar, Buscar, Guardados, Guía), permiso de micrófono en tiempo de ejecución, capturador local AudioRecord, detector YIN-like, espectro FFT, grabación WAV de 10 s con selector del sistema, modelo geométrico/resonancias acústicas simplificado, presets, guardado local, CSV, pruebas unitarias.
- **Verificado EN ESTE ENTORNO**: el núcleo Kotlin puro compiló con `kotlinc` 1.9.0 y pasó un ejecutable JVM (`docs/PURE_JVM_CHECK.kt`) con tonos sintéticos de 50, 65.4, 73.416, 82.407, 98 y 125 Hz, señal sin fundamental explícita, simulación PVC y cabecera WAV.
- **Verificado en Windows el 19-10-2026**: Gradle `:app:testDebugUnitTest` (7/7), `:app:test` (7/7 en debug y release) y `:app:assembleDebug` terminaron correctamente con JDK 17 y SDK 35. El APK debug está en `app/build/outputs/apk/debug/app-debug.apk`; detalles en `docs/VALIDATION.md`.
- **Registro histórico al 19/10:** no se habían verificado sincronización desde Android Studio, emulador, permisos reales, compatibilidad de móviles ni calidad acústica. Consultar el estado actual arriba y `docs/VALIDATION.md`.
- El APK se generó localmente; no se instaló en ningún dispositivo. `gradle-wrapper.jar` se descargó automáticamente del repositorio oficial y se validó contra el SHA-256 previsto por el script (requiere conexión **solo durante la instalación de herramientas**, la app no usa Internet).

## 2. PRIMERA TAREA para el siguiente agente (prioridad absoluta)

1. Revisar estos archivos y el código antes de modificarlo. Explicar brevemente comandos y banderas al usuario.
2. Verificar **JDK 17 o superior compatible**, Android SDK Platform 35 y herramientas de compilación instaladas. Preferir JDK integrado (`jbr`) de Android Studio si corresponde.
3. Desde la RAÍZ del repo ejecutar en Windows `gradlew.bat :app:testDebugUnitTest` y luego `gradlew.bat :app:assembleDebug`. En macOS/Linux `./gradlew`.
4. Si falla la resolución de dependencias o la sincronización, registrar el error exacto y corregir la causa; no actualizar todas las versiones sin diagnosticar. Stack compilado el 19-10-2026: AGP 8.13.2, Kotlin 2.2.21/Compose Compiler, Gradle 8.13, Compose BOM 2025.04.01, `compileSdk 35`, `minSdk 26`, `targetSdk 35`, Java 17. Las cifras anteriores AGP 8.9.2/Gradle 8.11.1 quedaron desactualizadas; el JAR del wrapper de arranque aún es 8.11.1 y funciona con la distribución 8.13.
5. Si el usuario conecta un Android y activa depuración USB: `adb devices`, `gradlew.bat :app:installDebug`, `adb shell am start -n ar.didgetaller/.MainActivity`, recopilar `adb logcat` ante fallas. Pedir autorización antes de instalar en el dispositivo.
6. Probar permiso de micrófono, arranque/detención, 30 segundos de funcionamiento sin crasheos, grabación WAV de 10 s, guardar/exportar, presets del diseñador y abrir diseños del historial. Validar una nota real con un afinador independiente.
7. Registrar evidencia (comandos, resultado, dispositivo, fecha, errores) en `docs/VALIDATION.md`. Marcar funcionalidades como verificadas solo después de probarlas.

## 3. Arquitectura y reglas de código

- Package/ID: `ar.didgetaller`. Mantenerlo para conservar compatibilidad con la app previa y SharedPreferences (`local_data`, claves `measurements`, `designs`). No cambiar el esquema sin migración.
- Kotlin + Jetpack Compose + Material 3, AndroidX. NO WebView, cuentas, publicidad, red, telemetría ni SDKs de pago. `AndroidManifest.xml` no incluye permiso INTERNET.
- Separar acústica pura (`core/`) de Android (`audio/`, `data/`, Activity, ViewModel, Compose UI). Mantener núcleo matemático testeable en JVM.
- Audio PCM 16-bit mono 44.1k; procesamiento fuera del hilo de UI. UI/Compose solo recibe resultados agregados; no persistir grabaciones sin acción explícita.
- El simulador `AcousticModel` ahora usa línea de transmisión 1D segmentada, pérdidas termoviscosas y radiación aproximadas, a 343 m/s. El default actual es 72 segmentos y 1 Hz; la búsqueda evolutiva utiliza 42 segmentos / 3 Hz por rendimiento. No equivale a CADSD/DidgeLab y no garantiza nota, timbre ni contrapresión real.
- El detector de tono se aproxima a YIN y puede errar con teléfonos que atenúen graves o señales con armónicos prominentes. Mostrar incertidumbre, evitar afirmar precisión clínica/profesional.
- Mantener código legible, acotado y testeable (DRY; SRP; separación de responsabilidades; arquitectura limpia sin sobreingeniería).
- Agregar o actualizar tests si se toca `PitchDetector`, `AcousticModel`, `Music`, `WavCodec`, persistencia o permisos. Evitar generar múltiples tareas/simulaciones durante cada arrastre de slider.
- El proyecto es PERSONAL y NO COMERCIAL. Es válido estudiar, adaptar o portar fuentes de DidgeLab si se cumplen CC BY-NC-SA 4.0, atribución, indicación de cambios y compartir igual. En esta entrega no se copiaron fuentes: se escribieron motores Kotlin independientes. No confundir licencia libre con prohibición de reutilizar código.
- Usar commits pequeños. No borrar archivos de la app, historial ni rediseñar el proyecto entero sin justificación. No agregar permisos de red ni enviar audios.

## 4. Referencias e inspiración (no son dependencias ni código integrado)

- **DidgeLab**: https://github.com/didgitaldoo/didge-lab — Python; simulación acústica, impedancias, geometría y búsqueda evolutiva de perfiles. Es la inspiración técnica principal. CC BY-NC-SA 4.0: NO reutilizar sus fuentes en esta app MIT sin revisar permisos.
- **Didgeridoo Physik / CADSD**: https://www.didgeridoo-physik.de/ — investigación, Didge-Creator y Didge(R)Evolution, afinación y geometría. CADSD no es una biblioteca disponible para integrar libremente. Inspiración conceptual.
- **Didgeridoo Breath**: https://www.didgeridoobreath.com/ — ejemplos reales de instrumentos de eucalipto ahuecado por termitas y agave dividido/vaciado/pegado; cuidado de madera, boquilla, acabado y afinación.
- **Didge Project**: https://www.didgeproject.com/ — didgeridoos, materiales y guías de fabricación con PVC; inspiración didáctica.
- **Andrea Ferroni / Didgeridoo Sound Design**: https://www.didgeridoo-crafting.com/ — relación geometría/resonancias/sonido; referencia teórica, no código.

Leer `docs/PRODUCT_CONTEXT.md` para la historia de decisiones e investigación y `docs/ROADMAP.md` para fases futuras y criterios de aceptación.

## 5. Próximo trabajo DESPUÉS de obtener un APK funcional

- P0: end-to-end real y bugfix (permisos, ciclo de vida, audio, WAV, archivo CSV, tests de regresión, UI en pantallas pequeñas).
- P1: calibración micrófono, detección fiable de 35–220 Hz, seguridad ante clipping/ruido, estabilidad y confirmación manual de nota, lectura de armónicos y toots tocados por separado.
- P2: diseñador con puntos de control libres, interpolación, perfiles guardados y comparar A/B con gráficos superpuestos, importar/exportar CSV con unidades explícitas.
- P3: modelo de pérdidas y radiación mejor fundamentado, ajuste a mediciones reales, algoritmos inversos/optimización **originales**. Mostrar siempre intervalos de incertidumbre.
- P4: pantalla de taller (materiales, herramientas, tiempos, boquilla, cálculos geométricos), enseñanza en español, grabaciones etiquetadas, exportación de reportes.
- P5: validación física, documentación comunitaria y mejora continua, sin monetización ni Play Store como objetivo.

## 6. Continuidad con Codex CLI

Codex lee automáticamente este `AGENTS.md` desde la raíz si se ejecuta dentro de esta carpeta. Usar `codex` dentro del directorio del proyecto y, como primer prompt: "Leé AGENTS.md y docs/*.md; compilá y corregí hasta lograr testDebugUnitTest y assembleDebug; registrá qué probaste en VALIDATION.md; después proponé el siguiente paso P0.".

Al terminar una iteración, agregar una breve sección fechada en `docs/WORK_LOG.md` con decisiones, archivos, pruebas, fallos y próximos pasos. Mantener `ROADMAP.md` con checks reales. No trabajar en background ni afirmar avances no ejecutados.

---

## 7. ACTUALIZACION PRIORITARIA 19-10-2026 — SIMULACIÓN TIPO DIDGELAB (LEER ANTES DE TODO)

**CORRECCIÓN DE OBJETIVO DEL USUARIO:** Didge Taller es un proyecto estrictamente **PERSONAL / NO COMERCIAL**. El usuario dijo expresamente: "yo no quiero nada comercial". No insistir con venta de apps, licencias comerciales, tiendas ni monetización. Quiere estudiar las capacidades de DidgeLab (https://github.com/Didgitaldoo/didge-lab), programar una app nativa Android similar con Kotlin, aprender acústica y construir sus propios didgeridoos. La intención es reutilizar ideas y estudiar implementaciones públicas como hacen los desarrolladores; dar atribución y respetar las condiciones de las licencias, sin presentarlo como plagio.

**IMPLEMENTADO EN ESTA ENTREGA (código nuevo):**
- `core/AcousticModel.kt`: un motor Kotlin original, sin código Python copiado, con modelo de línea de transmisión acústica 1D para segmentos, pérdidas termoviscosas aproximadas, carga de radiación de campana aproximada, impedancia de entrada y picos de resonancia interpolados. Perfil variable por puntos `BorePoint` con longitudes/diámetros en milímetros, preservando el modelo anterior de 3 diámetros para retrocompatibilidad.
- `core/BoreOptimizer.kt`: búsqueda evolutiva original de perfiles desde objetivos de 3 resonancias (drone + 2 resonancias superiores), población, mutación, elitismo, función de pérdida en cents, semilla reproducible, cancelación y progreso.
- `ui/TallerApp.kt`: editor de puntos del conducto y nueva pestaña `Buscar` para diseño inverso de 3 resonancias, progreso, cancelación, resultado y aplicar al diseñador.
- `TallerViewModel.kt`: búsqueda en executor de fondo independiente; no congelar la UI. Cancelación por token. `data/LocalStore.kt`: guarda/lee puntos personalizados con compatibilidad con JSON anterior.
- `docs/LAB_JVM_CHECK.kt`: validación JVM reproducible del modelo y la búsqueda.

**VALIDADO EN LA ENTREGA ANTERIOR:** `kotlinc` compiló AcousticModel, BoreOptimizer, Music y LAB_JVM_CHECK; `java -jar` pasó simulación de cilindro y cono, puntos, búsqueda evolutiva, mejora del fitness y cancelación. El experimento 8 generaciones ×10 individuos redujo error ponderado de 265.7 a 40.9 cents (para semilla fija); estos son valores de *simulación*, no resultados acústicos medidos. **Actualización 19-10-2026:** Gradle/Compose compiló, pasaron las pruebas JUnit y se generó el APK debug; dispositivo real y funciones físicas siguen sin verificar. NO afirmar que esto sea DidgeLab 3 portado; el modelo no incorpora conicidad analítica por segmento ni todas las funciones de DidgeLab.

**REFERENCIA Y LICENCIAS:** DidgeLab en GitHub se publica bajo CC BY-NC-SA 4.0, que autoriza adaptación no comercial con atribución y compartir igual para adaptaciones. ESTE código Kotlin es creación nueva con ecuaciones físicas clásicas y una búsqueda propia, por lo que por ahora se conserva la licencia propia del repositorio; NO se copiaron fuentes de DidgeLab. Si en el futuro se porta código de DidgeLab literalmente o de forma derivada, conservar avisos, atribuciones, indicar cambios, cumplir BY-NC-SA y revisar la compatibilidad de la licencia del paquete distribuido. Ni los modelos matemáticos generales ni una funcionalidad similar son en sí mismos plagio.

### Próxima tarea para Codex

1. Ejecutar `gradlew.bat :app:testDebugUnitTest` y `gradlew.bat :app:assembleDebug` en Android Studio / Windows con SDK 35; arreglar errores reales y anotar logs en `docs/VALIDATION.md`. **Primero esto, antes de agregar más funciones.**
2. Asegurar que el editor avanzado conserva los puntos al arrastrar controles, la UI escala en pantallas pequeñas y las seis pestañas son navegables.
3. Probar exportación CSV y reimportación (la reimportación todavía no está); probar JSON de nuevos `points` y diseños antiguos.
4. Contrastar simulación con un cilindro teórico de cuarto de onda, un cono y resultados de referencia de DidgeLab para geometrías idénticas. Ajustar tolerancias/condiciones de radiación y pérdidas, documentando claramente diferencias.
5. Después: import/export de geometrías estilo DidgeLab, objetivos de impedancia completos (no solo 3 picos), reporte de fitness por objetivos y optimización multietapa/algoritmos genéticos configurables.
6. Mantener el proyecto sin red, sin cuentas y no comercial; explicar los términos desconocidos en español y no asumir formación musical del usuario.

Véase `docs/DIDGELAB_STUDY.md` y `docs/VALIDATION.md`. Esta sección reemplaza las menciones antiguas que planteaban monetización o una eventual publicación comercial: ese NO ES EL OBJETIVO.
