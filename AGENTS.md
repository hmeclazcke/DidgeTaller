# AGENTS.md — Didge Taller (Kotlin / Android)

## Git obligatorio — 21/10/2026 (regla permanente y prioritaria)

Estas reglas reemplazan las indicaciones históricas de Git ausente o de no crear un commit del estado acumulado. El punto de partida preservado es `3f0510f` (`3.2.4-beta1/10`); el commit histórico `4f90893` se conserva intacto.

- Antes de modificar código, revisar `git status`, los cambios y `.gitignore`. Si hay trabajo previo sin registrar, crear un commit de punto de partida con los archivos del proyecto que corresponda versionar, excluyendo privados y generados. Si el árbol ya está limpio, usar el commit existente; no crear commits vacíos.
- Antes del primer commit de cada iteración, verificar `git config --get user.name`, `git config --get user.email` y la identidad efectiva con `git var GIT_AUTHOR_IDENT` / `git var GIT_COMMITTER_IDENT`. Usar exclusivamente la identidad configurada por el usuario; no inventar nombres/correos, usar otro autor ni modificar la configuración global. Si falta identidad o hay una identidad efectiva diferente, informar antes de crear commits.
- Después de cada fase funcional terminada, compilable y con sus pruebas pertinentes aprobadas, crear un commit pequeño con mensaje descriptivo, por ejemplo `fix(audio): restore reliable didgeridoo detection`, `fix(charts): recover fundamental rendering`, `fix(ui): restore confidence colors` o `test(audio): add harmonic variation regression tests`.
- Si una fase falla, conservar los cambios y documentar el error y el estado real; no crear un commit que la presente como validada. El commit de partida preserva trabajo existente y debe describir honestamente qué verificaciones se ejecutaron, sin atribuir nuevas validaciones a evidencia histórica.
- Preparar rutas explícitas con `git add -- <rutas>` o seleccionar fragmentos; nunca usar `git add -A` indiscriminadamente. Antes de cada commit revisar `git diff --cached --stat`, `git diff --cached`, `git diff --cached --check` y los nombres preparados. Inspeccionar también los archivos nuevos y binarios; `.gitignore` no protege archivos ya rastreados.
- No incluir WAV personales, otros audios privados, respaldos, fixtures/diagnósticos privados, contraseñas, claves, credenciales, archivos locales sensibles, builds ni cachés. Conservar el wrapper de Gradle y las evidencias de UI revisadas que correspondan a documentación del proyecto. No borrar archivos privados para limpiar el estado de Git: excluirlos.
- Nunca usar `git reset --hard` para descartar cambios sin autorización, ni `git clean -fd` indiscriminadamente. No modificar ni borrar commits históricos, hacer force push ni enviar a un remoto sin autorización explícita. No usar amend/rebase para reescribir el historial existente.
- Al terminar, crear un commit final solo si quedan cambios pendientes y verificados. Informar el hash abreviado y el mensaje de cada commit creado, el estado final de `git status` y la versión o commit recomendado para recuperación. Una recuperación debe preservar primero el trabajo posterior; no implica autorización para descartar cambios.
- Registrar comandos, resultados y límites en `docs/VALIDATION.md` y decisiones/pendientes en `docs/WORK_LOG.md`. Mantener el historial real y recuperable, sin reconstruir fases históricas como si se hubieran ejecutado ahora. No hacer push automáticamente.

## Reparación selectiva de Fundamental 3.2.4 — 21/10/2026

- Prioridad sobre la política visual3.2.3: se revierte el umbral global0,85 a0,70 y el corte obligatorio por incertidumbre. `PitchTrace` conserva originales y enlaza solo extremos medidos coherentes (≤80cents,≤4 omisiones,≤1s); ausencias reales/intervalos largos cortan. No crear frecuencias intermedias ni promover ruido prolongado.
- Didgeridoo: excursiones únicas con retorno y períodos alternativos bajos breves quedan originales inspeccionables. Reglas exactas/límites en VALIDATION; no aplicarlas indiscriminadamente a flauta. Toot de varias lecturas y melodías respaldadas permanecen. No modificar YIN ni datos para embellecer curvas.
- General sigue incluyendo todos los puntos de la tendencia con contexto; inciertos no dominan la escala. Reset/cámaras/gestos intactos. Tarjeta/detalle/fullscreen comparten `PitchTrace`, sin decimación de F0. «N fuera» sale de la interfaz principal; información completa en herramientas.
- Eje: números sin Hz repetido, una unidad Hz y nota secundaria; medidas reales y margen4+4dp conservados. Mismo detalle420dp: reserva115,55→74,55px, datos+5,33%. Leyenda «Línea: tendencia», sin párrafo permanente debajo.
- APK **3.2.4-beta1/10**, Samsung `R5CT21KTQ8W` install-r/Success y arranque correcto; respaldo fresco61 archivos/13 WAV,61 hashes intactos,13 sesiones/WAV indexados y legibles. Informe `checkpoints/v324-samsung-update.json`.104 JVM/39 casos UI distintos aprobados más6 repeticiones360/420dp/fuentes1,3/1,5;20 capturas en `docs/screenshots/v324-final/README.md`. Re·D2 recupera40 puntos/33 enlaces frente a17/15; Mib·Eb2,20/19 frente a12/5. Series reales exactas con WAV silencioso en emulador, no prueba acústica física.
- Solo4 main modificados/39 idénticos a3.2.3; checkpoint previo `checkpoints/before-3.2.4`, cierre recuperable `checkpoints/latest.txt`. Sin reset/stash/commit global ni cambios DSP/datos/cámara/gestos. Captura Samsung negra: validación visual/física allí pendiente; eventos de una sola lectura requieren inspección, no filtro infalible. Nunca instrumentar/clear/uninstall/restaurar Samsung.

