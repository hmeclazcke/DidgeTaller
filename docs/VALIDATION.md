# Registro de validación

## Adopción de Git y punto de partida — 21/10/2026

Solicitud: conservar el estado acumulado y establecer commits verificables por fase. Sin cambios al código de la app ni a su versión `3.2.4-beta1/10`.

- Identidad configurada y efectiva de autor/committer comprobada antes del primer commit: Hernán A. Meclazcke, mediante `git config --get user.name`, `git config --get user.email` y `git var GIT_AUTHOR_IDENT` / `GIT_COMMITTER_IDENT`. No se modificó la configuración global ni se sustituyó el autor.
- Estado inicial: un único commit `4f90893` (`Primer commit`), 28 archivos modificados y 115 nuevos del proyecto. Revisión de rutas, diffs, archivos nuevos, índices de capturas y jerarquías XML de UI; sin audio, credenciales ni archivos generados en la selección. Los atributos `password="false"` de las jerarquías Android no son contraseñas guardadas. Las capturas conservadas son evidencia de UI del proyecto; sus resultados históricos no se vuelven a contar como pruebas actuales.
- Entorno comprobado: Windows, JBR **21.0.11**, Platform 35 y Build Tools 35.0.0 disponibles; bytecode objetivo 17. `gradlew.bat :app:testDebugUnitTest --console=plain`: **BUILD SUCCESSFUL in 51s**, 104 pruebas en 11 suites, cero fallos, errores u omitidos. `gradlew.bat :app:assembleDebug --console=plain`: **BUILD SUCCESSFUL in 4s**. Los XML permanecen en `app/build/test-results/testDebugUnitTest/`, excluidos de Git.
- Commit de partida **`3f0510f`**, `chore(project): checkpoint existing 3.2.4-beta1 state`: 143 archivos preparados por rutas revisadas; `git diff --cached --check` aprobado, sin rutas privadas/generadas. Preserva los cambios acumulados, sin reconstruir commits funcionales retrospectivos ni modificar `4f90893`.
- Segunda fase: reglas permanentes en `AGENTS.md` y exclusiones adicionales de audio privado, credenciales, fixtures locales y cachés Kotlin/nativas en `.gitignore`. La compilación creó transitoriamente `.kotlin/sessions/*.salive`: la selección de rutas lo detectó y excluyó antes de preparar archivos. No se borró ese caché ni se incluyó en el commit de partida.
- Verificación de exclusiones con `git check-ignore`: 34 rutas representativas privadas/generadas correctamente ignoradas y seis controles de fuente, tests, documentación, capturas y wrapper no ignorados. También se comprobó `.WAV` en mayúsculas en este repositorio Windows (`core.ignorecase=true`). Sin crear archivos de prueba. `git diff --check` aprobado; ninguna ruta privada/generada rastreada según la revisión de nombres. `git diff --exit-code 3f0510f -- app` confirma que la segunda fase no modifica la app; no requiere repetir las pruebas JVM ni la compilación.
- Alcance: esta iteración verifica JVM y compilación; no ejecuta instrumentación, instalaciones ni operaciones en Samsung. Las advertencias existentes de `jvmTarget` y `ArrowForward` obsoletos no impidieron las tareas. Validación física/acústica pendiente según los registros siguientes.

Recuperación del código verificado: `3f0510f` (`3.2.4-beta1/10`); el commit posterior de política Git conserva el mismo código e incorpora las reglas permanentes. Consultar `git log --oneline` para identificar el cierre. Los respaldos y audios privados siguen fuera del repositorio.

## Reparación selectiva de Fundamental 3.2.4 — 21/10/2026

APK **3.2.4-beta1/10**,11277304 bytes, SHA-256 `6fe6bcdcfb976acd5124e1f91b43fabddefb41a534677fd6f6ec8c8611c1c1a6`. Mismo paquete/firma; JDK17.0.20, bytecode17/SDK35, sin dependencias/permisos nuevos. Checkpoint previo `checkpoints/before-3.2.4`; cierre `checkpoints/latest.txt`.

### Causa y reversión parcial

La regresión3.2.3 se reproduce con las dos series reales reportadas. El umbral visual global0,85 descartaba lecturas útiles de periodicidad moderada y cada incertidumbre borraba el enlace anterior. No era decimación: F0 no se reduce en tarjetas. Comparación contra fuente/APK3.2.2 y3.2.3, preservados; no rollback global. Se recupera aceptación0,70 y se reemplaza la segmentación dañina en `PitchTrace` común a tarjeta/detalle/fullscreen. Datos originales intactos.

La curva muestra tendencia, no certificación de nota tocada. Ante hasta4 omisiones y≤1000ms, enlaza **dos extremos medidos** si coinciden dentro de80cents; no inserta muestras ni suaviza. Una frecuencia nula solo admite ese enlace si su confianza guardada está en[0,50;0,70); null con confianza0/alta/desconocida, valores inválidos y pausas largas cortan. Sin omisiones, separación máxima1200ms. El dato incierto conserva Hz/tiempo/confianza/razón y acceso en herramientas/cursor.

Solo en didgeridoo: una excursión única≥100cents entre vecinos aceptados a≤400ms, coherentes≤80cents, se omite de la curva principal. Se conserva criterio previo de múltiplos2/3/4. Para breves períodos alternativos bajos: modo dominante por semitono, banda±160cents con≥6 lecturas y≥55% de aceptadas; candidato<mediana/1,4, confianza[0,65;0,90), razón1,5/2/3/4±80cents; grupo≤4 lecturas/≤800ms con separación≤400ms. Bordes junto a ausencia: confianza<0,90, dos vecinos aceptados coherentes, diferencia≥100cents y caída de confianza≥0,04 frente al más fuerte. Son heurísticas de presentación, no correcciones del detector ni prueba de error. Cambios respaldados por varias lecturas y flauta conservados; un evento real de una única lectura puede quedar entre originales.

General conserva algoritmo previo, incluyendo todos los puntos principales y contexto mínimo±3semitonos: no nuevo recorte por percentiles. Inciertos aislados no inflan rango. Ajustada, Mostrar todos/rango exacto, Reset, límites de zoom y cámaras independientes intactos. «N fuera» ya no aparece en título/lectura/leyenda principal; sigue consultable en herramientas.

### Casos reales y comparación visual

Fixtures privados extraídos **en solo lectura del respaldo existente**: Re·D2,20oct17:06,17320ms,72,077Hz,91 lecturas; Mib·Eb2,20oct21:20,7720ms,75,880Hz,40 lecturas. Se conservaron exactamente serie/fechas/duración/resumen, con IDs nuevos y WAV silenciosos de igual duración **solo en almacenamiento de prueba del emulador**. No se copiaron audios reales al repositorio ni se reanalizaron/sobrescribieron WAV del Samsung. Es validación del renderizado de sus lecturas guardadas, no reproducción acústica real.

Mismo emulador API37,1080×2400,densidad411≈420dp,fuente1,0; ejecuciones con APK3.2.2/8,3.2.3/9 y3.2.4/10. Los APK históricos se instalaron exclusivamente en emulador. Conteos de puntos principales/enlaces:

| Sesión |3.2.2 previa|3.2.3 regresiva|3.2.4 reparada|General reparada|
|---|---:|---:|---:|---|
|Re·D2|43/35|17/15|40/33|60,58–85,68Hz|
|Mib·Eb2|26/23|12/5|20/19|63,77–90,18Hz|

Re·D2 recupera la tendencia y conserva huecos reales; no se afirma línea ininterrumpida durante toda la grabación. Mib·Eb2 recupera un tramo principal continuo cerca de75Hz: antes su General llegaba a44,56Hz y quedaban trazos mínimos;3.2.2 incluía períodos bajos hasta≈35Hz. Inicio sin tendencia respaldada permanece vacío. Originales no borrados. Métricas comparativas `checkpoints/v324-real-comparison.json`; fixture privado ignorado `v324-real-fixtures.json`.

Eje medido con Paint: números sin sufijo repetido, unidad Hz única y notas secundarias; margen4+4dp y congelación durante gesto conservados. Detalle916px: reserva115,55→74,55px(−35,48%), ancho de datos769,625→810,625px(+5,33%). Leyenda «Línea: tendencia» y ayuda en herramientas; se quita el párrafo permanente de gestos bajo Fundamental. Otros gráficos conservan su presentación.

### Validación y límites

Gate `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest`: **BUILD SUCCESSFUL,104 JVM**, cero fallos/errores/omitidos; logs `v324-focus-build.log`/`v324-final-build.log`. Siete casos nuevos: drones moderados, outliers bajos/altos, puentes limitados, silencio real, toot respaldado, flauta, períodos alternativos breves frente a sostenidos, ruido prolongado y1000 puntos sin decimación. Expectativas antiguas de segmentación0,85 actualizadas porque expresaban la conducta rechazada. Primera ejecución detectó además un borde que descartaba el retorno legítimo del toot: corregido exigiendo degradación real de confianza; no se cuenta esa ejecución fallida como aprobada.

**39 casos instrumentados distintos**, exclusivamente `emulator-5554`:1 comparación real nueva(`v324-after`),36 regresión y2 recreación/rotación real de MainActivity. Además6 repeticiones:3 casos en360dp/fuente1,3 y3 en420dp/fuente1,5. Todos sin fallos/omitidos, mismo SHA del APK final en cada `v324-*-run.json`. Regresión incluye Back/Vista amplia con captura, cámaras independientes, zoom/pan/ancla/reset, originales, cero sin nota, reproducción/selección, flauta/perfiles, WAV/índice/relaciones/persistencia. No se cuenta dos veces un caso repetido por configuración.

Comparaciones baseline:1 caso contra3.2.3 y1 contra3.2.2, aprobados separadamente, excluidos del conteo39 del APK final. Arranque prematuro del baseline antes de terminar de compilar el APK de prueba falló por clase no disponible; repetición limpia aprobada, fallo conservado aparte. El curador auxiliar rechazó inicialmente el texto singular «1 test»; corregido el parser, sin repetir suites aprobadas ni modificar app/APK. Solo cuatro archivos main cambian frente al checkpoint3.2.3 (`PitchTrace`, `PlotSurface`, `SessionExplorer`, `ImmersiveViewer`),39 main idénticos; informe `v324-source-evidence.json`. Sin cambios DSP/PCM/STFT/YIN/reproducción/captura/persistencia/cachés/cámaras/gestos/navegación/permisos/dependencias.

[20 capturas revisadas](screenshots/v324-final/README.md): comparación de ambas tarjetas/detalles en tres versiones, fullscreen reparado, fuentes grandes, leyenda, toot/flauta, Reset,0Hz y espectrograma horizontal. Matriz sin superposición de ejes ni mutilación de las miniaturas. Re·D2 conserva pausas y algunos cambios respaldados: no se maquillaron como tono perfectamente plano. STFT conserva contenedor compacto; no se vuelve a atribuir una medida física71,6% a esta entrega sin medirla allí.

### Samsung: actualización no destructiva

Serial **R5CT21KTQ8W**, SM-G990E. Micrófono inactivo verificado antes de respaldar y justo antes de instalar. Respaldo fresco `backups/samsung-before-3.2.4-20261021-152023`: **61 archivos/13 WAV**, hashes contrastados antes de actualizar. Certificado del APK nuevo e instalado previo coincide: `fbf54fbe12161bdbf5adbfb9d906961511e9b49a715be8eac8f886e01292b7b2`. Paquete `ar.didgetaller`, código9→10.

`adb -s R5CT21KTQ8W install -r …` → **Success**. `am start -W` → **Status: ok**, proceso18346, sin fatal en su buffer de crash. Bytes del APK instalado iguales al compilado. Verificación a15:20:53: **61/61 hashes intactos**, sin nuevos archivos derivados/runtime; índice completo idéntico,13 sesiones y13 WAV indexados/legibles. Incluye ajustes/asociaciones/mediciones; no migración, reanálisis masivo ni escritura de datos originales. Informe/script/log privados `checkpoints/v324-samsung-update.json`, `update_samsung324.py`, `v324-samsung-update.log`. No instrumentación, clear, uninstall, restauración, captura ni reproducción de audio real. Captura actual del teléfono negra: no permite inspección visual física; la revisión de gráficos reales se realizó con sus lecturas en emulador.