## Fundamental Precision 3.2.3 — 21/10/2026

- APK **3.2.3-beta1/9**, mismo paquete/firma; Samsung `R5CT21KTQ8W` install-r/Success y arranque correcto. Respaldo fresco **61 archivos/13 WAV**,61 hashes intactos;13 sesiones y13 WAV indexados/legibles. Informe `checkpoints/v323-samsung-update.json`. Checkpoint recuperable `checkpoints/latest.txt`; previo `checkpoints/before-3.2.3`. No revertir cambios anteriores de Git.
- `PitchTrace`: curva exige confianza0,85; repetición no promueve incertidumbre. Conserva criterio de períodos múltiples y añade retornos aislados no armónicos con contexto/confianza degradada y bordes de nota junto a ausencias. Son inciertas, no correcciones. Hz/tiempos/confianza guardados intactos; huecos, sin interpolación; originales opcionales/cursor con razón. Ver reglas exactas en VALIDATION.
- General conserva percentiles/contexto pero extiende a TODOS los confiables: no recortar toot breve por su peso estadístico. Ajustada robusta/zoom0,2Hz/Reset preservados. Tarjeta sin decimación de F0 ni marcadores inciertos por defecto; misma clasificación/verde que visor.
- Eje de Fundamental medido según textos reales Hz/nota con Paint; margen4+4dp, congelado durante gesto/recalculado al soltar. Sin Hz superior redundante. Leyenda breve y aviso discreto; datos detalle+10,68% en fixture420dp/fuente1,0.
- **97 JVM/38 casos instrumentados distintos** aprobados; matriz360/420dp y fuentes1,3/1,5.13 capturas inspeccionadas en `docs/screenshots/v323-final/README.md`.21 módulos acústicos/datos/gestos/cámara idénticos a3.2.2; sin migración/reanálisis masivo/cambio de caché. Nunca instrumentar/clear/uninstall/restaurar Samsung. PCM cercana prueba ambigüedad, no nota real; amplitud por lectura no persistida. Pruebas físicas de gesto/afinador/audio/TalkBack/15min pendientes.

## Graph Intelligence & UX Stability 3.2.2 — 21/10/2026

- APK **3.2.2-beta1 / 8**, paquete y firma conservados; Samsung `R5CT21KTQ8W` install-r/Success. Respaldo fresco de **60 archivos / 12 WAV**; datos de usuario con hashes intactos. Informe `checkpoints/v322-samsung-update.json`. Nunca instrumentar/clear/uninstall/restaurar el Samsung.
- Fundamental: General robusta por `PitchTrace` + percentiles2,5–97,5% para ≥40 confiables, padding y mínimo contexto ±3 semitonos. Pinch limitado a ese dominio; rango amplio solo por Mostrar todos/rango exacto. Reset desactiva rango extendido y vuelve al auto-fit. No auto-fit por gesto ni por desplazamiento temporal.
- `preview(id)` y `explorer(id)` tienen cámaras independientes por gráfico y contexto; selección/fragmento/bucle compartidos por sesión. Al abandonar de verdad una sesión se eliminan ambos estados transitorios. Rotación/detalle↔fullscreen conservan el propietario activo; tarjeta pura, sin cámara mutable.
- `PitchTrace` clasifica sin reescribir: confianza insuficiente, fuera de perfil y salto aislado de periodicidad. Este último exige retorno en ≤600ms, vecinos plausibles en ≤400ms por lado y ≤80cents entre sí, múltiplo2/3/4 dentro de80cents y confianza central<0,92. Es incertidumbre, no prueba de error ni corrección de octava. No une esos puntos; Mostrar lecturas inciertas/Mostrar todos y cursor conservan acceso a Hz/confianza original sin confirmar nota.
- `Music` limita equivalencia a MIDI0–127 (bordes de medio semitono), sin log de cero; `PitchAxis` considera únicamente notas de ese rango. 0Hz/valores inválidos no tienen nota. Fundamental mantiene Hz/nota secundaria; STFT solo Hz.
- **86 JVM / 35 casos instrumentados distintos**, más focos 360/420 dp y fuentes1,3/1,5. Capturas revisadas en `docs/screenshots/v322-final/README.md`. Motores YIN/PCM/STFT/reproducción/persistencia y cachés idénticos a3.2.1; no migración ni reanálisis masivo. Checkpoint recuperable en `checkpoints/latest.txt`.
- Las ventanas PCM vecinas reproducen mínimos de periodicidad en≈43 y≈130Hz: ambigüedad del estimador en señal cambiante, no artefacto de decimación. Tiempos guardados son de entrega, no posiciones exactas de ventana; no atribuir todos los saltos al detector ni al instrumento. Prueba física de gesto/afinador/audio/TalkBack/15min pendiente.