Límites: confianza no calibrada, clasificación contextual no infalible; eventos brevísimos pueden quedar inciertos y siguen inspeccionables. Amplitud por lectura no almacenada; tiempos históricos corresponden a callback, no ventana PCM exacta. No se atribuye cada punto omitido a error físico confirmado. Gestos con dedos físicos, afinador independiente, sincronía audible,15min/batería y TalkBack completo pendientes.

## Fundamental Precision 3.2.3 — 21/10/2026

APK **3.2.3-beta1 / 9**, mismo `ar.didgetaller`, 11275867 bytes, SHA-256 `319f1223326e60ea34f0c357a39639c1725b2304f1260627933aa1df05ba5787`. JDK21.0.11/bytecode17/SDK35; dependencias/permisos intactos. Línea base86 JVM+APK aprobada antes de modificar. Gate `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest`: **BUILD SUCCESSFUL**, **97 JVM**, cero fallos/errores/omitidos (`checkpoints/v323-final-build.log`). Once casos nuevos; expectativa anterior actualizada para incluir eventos confiables raros en General.

### Causas y política común

Los valores problemáticos ya están en el índice. No existe decimación de F0 en las tarjetas: tarjeta y visor consumen `PitchTrace` íntegro y sus enlaces `previous`. 3.2.2 aceptaba confianza≥0,70 y solo reconocía retornos a períodos múltiples; conectaba estimaciones débiles no armónicas y bordes junto a ausencias. Pintarlas naranja no evitaba dibujar las caídas.

Ahora la curva exige confianza YIN≥0,85 y validez del perfil, sin promover lecturas por repetirse. Se mantiene el criterio de periodicidad aislada de3.2.2. Retornos no armónicos: una sola lectura con confianza<0,92, dos vecinos por lado a≤400ms entre muestras, vecinos≥0,90 y dispersión≤80cents, separación central≥100cents y caída de confianza≥0,04 respecto de todos ellos. Bordes: lectura<0,92 junto a ausencia/contexto sin apoyo, dos vecinos del otro lado a≤400ms entre muestras, ambos≥0,85 y alguno≥0,90, coherentes≤80cents, separación≥100cents y caída≥0,04 frente al más confiable. Son reglas de incertidumbre de presentación; confianza no es probabilidad calibrada. No confirman error ni nota tocada.

La incertidumbre corta antes/después. Normal usa verde; originales agrega círculos pequeños sin unirlos. Cursor: Hz/confianza/razón sin nota confirmada. Ausencias no generan0Hz. No se interpolan ni suavizan mediciones. Transiciones respaldadas permanecen confiables, sin duplicar modelos. Amplitud y curva CMNDF no se persistían por lectura: no se inventaron metadatos.

General usa percentiles2,5–97,5% desde40 confiables y margen previo, conserva contexto mínimo±3semitonos y extiende a **todos** los confiables con margen `max(0,6Hz;8% de amplitud total)`. Un toot de dos lecturas no desaparece por peso estadístico. Ajustada conserva percentiles/precisión fina; zoom admite0,2Hz. Mostrar todos incluye inciertas; rango exacto habilita dominio amplio. Reset vuelve a tiempo completo+General y desactiva dominio extendido. Sin auto-fit durante gesto/pan. Aviso discreto de lecturas fuera del viewport actual, incluidas inciertas; originales y ayuda en herramientas.

### Diagnóstico real, solo lectura

Respaldo diagnóstico fresco: **61 archivos/13 WAV**, hashes contrastados sin escribir al Samsung. Informes privados ignorados `v323-real-context.json`, `v323-real-isolated.json`, `v323-real-pitch-diagnostic.json`; script `diagnose323.py`. Seis eventos inspeccionados; ejemplos:

| Tiempo / Hz / confianza guardados | Contexto original | PCM cercano: RMS / periodicidad |
|---|---|---|
|4,880s /66,846 /0,787|71,661→66,846→ausencia|0,07951 /0,560: energía suficiente, periodicidad débil|
|6,376s /77,733 /0,797|72,360→77,733→ausencia|0,01872 /0,490; mínimo≈45,37Hz: ambiguo|
|8,042s /35,235 /0,785|ausencia→35,235→65,557|0,10347 /0,713; mínimo≈66,02Hz: no confirma caída real|
|9,717s /65,419 /0,905|71,973→65,419→ausencia|0,00285, menor que umbral RMS0,005: no aceptar mínimo como detección|

Los tiempos capturados son de entrega de callback. Ventanas16384 cercanas, promedio/4 y CMNDF local no reconstruyen exactamente la ventana original. El script informa lag entero sin interpolación ni aceptación RMS. No atribuir cada evento al detector o instrumento. Ambigüedades43/130Hz documentadas debajo. JVM reproduce también ataque91,115Hz/conf0,861 y final65,419Hz/conf0,905 como inciertos junto a ausencias.

### Eje e interfaz

Mismo fixture, emulador1080×2400, densidad411 (≈420dp), fuente1,0; `PlotDataArea` gobierna dibujo/gesto/inspección. Antes: etiqueta ficticia `000,0000 Hz` y mínimo56dp. Después: cada etiqueta real Hz/nota medida con el mismo Paint del Canvas, nota85%, margen exterior4dp+separación4dp. Ancho retenido durante gesto, recalculado al soltar. Se elimina Hz superior redundante. Otros gráficos conservan márgenes.

| Superficie / ancho total | Reserva anterior→actual | Datos anterior→actual | Ganancia de datos |
|---|---:|---:|---:|
|Detalle /916px|189,825→115,550px|695,350→769,625px|74,275px /10,68%|
|Fullscreen /1080px|189,825→115,550px|859,350→933,625px|74,275px /8,64%|

Reserva−39,13%; detalle recupera8,11 puntos porcentuales del ancho total. Medidas de este escenario, no constantes para todas las frecuencias/fuentes. Leyenda «Verde: confiable · huecos: sin lectura segura»; aviso «N fuera · ver Escalas y rangos»; explicación técnica en panel existente.

### Pruebas y cierre

- JVM nuevos: drone69–72 con siete tipos de pico; retorno no armónico; toot corto/sostenido incluso2/202 lecturas; salida real de YIN sobre tonos sintéticos205/207Hz con RMS/confianza; melodía/glissando; incertidumbre prolongada; silencios/tiempos discontinuos; ejemplos reales; serie larga de tarjeta; Ajustada/Reset/vacíos/extremos/perfiles y notas/ticks.
- Nuevos UI **3/3** (`v323-fundamental-instrumentation.log`): tarjeta/detalle/fullscreen, Ajustada/pinch/Reset/originales/cursor, toot breve y flauta. Primer recorrido falló por buscar un instante fuera del zoom temporal: corregido con Reset antes de inspeccionar. Dos expectativas iniciales JVM erróneas: índice tras ausencia omitida y nombre corto frente a bilingüe. Ejecuciones fallidas preservadas y excluidas del conteo.
- Regresión final **33/33** (`v323-regression-instrumentation.log`) + rotación real **2/2** (`v323-rotation-instrumentation.log`): **38 casos instrumentados distintos** contando los3 nuevos. Matriz360dp/fuente1,3 y420dp/fuente1,5: **4/4 cada una**, repeticiones de los3 nuevos y visor horizontal; no sumarlas como casos nuevos. Cada JSON `v323-*-run.json` registra el SHA del mismo APK final. Toda instrumentación dirigida explícitamente a `emulator-5554`, con guardas de hardware.
- Una corrida agrupando las35 regresiones quedó esperando durante ViewerRotation después de las otras clases; se interrumpió solo en emulador y se descartó completa. Se ejecutaron limpias33+2 por separado, como en validaciones anteriores. No se presenta la corrida parcial como aprobada.
- [13 capturas auténticas revisadas](screenshots/v323-final/README.md): antes/después, tarjeta/detalle, General/Ajustada/Reset, originales/cursor, toot/flauta, fuentes1,3/1,5 y horizontal. Sin V espurias en fixture normal; originales sin conectar; toot/melodía visibles; eje y notas sin recorte/superposición. Leyenda ocupa una línea a fuente1,0 y dos en los focos grandes. Sin prometer una única línea con cualquier fuente.
- **21 módulos** DSP/PCM/WAV/STFT/reproducción/persistencia/cachés/gestos/cámara comparados byte a byte con `checkpoints/before-3.2.3`: idénticos. Hashes en `v323-final-evidence.json`. Sin migración ni cambios de versiones de caché.

Samsung **R5CT21KTQ8W / SM-G990E**: respaldo fresco `samsung-before-3.2.3-20261021-144220`, **61 archivos/13 WAV**, tar15417344bytes. Certificado compatible `fbf54fbe12161bdbf5adbfb9d906961511e9b49a715be8eac8f886e01292b7b2`, versión8→9, mismo paquete. Guardas de micrófono inactivo/ausencia de `.part` y hashes estables antes de instalar. `adb -s R5CT21KTQ8W install -r …` → **Success**; arranque **Status:ok**, PID7123, buffer crash vacío. SHA del APK instalado coincide con el local. Comprobación a **2026-10-21T14:42:50**: **61/61 hashes idénticos**, ningún nuevo derivado ni diferencia runtime en ese momento; índice completo igual, **13 sesiones/13 WAV indexados legibles**. No extrapolar estos hashes a cambios posteriores del usuario. Informe `checkpoints/v323-samsung-update.json`; respaldo señalado por `backups/latest.txt`. Sin instrumentación, clear, uninstall, restauración, captura o reproducción automática en el teléfono. Accesibilidad de archivos/índice comprobada de forma no destructiva; no se atribuye recorrido visual o prueba musical física.

Checkpoint final recuperable `checkpoints/after-3.2.3`, indicado por `checkpoints/latest.txt`: fuente/documentos de esta iteración, APK final y manifiesto. Cambios anteriores preservados; no se creó un commit que los mezclara. Capturas auxiliares/ejecuciones descartadas archivadas en checkpoints, únicamente13 imágenes útiles en docs.

Fixtures visuales equivalentes al escenario reportado, no grabaciones del Samsung ni ejecuciones acústicas reales. PCM real analizada separadamente en privado. Límites: amplitud no persistida, tiempo de callback, posible incertidumbre en notas brevísimas imperfectas, precisión física/calibración y pruebas manuales de gesto/afinador/sincronía/15min/TalkBack pendientes.

## Graph Intelligence & UX Stability 3.2.2 — 21/10/2026

**APK3.2.2-beta1 / 8**, `ar.didgetaller`, 11257318 bytes, SHA-256 `caa33120c435943b2f441e652322393fb6f904421371c9d04c837663b720916d`. `testDebugUnitTest assembleDebug assembleDebugAndroidTest`: **BUILD SUCCESSFUL** (`checkpoints/v322-final-build.log`), **86 JVM**, cero fallos/errores/omitidos. Se verificaron los76 resultados existentes antes de modificar; se añadieron10 casos matemáticos/modelo. Stack/permisos intactos.

| Ejecución en emulator-5554 API37 | Resultado | Log checkpoints/ |
|---|---:|---|
| GraphIntelligenceUiTest | 3/3 | v322-intelligence-instrumentation.log |
| PrecisionUiTest | 5/5 | v322-precision-instrumentation.log |
| StudioViewer + SessionNavigation + LocalPersistence | 25/25 | v322-regression-instrumentation.log |
| ViewerRotation | 2/2 | v322-rotation-instrumentation.log |
| 360dp / fuente1,3: Intelligence + visor horizontal | 4/4 | v322-360-large-instrumentation.log |
| 420dp / fuente1,5: Intelligence + visor horizontal | 4/4 | v322-420-large-instrumentation.log |

**35 casos distintos**=3+5+25+2. Los ocho recorridos de matriz son repeticiones, no casos nuevos. JSON por ejecución contiene SHA del mismo APK final. Instrumentación solo emulador, con guardas de hardware; los tres nuevos casos crean/limpian exclusivamente preferencias `intelligence-tests-*` y directorio `intelligence-tests`. Ninguna prueba instrumental en Samsung.