## Stability & Precision 3.2.1 — 21/10/2026

- APK **3.2.1-beta1 / 7**, mismo paquete/firma, Samsung `R5CT21KTQ8W` actualizado por install-r/Success. Respaldo fresco `samsung-before-3.2.1-20261021-112011`: **48 archivos / 10 WAV**; índice, ajustes, relaciones y WAV con hashes intactos. Único cambio: marcador runtime `files/profileInstalled`, esquema/fecha de actualización comprobados; informe `checkpoints/v321-samsung-update.json`. No instrumentación/clear/uninstall/restauración en Samsung.
- Vista amplia es destino `measure/wide` encima de Medir; NavController maneja Atrás, sin BackHandler. El ViewModel reconoce ambas presentaciones como la misma captura: no detener/reiniciar/guardar al alternarlas. Se mantiene la política de detener/guardar al salir realmente de Medir o del primer plano.
- Pinch radial continuo (`calculateZoom`), ancla actual menos pan previo, límites en coordenadas de datos; márgenes de Fundamental estables durante zoom. STFT con ventanas ancladas al WAV, mezcla por soporte temporal; bitmap con transformaciones flotantes. Render después de soltar, sin espera artificial de 180 ms.
- Fundamental usa lecturas capturadas como serie canónica si existen, incluso tras Analizar. `PitchTrace` y `PlotScales.overview` compartidos por tarjetas/visor; general con contexto y todos los válidos, ajustada robusta en herramientas. Baja confianza: puntos aislados; silencios interrumpen. No suavizar ni borrar anomalías reales. Espectrograma solo Hz.
- VERSION de análisis sigue 2; **VISUAL_VERSION=3** invalida solo teselas regenerables de forma perezosa. YIN/captura/WAV/reproducción/simulación/persistencia sin cambios respecto de 3.2 (11 módulos comparados byte a byte). Barras/márgenes de STFT de 3.2 conservados; lectura íntegra en herramientas con indicador i.
- **76 JVM**, **32 casos instrumentados distintos** aprobados solo en emulador: 5 nuevos + 25 regresión + 2 rotación; focos 360/420 dp y fuentes 1,3/1,5. No contar ejecuciones fallidas/interrumpidas. Git ahora disponible, cambios previos preservados en `checkpoints/before-3.2.1`; cierre recuperable en `checkpoints/latest.txt`.
- Capturas y límites: `docs/screenshots/v321-final/README.md`, VALIDATION. No se validó pinch con dedos físicos, exactitud contra afinador, sincronía audible ni batería/15 minutos/TalkBack completo.

## Cierre Acoustic Studio 3.2 — 21/10/2026 (prioridad sobre 3.1)

- APK **3.2.0-beta1 / 6**, mismo paquete y certificado. Samsung `R5CT21KTQ8W`: install -r **Success**, arranque correcto, **8 WAV** e índice/ajustes/relaciones con hashes intactos frente al respaldo fresco de 27 archivos indicado en `backups/latest.txt`. Solo metadatos runtime comprobados y cachés derivadas; ver `checkpoints/v32-samsung-update.json`. Nunca instrumentar/desinstalar/limpiar/restaurar el Samsung.
- Visor compartido: barras 48 dp; horizontal bajo o fuente ≥1,3 reúne transporte arriba; herramientas en panel. Un propietario de safeDrawing, barra de estado oculta/restaurada; navegación/cutout conservados. Fundamental musical, STFT exclusivamente Hz. Leer `docs/IMMERSIVE_VIEWER.md` antes de tocar gestos/insets/ciclo de vida.
- Verificación física de área de datos del espectrograma: **306 / 1080 px (28.3%) → 773 / 1080 px (71.6%)**, antes/después sobre la misma sesión. Capturas Samsung y emulador revisadas; índice `docs/screenshots/v32-final/README.md`.
- Perfil `DIDGERIDOO` / `NATIVE_AMERICAN_FLUTE` persistido por sesión; campo ausente/desconocido → Didgeridoo, sin reescritura al leer. Detector YIN compartido: didge conserva /4 y 16384 muestras; flauta nominal 200–1600 Hz a tasa completa, 8192 muestras, actualización ~93 ms y filtro de confianza. Cache VERSION=2, clave incluye instrumento. Laboratorio exclusivamente didgeridoo.
- **67 JVM** y **27 casos instrumentados distintos** aprobados solo en emulador (25 suite + 2 rotación). Matriz 360/420 dp y fuentes 1,0/1,3/1,5. Fallos/ejecuciones interrumpidas no cuentan como aprobados.
- Prueba física de flauta, precisión contra afinador, sincronía audible, 15 minutos/batería y TalkBack completo siguen pendientes. No atribuir precisión física a las señales sintéticas. No hay Git; checkpoint final en `checkpoints/latest.txt`.

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