### Causas y cambios comprobados

1. **Autoescala/zoom-out:** el dominio de Fundamental era literalmente `0..max(maxHz,500,full.high)`. General incluía todos los puntos aceptados por confianza/rango, incluso retornos aislados a períodos múltiples. Ahora General usa lecturas clasificadas confiables, percentiles2,5–97,5 para≥40 puntos, margen `max(0,6Hz;1,2% mediana;18% dispersión)` y contexto mínimo de tres semitonos por lado. El dominio interactivo es ese rango, no el rango físico global. Para70±0,15Hz:≈58,9–83,3Hz; Ajustada≈69,01–70,99Hz. Mediana/rango se memoizan por serie, no se recalculan por gesto. El pan X no altera Y. Reset restaura ambos ejes y desactiva rango extendido; no cambia selección/audio/mediciones. Valores fuera de la escala robusta se cuentan y siguen accesibles en Mostrar todos o rango exacto.
2. **Propiedad del estado:** `vm.explorer(id)` persistía en el mapa del ViewModel al regresar a Biblioteca y lo compartían detalle/fullscreen. La tarjeta ya calculaba su rango sin ese mapa; el defecto comprobado estaba en el preview integrado y la nueva visita. Ahora preview y fullscreen tienen objetos/cámaras separados por UUID/tipo de gráfico; comparten solo selección, fragmento y bucle. La salida real elimina ambos; la rotación no los elimina. UI comprueba igualdad de viewport de tarjeta antes/después, preview intacto al salir del fullscreen, nuevo estado al reabrir A y perfil/rango propio de B(flauta), además de cambio a STFT. Se evita también clamping de Y de miniaturas que pegaba lecturas recortadas al borde: se recorta el Canvas en coordenadas reales.
3. **Picos:** el índice ya contiene las caídas, antes de cualquier renderizado; no hay decimación de Fundamental que invente puntos. `PitchTrace` antes solo filtraba rango/confianza≥0,70, insuficiente para resolver ambigüedad de periodicidad. Se conserva esa política básica y se añade una clasificación contextual conservadora: un único punto con confianza<0,92, retorno a vecinos plausibles(conf≥0,65), ≤400ms por lado/≤600ms total, vecinos separados≤80cents y razón2/3/4 dentro de80cents → estimación incierta, nunca Hz corregidos ni error acústico declarado. Se interrumpe antes/después; el original queda como círculo opcional y cursor con razón, Hz y confianza YIN, sin nota/cents confirmados. Notas sostenidas, transiciones70→75→82→90, melodía392→440→523→440 y octavas limpias de alta confianza permanecen sin suavizado. Una articulación rapidísima de octava con periodicidad imperfecta puede quedar incierta: el dato no desaparece ni se modifica.
4. **0Hz/C#-14:** `PitchAxis` empezaba en `low.coerceAtLeast(.001)` y generaba MIDI negativos extremos; el texto redondeaba esas frecuencias positivas a0Hz. `Music.pitch` rechazaba0 exacto pero aceptaba cualquier positivo. Ahora Music admite solo MIDI0–127 con sus bordes de medio semitono (aprox7,94–12911Hz), y PitchAxis enumera únicamente0..127. Numeric ticks fuera de ese rango no llevan nota ni«≈». Pruebas de0/negativos/NaN/infinito/subnormales/0,001/1Hz y notas C2,C#2,C3,C4,A4,A5. UI solicita rango exacto0–1 y verifica0Hz sin nombre musical; luego Reset vuelve al auto-fit. También se corrigió semántica accesible que anunciaba«—Hz aproximadamente» para0.

### Audio real en modo solo lectura

Se leyeron los WAV del respaldo fresco previo a3.2.2, generado mientras estaba instalada3.2.1, sin copiarlos al repo público ni escribir al teléfono. Script local `checkpoints/diagnose322.py`, informe privado ignorado `v322-real-pitch-diagnostic.json`: cuatro eventos de didgeridoo, ventanas PCM16384 cercanas con el mismo promedio por4, win2200, diferencia/CMNDF y umbral0,14 del detector. Se informa frecuencia del lag entero, sin pretender interpolación ni alineación exacta con la captura.

| Tiempo original / Hz / confianza | Vecinos | PCM cercano, final de ventana | Resultado |
|---|---|---|---|
| 2,653s /42,903 /0,8762 | 2,475s126,857 →2,831s128,718 | 2,553s: RMS0,0765,≈43,07Hz/conf0,897; 2,653s: RMS0,0793,≈129,71Hz/conf0,896 | Mínimos a períodos relacionados y dependencia de la ventana; incierta por retorno aislado |
| 6,166s /44,776 /0,7447 |136,246 →138,035(conf0,698) | 5,966s≈44,82Hz; 6,166s≈137,81Hz | Vecino marginal y periodicidad ambigua; incierta |
| 6,927s /43,336 /0,7686 |132,604 →128,888 |6,827s≈43,41Hz;6,927s≈129,71Hz, RMS0,0446 | Retorno aislado; incierta |
| 7,870s /36,732 /0,7961(otra sesión) |141,346 →74,343 |7,770s≈36,87Hz;7,870s≈74,49Hz | Vecinos no coinciden: no se fuerza la clasificación de retorno |

Los timestamps guardados son del callback/tiempo transcurrido, no posiciones de muestra ni timestamp de inicio de ventana. La PCM cercana reproduce el comportamiento del estimador pero no prueba qué nota tocó la persona. No se modificó YIN por una presunción de ground truth. Silencio, incertidumbre de confianza, periodicidades ambiguas y cambios confiables conservan tratamientos distintos. Originales y cachés previas no se reescriben.

### UI, integridad y límites

[Capturas reales revisadas](screenshots/v322-final/README.md): General/Ajustada/Reset, tarjeta antes/después, preview y reapertura, flauta, incierta y cursor,0Hz, horizontal y fuentes grandes. Datos STFT sin ejes: **66,4% de altura útil en420dp/fuente1,0;71,2% en360dp/1,3;73,8% en420dp/1,5**, según logs `v322-*-layout-metrics.log`. Se conservan los48dp/insets/STFT de3.2; no se atribuye una nueva medición física del71,6%. Los gestos matemáticos y multitáctiles inyectados no equivalen a una prueba con dedos en Samsung.

Samsung `R5CT21KTQ8W`: certificado `fbf54fbe12161bdbf5adbfb9d906961511e9b49a715be8eac8f886e01292b7b2` compatible, install-r **Success**, arranque **Status:ok** y buffer crash sin fatal. Respaldo fresco `samsung-before-3.2.2-20261021-140322`: **60 archivos/12 WAV**, **59 archivos idénticos**. Diferencias runtime comprobadas: `{"shared_prefs/android.app.ActivityThread.IDS.xml": {"valid_xml": true, "entries": 1}}`; nuevos derivados: `[]`. WAV, índice, ajustes, relaciones y perfiles intactos en la comprobación de 2026-10-21T14:03:41. No clear/uninstall/instrumentación/reproducción/captura automática. Informe `v322-samsung-update.json`. Después, un segundo intento de comprobar volvió a detectar RECORD_AUDIO activo y se abortó antes de leer hashes o instalar; no se interfirió con esa captura. No extender los hashes de la primera comprobación a cambios posteriores hechos por el usuario. El teléfono estaba bloqueado durante la preparación: UI física de3.2.2 no comprobada.

14 módulos comparados byte a byte con antes de3.2.2: YIN, audio PCM/WAV, STFT, reproducción, simulación, persistencia, cachés y gestos anteriores idénticos; lista `v322-final-evidence.json`. No cambio de formatos ni versiones de caché, porque cambia exclusivamente presentación/estado/notación.

Fallos de desarrollo no contados: primera corrida nueva detectó selección de nodo fusionado en tarjeta (test pasó a árbol no fusionado) y semántica de0Hz; se corrigieron y repitieron los tres casos limpios. No se presenta una corrida parcial como regresión aprobada. Persisten límites previos: alineación de captura por callback, precisión/calibración física del micrófono, clasificación contextual no infalible, STFT743ms, actualización de regiones STFT al soltar, MediaPlayer no sample-accurate. Pendientes gesto físico, afinador, sincronía audible,15min/batería y TalkBack completo.

## Stability & Precision 3.2.1 — 21/10/2026

**APK:** 3.2.1-beta1/7, `ar.didgetaller`, 11518152 bytes, SHA-256 `c7b56895053f889019f51bedc338fa8e3bef2dbb1840bf1bd282de4a86b9ef33`. JDK17/SDK35, mismas dependencias/permisos. Gate `testDebugUnitTest assembleDebug assembleDebugAndroidTest`: **BUILD SUCCESSFUL**, **76 JVM, cero fallos/errores/omitidos** (`checkpoints/v321-final-build.log`, XML de app/build/test-results).

| Emulador emulator-5554 | Resultado limpio | Log checkpoints/ |
|---|---:|---|
| PrecisionUiTest | 5/5 | v321-precision-instrumentation.log |
| StudioViewer + SessionNavigation + LocalPersistence | 25/25 | v321-regression-instrumentation.log |
| ViewerRotation | 2/2 | v321-rotation-instrumentation.log |
| 360 dp, fuente 1,3 | 3/3 | v321-360-large-instrumentation.log |
| 420 dp, fuente 1,5 | 3/3 | v321-420-large-instrumentation.log |

**32 casos distintos** (5+25+2), no una suite única de 32. Focos de tamaño/fuente adicionales, no duplicar su conteo. JSON de cada ejecución registra el SHA del mismo APK final. Toda instrumentación se dirigió explícitamente al emulador y tiene guarda ranchu/goldfish. Ninguna en Samsung.

### Causas y comprobaciones

- Atrás: Vista amplia solo cambiaba `vm.fullScreen`; el back stack seguía en Medir raíz y Android salía. Se agregó `measure/wide` y propiedad común de captura; no BackHandler. Probados tres ingresos/retornos, Back real por tecla y gesto de borde, escucha sola, silencio y grabación activa. Mismo `sessionStartedAtMs`, prefijo de muestras y archivo .part que continúa creciendo; ninguna sesión hasta detener y exactamente una al guardar.
- Zoom: razones independientes de separación X/Y, umbral por eje y factor limitado por evento eran inestables al alinear/cruzar dedos. Se reemplazaron por razón radial de centroides de Compose. La fórmula anterior combinaba centro actual con pan escalado por el span nuevo, desplazando el ancla al hacer ambas cosas. Ahora `inicio = anterior.low + anterior.span*(centro-pan) - nuevo.span*centro`, en espacio lineal/log correspondiente, con clamp final. Márgenes Y de Fundamental estables; no cuantización de viewport ni presets durante gestos.
- Raster: STFT reiniciaba ventanas en el inicio del viewport y cortaba al final visible, por lo que un pan cambiaba las observaciones. Ahora origen/hop fijos al WAV y contribución ponderada por soporte temporal. Sin copiar celdas vacías de un vecino. Se conserva Hann32768/hop16384 y la referencia relativa fija; cache visual v3 separada de análisis v2. Se eliminó redondeo entero del rectángulo dibujado y la espera de180ms tras soltar/resetear. No FFT nueva por cada movimiento de dedo.
- Pruebas geométricas: zoom2/inverso,120 incrementos equivalentes a uno, ancla con pan simultáneo X/Y, dato→píxel→dato en tres tamaños, límites/zoom extremos lineales/log y entradas inválidas. Prueba STFT de pan de200 muestras conserva exactamente las ventanas y potencias de apoyo; sin columnas vacías sobre señal, silencio real cero. UI inyecta pinch horizontal/vertical, verifica proporción y ancla, pan en ambos ejes, rotación, continuación/inverso/reset y referencia fija; incluye onda y armónicos. Toggle log en el bin lineal inferior dejó de producir intervalo degenerado.
- Fundamental: miniatura usaba min/max±4 Hz; visor percentiles/margen estrecho. Además, Analizar podía sustituir la serie por reanálisis de otra ventana/cadencia, mientras Biblioteca conservaba la original. Ahora misma serie capturada, mismos tiempos/validez, `PitchTrace` y vista general común. Si no hay serie histórica, se identifica la fuente de reanálisis. Ajustada sigue disponible para <1Hz, con advertencia de puntos fuera; General incluye todos los válidos y contexto de ±3 semitonos como mínimo. No suavizado añadido.
- Datos reales leídos del respaldo (sin recalcular ni escribir): sesión de17,32s,91 puntos/43 válidos,35,24–91,12Hz; de7,72s,40/26,37,92–76,67Hz. Otra incluye126,86→42,90→128,72Hz con confianza0,876/0,912. Esos saltos existen en el índice; no son picos inventados por Canvas. La confianza no permite afirmar si son sonido real o errores de periodicidad/octava; se conservan. Flauta real:115 puntos/65 válidos,481,63–1518,83Hz, sin inferir su nota tocada. Diagnóstico privado ignorado por Git: `v321-real-timeline.json`.
- Baja confianza no participa de la curva ni escala; se representa con círculo aislado cuando entra en rango, se informa cantidad en herramientas. Una anomalía válida por rango/confianza se mantiene aunque difiera. Nulos y gaps interrumpen segmentos. Tests de70±0,1, silencio,35Hz con baja/alta confianza y melodía392→440→523 sin suavizar. La misma serie se conserva al terminar análisis WAV. Resumen de Biblioteca sigue describiendo la nota predominante, no todos los sonidos.
- Lecturas: el título del visor ya no se reemplaza por números truncados; badge i/acción Lectura completa lleva al panel con texto que ocupa el ancho disponible y se adapta a líneas. Se verifican texto y ausencia de overflow, fuentes1,3/1,5. STFT únicamente Hz y dB relativos. Layout de48dp/márgenes de STFT idéntico a3.2; pruebas exigen ≥65% de altura útil para datos sin ejes en las configuraciones recorridas. [Capturas revisadas](screenshots/v321-final/README.md).

### Integridad y límites

Samsung **SM-G990E / R5CT21KTQ8W**: firma `fbf54fbe12161bdbf5adbfb9d906961511e9b49a715be8eac8f886e01292b7b2` compatible; `install -r` **Success**, arranque correcto, sin fatal en buffer crash del proceso. Respaldo fresco `samsung-before-3.2.1-20261021-112011`: **48 archivos / 10 WAV**. **47 archivos idénticos**; única diferencia: `files/profileInstalled`, marcador ProfileVerifier de 24 bytes con esquema 1 y fecha exactamente igual a `lastUpdateTime` del paquete. Índice, ajustes, relaciones, perfiles y WAV intactos. El teléfono estaba bloqueado; no se atribuye inspección visual física de 3.2.1. Al revalidar más tarde no había proceso activo, sin fatal en el buffer del PID de arranque; no se volvió a iniciar. Informe `checkpoints/v321-samsung-update.json`; no clear/uninstall/restauración/instrumentación. La cifra actual supera los 8 WAV de 3.2 porque el usuario grabó después.

11 módulos DSP/captura/reproducción/simulación/persistencia comparados byte a byte con el checkpoint inicial: idénticos; listado en `v321-final-evidence.json`. Solo se cambió el muestreo visual STFT, sin cambiar sus parámetros acústicos ni el reanálisis numérico existente.

Fallos investigados, no aprobados: captura de panel con dos raíces; lectura con ancho intrínseco/overflow (ahora ancho completo); screenshot anterior al render retardado de reset (demora eliminada); aserciones antes de completar Back gestual (esperar destino). El recorrido de fragmento inyectaba Back con la actividad RESUMED pero sin foco tras cerrar el menú; se comprobó en PrecisionBack y se espera devolución del foco, sin cambiar navegación de producción. Una comparación exacta de100Hz tras log se sustituyó por tolerancia1e-8Hz. Intento ampliado interrumpido al límite240s (`v321-instrumentation-timeout.log`); ejecución final separada con360s. SystemUI del emulador murió a11:12:43 (DeadSystemException), interrumpiendo wm density antes de comenzar pruebas; se hizo arranque frío sin snapshots ni wipe SOLO del AVD, y se evitan reconfiguraciones redundantes. Log `v321-systemui-recovery.log`. No se cuentan ejecuciones fallidas/incompletas.

Pendiente: pinch con dedos físicos/Samsung y percepción de fluidez/latencia, precisión de flauta/PVC contra afinador, sincronía audible,15min/batería y TalkBack completo. No se afirma que ruido/fundamental débil estén calibrados. Durante un gesto se conserva la tesela completada en sus coordenadas; si se sale de su cobertura, la nueva región llega al soltar/cargar, sin inventar energía. STFT≈743ms difumina transitorios y ampliar no agrega información; MediaPlayer no es sample-accurate/gapless. APIs consultadas: [gestos Compose](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures), [back stack](https://developer.android.com/guide/navigation/backstack).

## Cierre Acoustic Studio 3.2 — 21/10/2026

### APK y Samsung

- **3.2.0-beta1 / 6**, `ar.didgetaller`, JDK 17 / SDK 35; sin cambios de dependencias ni permisos. APK **11,624,005 bytes**, SHA-256 `08fbd64c52622ce2e601cc43ac6544a72211d27e06c36c3bd261a58e9201a0b5`. Certificado `fbf54fbe12161bdbf5adbfb9d906961511e9b49a715be8eac8f886e01292b7b2`, idéntico al APK instalado respaldado.
- Samsung **SM-G990E / R5CT21KTQ8W**: `adb -s R5CT21KTQ8W install -r` devolvió **Success**, arranque COLD correcto (1532 ms), PID inicial 28440, buffer crash vacío. No instrumentación, uninstall, pm clear, restauración, captura ni reproducción de audio en el teléfono.
- Respaldo fresco `backups/samsung-before-3.2-20261021-092022/`: tar 10.447.360 bytes, **27 archivos / 8 WAV** (el usuario tenía una sesión adicional frente a los siete WAV de 3.1). Índice, ajustes, relaciones y los ocho WAV conservan hashes. Verificación posterior completa: **25/27 archivos idénticos**; diferencias limitadas a los metadatos runtime documentados en `checkpoints/v32-samsung-update.json`, XML válido / marcador ProfileVerifier de 24 bytes con fecha compatible. Archivos nuevos exclusivamente derivados de análisis. No se ignoraron diferencias del usuario.
- Navegación física no destructiva por Biblioteca, sesión y visor; capturas reales antes/después de la MISMA sesión de **7,72 s**, inspeccionadas visualmente. Datos coloreados sin ejes: **306 / 1080 px (28.3%) → 773 / 1080 px (71.6%)**, porcentaje sobre altura TOTAL. No se extrapola esta sesión a todos los teléfonos. [Imágenes y matriz](screenshots/v32-final/README.md).

### Pruebas aprobadas

| Comprobación | Resultado | Evidencia |
|---|---|---|
| `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | **67 JVM**, cero fallos/errores/omitidos; APK y test APK compilados | XML `app/build/test-results/testDebugUnitTest/`, `checkpoints/v32-jvm-summary.json` |
| StudioViewer + SessionNavigation + LocalPersistence | **25/25**, 112,687 s, emulador | `checkpoints/v32-regression-final-instrumentation.log` |
| ViewerRotation | **2/2**, 22,43 s, emulador | `checkpoints/v32-rotation-close-instrumentation.log` |
| Fuente 1,5, 420 dp; visor/Medir/perfil persistido | **2/2**, 16,785 s | `checkpoints/v32-420-f150-final-instrumentation.log` |
| 360 dp, fuentes 1,0 y 1,3 | **2/2 por configuración** | `checkpoints/v32-360-f100-instrumentation.log`, `v32-360-f130-instrumentation.log` |

Son **27 casos funcionales distintos**, aprobados en ejecuciones limpias complementarias (25 + 2), no una suite única de 27. Los JSON `v32-*-run.json` registran el mismo SHA del APK final. Después del gate solo se ajustaron las pruebas de rotación y del selector de gráficos; sus APK de instrumentación se recompilaron. El foco final del selector real aprobó 2/2 (`checkpoints/v32-evidence-close-instrumentation.log`), sin modificar el APK de la app. Verifica que una lectura puntual no se arrastre a otro tipo de gráfico; las capturas finales proceden de ese recorrido por menú. Todas las pruebas instrumentadas apuntaron explícitamente a `emulator-5554`, con guarda de hardware/almacenamiento aislado. No se ejecutó connectedDebugAndroidTest sobre dispositivos conectados.

JVM: equivalencias 69,2957/C♯2, 261,6256/C4, 392/G4, 440/A4 y 880/A5; cents decimales, octavas, entradas inválidas, ticks exactos/adaptativos y escala por instrumento. YIN: flauta 200/261,63/329,63/392/440/659,26/880/1600 Hz (tolerancia 0,3%, confianza >0,90); H2 más intenso que H1 más ruido uniforme (0,5%, >0,80); silencio/ruido sin nota; regresión didge 35,5/50/65,4/69,3/70/98/125/210 Hz (0,4 Hz). PCM melódico de siete segmentos verifica saltos y pausa sin resumen de nota predominante ni órdenes armónicos inventados. No son grabaciones de flauta física.

Persistencia en emulador: JSON antiguo sin campo conserva bytes al leer, WAV intacto; nueva flauta guarda/renombra/reabre con su instrumento, caché y rango correctos aun seleccionando Didgeridoo para la próxima captura. Se probó el flujo real de micrófono/grabación/guardado en emulador (silencio); no se presenta como validación acústica. La ficha de flauta no propone asociar geometría de didgeridoo.

Visor: inspección temporal de coordenada calculada desde `PlotDataArea` (2000 ms ±20 ms), pan/pinch, restablecer, selección, seguimiento/reproducción, Atrás, recreación/rotación y cuatro gráficos. La prueba de rotación comprueba que el reproductor sigue activo al volver al detalle antes de pausarlo para una aserción de UI estable. Medidas de área de datos sin ejes en horizontal: **74,6% / 71,2% / 66,4% / 73,8%** para 360-1,0 / 360-1,3 / 420-1,0 / 420-1,5. [Matriz visual](screenshots/v32-final/README.md).

### Fallos corregidos y límites

- Capturas iniciales confirmaron que la causa principal era la suma de TopAppBar, acciones, leyenda y lecturas en filas. No había doble inset del Scaffold exterior; se consolidó el propietario de áreas seguras y la barra de estado del visor. Se reemplazaron filas por barras de 48 dp y panel contextual. Un primer ajuste aún quedaba bajo la proporción deseada: se redujeron los márgenes reservados medidos, conservando etiquetas/ejes.
- En 360 dp el criterio inicial de horizontal corto era insuficiente y un snackbar tapaba Pantalla completa: corregidos. Con fuente 1,5 el contador temporal se truncaba: mayor ancho y prioridad frente a botones de zoom alternativos; prueba sin elipsis.
- Selectores instrumentados ambiguos y dos reemplazos incorrectos de etiquetas se corrigieron. Una ejecución de rotación se interrumpió por ComposeNotIdle al continuar el cursor; otra falló por la misma espera al volver al detalle. La prueba final verifica preservación del player antes de pausarlo. Logs de fallo/interrupción conservados en checkpoints; **no se cuentan como aprobados**.
- YIN de flauta conserva periodicidad, usa tasa completa, ventana de entrada 8192 y comparación ~50 ms, actualización ~93 ms; no máxima FFT ni continuidad que impida saltos. La guarda de ±0,5% solo cubre extremos/interpolación. Los ataques/mezclas ambiguas pueden no tener lectura; la confianza no es probabilidad calibrada.
- STFT de 32768 muestras (~743 ms) conserva resolución espectral de 3.1: transitorios rápidos pueden verse difusos. La pista de fundamental usa ventanas más cortas. Melodías sin nota predominante no reciben una F0 global ficticia para numerar armónicos.
- Pendientes manuales: flauta/PVC reales contra afinador, soplido y fundamental extremadamente débil, sincronía audible, 15 minutos/batería, accesibilidad completa. MediaPlayer sigue sin promesa sample-accurate/gapless; cámara retenida en recreación, no tras muerte del proceso. No se modificó el simulador de didgeridoo.

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
