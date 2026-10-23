# Auditoría de interfaz — Didge Taller 3.1

## Product polish audit — 2026-10-23

Baseline: clean `main/67b541d`, two documentation commits ahead of fetched
`origin/main/170bcf9`. Existing history and the separate conflicted worktree remain
intact. This review concerns interaction and presentation, not pitch accuracy.
YIN, optional SWIPE′, instrument ranges, PitchTrace and saved samples are unchanged.

### Screen and interaction inventory

| Area | Reviewed surfaces and state ownership | Rendered/interacted evidence |
| --- | --- | --- |
| Navigation | Measure, wide Measure, Laboratory home and four tools, Library tabs, session detail, comparison, four immersive charts; NavHost owns Back | Actual route/Back tests and emulator screenshots; no new BackHandler |
| Measurement | Instrument menu, capture limit/settings, idle/listening/uncertain/permission states, note/Hz/cents, live trace, acoustic diagnostics, start/stop/save | Actual emulator AudioRecord plus presentation fixtures; ordinary/wide and rotation coverage |
| Laboratory | Design presets, bore points/dimensions, simulated resonances, save/export; live spectrum; inverse search/progress/cancel/apply; guide | Baseline matrix and final layout matrix; slider semantics reviewed; numerical engines untouched |
| Library | Sessions, Designs, legacy measurements, menus, recovered/no-analysis/empty states, swipe/Undo/trash/restore, session comparison | Isolated synthetic library, native navigation and delete/Undo tests; legacy detail reviewed in source |
| Session | Metadata/rename/delete/export, shared player, four chart tabs, viewport/selection/loop, WAV analysis, markers, design association, glossary | Synthetic-WAV player/seek tests and chart inventory; export callbacks and some secondary dialogs reviewed in source |
| Immersive viewer | Chart menu, transport/follow, reset/zoom/pan/inspection, rotation, scales/ranges, excerpt tools and advanced diagnostics | Actual viewer/rotation/edge-Back tests and portrait/landscape screenshots |
| Localization/theme | English/Spanish strings and formats, increased text size, semantic control names, application dark palette | Emulator layout matrix and localization tests; the app intentionally has one dark theme, including when Android uses light mode |

The OS document picker/export destination, complete TalkBack traversal, physical
predictive-Back animation and audible playback alignment are not certified by
these screenshots or automated tests. No Samsung or private acoustic fixture is
used. Test PCM/sessions are explicitly synthetic and isolated or individually owned.

### Prioritized findings and minimal corrections

| Priority | Observed issue / impact | Evidence and correction |
| --- | --- | --- |
| P0 | No crash, unintended data loss or blocked flow reproduced in the exercised inventory | This is bounded evidence, not proof of absence across all devices |
| P1 | Rotation closes an active recording unexpectedly | Actual Activity test entered SAVING instead of CAPTURING; check configuration change before stopping capture, retaining normal background-save policy |
| P1 | Short landscape wide measurement pushes frequency/status below the visible area | Native screenshot and a failing visibility assertion; use a height-dependent two-column note/readout card and combine the instrument label with the capture heading |
| P1 | Playback seeking is hidden under excerpt/selection tools | Add a labeled position slider next to Play/Restart and elapsed/total time in the existing chart card; reuse the player and existing clock |
| P1 | Estimator comparison is prominent in ordinary Fundamental analysis | Move switch and signal-window inspector into collapsed Advanced diagnostics; keep the active chart legend visible and preserve deliberate activation/cancellation |
| P1 | Sliders and the comparison switch lack self-contained accessible names | Add localized semantics to playback/comparison, dimensions and inverse-design targets; explicitly size comparison touch target to at least 48 dp |
| P2 | A separate full-width full-screen action pushes the chart down | Move that action into the existing card heading; no new screen or layout system |
| P2 | Dialog/sheet container colors inherit a purple Material default inconsistent with the app palette | Complete the existing dark surface-container tokens; preserve meaningful chart colors |
| P2 | Advanced numerical/chart controls remain dense and some repeated labels need richer accessibility context | Preserve working controls; record remaining work rather than restructuring the explorer |

The normal screen does not gain algorithm selection. In a saved session, use
**Advanced diagnostics → Compare pitch estimators**; Spanish:
**Diagnósticos avanzados → Comparar estimadores de tono**. Full-screen access is
under **Tools → Advanced diagnostics**. Opening diagnostics does not run SWIPE′;
collapsing an enabled comparison keeps its explicit legend, and its switch turns
it off. Closing diagnostics cancels a pending selected-window inspection.

### Playback and chart contract

The chart card already owned transport before this task. It now includes a seek
slider and a larger elapsed/total label. Both detail and immersive adapters use
the existing 80-ms MediaPlayer position helper; there is no second player or
wall-clock prediction. A slider drag previews only the thumb; release seeks the
existing player. Chart time remains the player's reported position. Tabs preserve
playback and their cameras; preview/immersive cameras retain their existing
separate owners. MediaPlayer polling/seeks remain approximate, not sample-accurate
or guaranteed gapless. There is no acoustic reclassification or visual smoothing.

Before/after evidence, configurations, actual counts and remaining limitations are
recorded in [VALIDATION](VALIDATION.md) and the [screenshot index](screenshots/ux-polish/README.md).
Historical audits below remain unchanged.

## Real Audio Diagnostics 3.2.6 — 21/10/2026

La serie nominal de19:17 contiene grupos reales de estimaciones35–36/60–68Hz; se conservan completos y se retiran dos enlaces que sugerían un barrido amplio con periodicidad débil. Las otras dos series y colores no cambian. Las capturas de esos datos privados quedan locales; las evidencias versionadas usan fixtures sintéticos explícitos. Medir muestra incertidumbre sin fingir una nota actual. STFT: margen147→69px, ancho útil+9,54% en el mismo fixture1080px/420dpi/fuente1,0. Fundamental ya medía75px; no se afirma una reducción inexistente. Unidad Hz en leyenda/rango, etiquetas X medidas sin superposición y fallback cuando el paso redondeado solo dejaba0s. Título de sesión íntegro al desplazarlo a la vista; scroll/insets/navegación intactos. Validación y límites acústicos en [VALIDATION](VALIDATION.md).

## Recovery 3.2.5 — 21/10/2026

Comparación nativa de las mismas series en APK3.2.2,3.2.4 y3.2.5, más fixtures explícitos de variación/periodicidad y PCM controlado de Medir. La revisión detectó un barrido engañoso desde un ataque débil aislado; se conserva como punto, sin unirlo al tramo respaldado. Los colores vuelven a expresar métricas y las conexiones sobre omisiones son discontinuas. Los huecos del registro reciente no desaparecen artificialmente: gran parte ya existe en la captura. La prueba exige los tres colores en píxeles reales del Canvas y nota/Hz visibles en Medir; Ajustada se inspecciona con el panel cerrado. Matriz, capturas e integridad: [VALIDATION](VALIDATION.md) y [evidencia](screenshots/v325-final/README.md). No es validación acústica física.

## Reparación selectiva de Fundamental 3.2.4 — 21/10/2026

La revisión de3.2.3 usó un fixture demasiado favorable al filtro: las **lecturas exactas** Re·D2/Mib·Eb2 reproducen la fragmentación que denunció el usuario. Se compararon visualmente las mismas series en APK3.2.2/8,3.2.3/9 y3.2.4/10, con fechas/duración/resumen originales y WAV silenciosos de soporte solo en emulador. Ahora Re·D2 recupera33 enlaces frente a15 y Mib·Eb2,19 frente a5; sus huecos reales permanecen. No se presentan estas capturas como prueba física del audio.

General queda60,58–85,68Hz para Re·D2 y63,77–90,18Hz para Mib·Eb2; ya no dominan períodos bajos inciertos. Lecturas originales accesibles en herramientas. Curva/escala común a tarjeta/detalle/fullscreen, sin decimación de F0. Se retiran «N fuera» y el párrafo permanente de gestos de Fundamental; «Línea: tendencia» evita confundir periodicidad moderada con certeza acústica.

Detalle916px en420dp/fuente1,0: eje115,55→74,55px(−35,48%), datos769,625→810,625px(+5,33%). Se conserva Paint real/margen4+4dp y ancho congelado durante gesto. Hz una vez, notas secundarias. Espectrograma/onda/controles horizontales/insets no modificados. Matriz360/420dp/fuentes1,3/1,5 aprobada y capturas inspeccionadas; [20 evidencias](screenshots/v324-final/README.md).39 casos UI distintos aprobados solo en emulador. Samsung actualizado con datos intactos; captura negra impide afirmar inspección visual física. Ver VALIDATION.

## Fundamental Precision 3.2.3 — 21/10/2026

Corrección focalizada de Fundamental, sin auditoría general. Capturas auténticas antes/después del mismo fixture70–73Hz con cuatro retornos débiles a60/65/77/82,5Hz: en3.2.2 forman V/segmentos naranjas; en3.2.3 la curva verde tiene huecos y los originales son círculos opcionales sin conexión. La transición respaldada205/207Hz permanece visible. Fixture explícito: no atribuir esta comparación al WAV del Samsung.

Medida420dp/fuente1,0: gráfico detalle916px; reserva189,825→115,550px (−39,13%), datos695,350→769,625px (+10,68%). Ancho por textos reales Hz/nota, margen exterior4dp+separación4dp y origen retenido durante gesto. Sin Hz superior redundante. Leyenda de una línea (dos con fuentes grandes) y ayuda completa en herramientas. [13 capturas inspeccionadas](screenshots/v323-final/README.md), incluyendo360/420dp, fuentes1,3/1,5 y horizontal, sin superposición/recorte.38 casos UI distintos aprobados solo en emulador. Evidencia y límites en [VALIDATION](VALIDATION.md).

## Graph Intelligence & UX Stability 3.2.2 — 21/10/2026

- General/Reset de drone70Hz vuelven a≈58,9–83,3Hz; Ajustada permite ver±0,15Hz. Zoom-out se detiene limpiamente en General, sin0–500 implícito.
- Tarjeta determinística confirmada antes/después de explorar. Preview recupera su cámara propia y una nueva visita crea estado inicial; flauta mantiene el perfil guardado aunque Medir esté en Didgeridoo.
- Incertidumbre contextual interrumpe la línea y queda disponible como círculo/original en Herramientas, sin nota confirmada. Datos confiables y silencios mantienen tiempos y cortes comunes. No suavizado ni decimación engañosa; clipping real en miniatura.
- 0Hz y región0–1Hz sin notas imposibles, también en semántica accesible. Hz primario/nota secundaria; espectrograma solo Hz.
- Revisados [fixtures y matriz visual](screenshots/v322-final/README.md); visor horizontal compacto conservado. Gestos físicos/Samsung y accesibilidad completa pendientes.

## Stability & Precision 3.2.1 — 21/10/2026

- Revisados Medir normal/amplio, misma tarjeta/detalle/visor de Fundamental, espectrograma vertical/horizontal, herramientas, flauta y fuentes ampliadas. [Selección de capturas](screenshots/v321-final/README.md).
- Corregidos: Atrás fuera de Vista amplia, razones de zoom singulares por eje, ancla incorrecta con pan+zoom, origen móvil de STFT, redondeo del bitmap y recorte antiguo tras reset. También intervalo degenerado al pasar el bin inferior a log.
- Fundamental general y miniatura ahora comparten serie y escala; ajustada amplía voluntariamente variaciones pequeñas. Datos inciertos aislados, pausas sin línea, anomalías válidas conservadas. El diagnóstico numérico confirma saltos presentes en el índice, sin atribuirlos todos al instrumento ni todos al detector.
- El título conserva el gráfico; información científica completa en panel adaptable con indicador i, sin reducir letra ni agregar barras. La advertencia de recorte corresponde al viewport actual, no al rango robusto de otro preset. Cambiar pestaña limpia la lectura de otro gráfico.
- Barras y márgenes de STFT de3.2 conservados; datos ≥65% de altura útil en recorridos verificados. No se vuelve a la distribución de28,3%. La medición física de71,6% corresponde a3.2, salvo evidencia adicional de3.2.1.
- Cierre:76 JVM y32 casos instrumentados distintos más focos; Samsung actualizado preservando datos. Gestos físicos, afinación real y accesibilidad completa pendientes.

## Cierre Acoustic Studio 3.2 — 21/10/2026

Auditoría focalizada y corregida: visor, Fundamental, Medir/perfiles, Biblioteca y detalle. No rediseño general. [Capturas reales revisadas](screenshots/v32-final/README.md).

- **Causa del visor comprimido:** TopAppBar y controles pensados para vertical, varias filas de leyenda/lectura/transporte, además de márgenes amplios de ejes. No se encontró doble padding del Scaffold exterior. Se reemplazó por cabecera 48 dp y transporte 48 dp, reunidos en una sola barra cuando la altura/fuente lo exige. Girar, reproducir, seguir y restablecer son iconos con objetivos táctiles; opciones, rangos y lectura completa están en Herramientas. Leyenda pequeña integrada en margen reservado.
- **Medición física:** espectrograma Samsung de la misma sesión, antes/después: **306 / 1080 px (28.3%) → 773 / 1080 px (71.6%)** de altura TOTAL de captura para los datos sin ejes. Se inspeccionaron ejes, colores, controles, leyenda y navegación. El gráfico domina después del cambio. En emulador el área de datos varía entre 66,4% y 74,6% de altura útil según tamaño/fuente; ver tabla del índice.
- **Fundamental:** Hz en primera línea y nota secundaria en segunda; ticks de frecuencia musical exacta cuando caben. Zoom fino utiliza marcas numéricas y una única referencia ≈. Ancho medido antes del Canvas; cursor completo con cents disponible en panel. Autoescala robusta y aviso explícito de puntos fuera del viewport. STFT continúa exclusivamente Hz/dB relativos.
- **Fuente grande y 360 dp:** se corrigieron contador con elipsis, criterio de barra única y snackbar que cubría acceso al visor. Medir adapta la nota al ancho medido; selector de instrumento visible y bloqueado mientras graba/guarda. Vertical sin scroll contenedor accidental ni controles superpuestos a ejes.
- **Flauta/Biblioteca:** instrumento discreto junto a fecha; detalle respeta perfil guardado; melodía aparece como notas variables. El Laboratorio dice didgeridoo y no ofrece asociación engañosa a flauta. Pausas no conservan una nota anterior.
- **Interacción:** gestos y dibujo comparten la misma región de datos, probada tras cambio de tamaño; preservación del reproductor/selección/cámara en rotación y Atrás comprobada en emulador. Samsung: comprobación visual y navegación, sin iniciar micrófono/reproducción. Pruebas acústicas físicas, audibles y accesibilidad completa pendientes.

## Alcance y evidencia (20/10/2026)

Auditoría sobre la interfaz real del emulador `emulator-5554`. Samsung excluido de pruebas instrumentadas. Línea base: 44 pruebas JVM sin fallos y APK debug compilado. No hay metadatos Git en esta copia; respaldo recuperable en `checkpoints/before-3.1-20261020-195852.zip`.

Capturas previas: `screenshots/v31-before/ui-audit/`, ancho 360 dp, fuente 1,3. Datos de prueba sintéticos, identificados como tales, generados exclusivamente en el emulador; las imágenes son capturas reales de Compose.

| Prioridad | Pantalla y reproducción | Hallazgo | Corrección / validación |
|---|---|---|---|
| P1 | Detalle, selección, 360 dp / fuente 1,3 | «Marcar fin» queda casi vertical: tres chips compiten en una Row sin adaptación. Captura `marcar-fin`. | Implementado y verificado: distribución adaptable sin comprimir texto. |
| P1 | Laboratorio, bajar formulario y cambiar pestaña | La barra de herramientas es parte del scroll vertical. Su scroll horizontal anidado impide recuperar la barra de manera predecible. Reproducido durante el recorrido automatizado. | Implementado y verificado: navegación a herramientas y barra fija. |
| P1 | Detalle, arrastrar gráfico verticalmente | El contenedor desplaza la pantalla; el gráfico solo administra el eje horizontal. | Implementado y verificado: destino inmersivo sin scroll contenedor, gestos compartidos en dos ejes. |
| P1 | Rotar durante reproducción / abrir nuevo visor | La limpieza global en cada destino y en onStop interrumpe el reproductor incluso cuando sigue siendo la misma sesión. Inspección de código. | Implementado y verificado: propiedad de recursos por sesión y distinción de cambio de configuración. |
| P2 | Detalle con nombre largo | Resumen y explicaciones ocupan más de una pantalla, relegando el audio. Captura `detalle`. | Implementado y verificado: jerarquía compacta y divulgación progresiva. |
| P2 | Fundamental cercana a 70 Hz | Rango fijo 0–500 Hz comprime las variaciones. | Implementado y verificado: escala robusta automática, contador explícito de valores fuera de rango. |
| P2 | Espectrograma al cambiar rango | Referencia de color variable con el máximo de cada viewport. Inspección de código. | Implementado y verificado: referencia conservada durante la exploración. |
| P2 | Varias pantallas | Filas de acciones y metadatos pueden estrechar botones con fuente grande; los colores predeterminados morados no coinciden con el tema. | Implementado y verificado: acciones adaptables y tema coherente. |

## Inventario inicial

Recorridos y capturados: Medir inicial, permiso denegado, captura silenciosa, foco, diseñador, perfil, simulación, espectro vacío, búsqueda, progreso, guía, Biblioteca, diseños y detalle. La captura inicial confirma el defecto reportado de selección. La inspección posterior se completó; ver resultados y matriz debajo.

## Decisiones

- Conservar algoritmos acústicos, formato de sesiones, WAV y papelera duradera existentes.
- Un único modelo de viewport y gestión de punteros, con adaptadores de representación según los ejes reales de cada gráfico.
- Navigation Compose para visor y herramientas; botón Atrás y flecha desapilan el mismo destino.
- La reproducción procede de MediaPlayer.currentPosition. El seguimiento visual se desactiva al explorar manualmente.
- Documentación Android consultada: [gestos](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures), [transformaciones multitáctiles](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch), [ciclo de vida](https://developer.android.com/guide/components/activities/activity-lifecycle). Implementación propia, sin código externo ni dependencias nuevas.

No considerar un hallazgo corregido hasta registrar pruebas y capturas posteriores.

## Validación posterior de la implementación

- Matriz real recorrida: 360 dp / fuente 1,0 y 1,3; 420 dp / fuente 1,0 y 1,5. Cuatro recorridos aprobados con capturas en `screenshots/v31-final/`. Inspección visual de Medir/ajustes, diseñador/perfil/dimensiones, reproducción, comparación, selección y cuatro gráficos. Capturas adicionales enfocan controles completos; desplazar un título apenas hasta el borde no se contó como inspección de todo su formulario.
- «Marcar fin» conserva texto horizontal y altura táctil ≥48 dp; pasa una aserción de relación ancho/alto. Se utiliza FlowRow con separación y paso de línea, sin reducir la fuente.
- El visor usa viewport/proyección común, pan 2D, zoom por eje anclado, doble toque, inspección y controles alternativos. Restablecer y herramientas permanecen visibles. Los presets y el rango logarítmico conservan la referencia de magnitud; fundamental tiene escala robusta y opción para mostrar extremos.
- Un cierre descubierto durante la primera prueba de rotación provenía de dibujar etiquetas con altura transitoria casi nula; se evita dibujar hasta contar con espacio útil. La rotación real de MainActivity pasó conservando reproductor, posición, pausa, selección, cámara y back stack. El botón Girar funciona con rotación automática bloqueada en el emulador.
- El gesto Atrás desde el borde del sistema volvió visor→sesión→Biblioteca y liberó audio al abandonar la sesión. Las flechas y el botón Atrás también pasaron; el listado conserva pestaña y scroll.
- Se detectó que Armónicos ocultaba los controles de pausa aunque podía continuar el audio. Corregido: el reproductor queda disponible también en el espectro, sin dibujar un cursor temporal sobre su eje Hz. Inspección real de H1 candidato, nivel/desviación y pausa aprobadas.
- Resumen compacto, nombres extensos con elipsis y acceso al nombre completo, fecha única en tarjetas, menú accesible de papelera, decimales y fechas en español. Formularios, presets y acciones usan distribución adaptable. La Biblioteca muestra honestamente audios sin nota y sin minigráficos inventados.
- Un aviso de permiso podía sobrevivir al inicio correcto de captura. Se retira al conceder acceso y se agregó enlace a Ajustes; volver de Ajustes no inicia grabación. Se comprueba por prueba de ciclo de vida y capturas de los estados de permiso/captura/foco.
- Prueba ampliada de swipe: un gesto corto rápido (80 ms) reveló que la velocidad de Material podía descartar antes del umbral. Se exige distancia ≥38% **al soltar**, además del settle de la animación; foco final aprobado con preservación del WAV y Deshacer real. Fuente de API estudiada: [SwipeToDismissBox de AndroidX](https://android.googlesource.com/platform/frameworks/support/+/15ccca2bd51eab204fbee3c140a3076621e8ea61/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/SwipeToDismissBox.kt). No se copió código externo.

## Límites de la auditoría

Las capturas contienen grabaciones sintéticas y silencio, no un didgeridoo físico. La navegación, controles y cálculos sí se ejecutan en la aplicación real. La batería/temperatura, precisión audible de cursor, TalkBack completo y una interpretación real sostenida de 15 minutos requieren validación adicional. Un WAV sintético de 900 s se abrió, inició/canceló STFT y generó su onda en unos 10,5 s en el recorrido del emulador; se verificó zoom a 441 muestras y conservación del archivo. Esa cifra incluye navegación y controles, no es una medición aislada de FFT ni un benchmark del Samsung.

## Cierre de la auditoría

No quedan P0/P1 conocidos sin corregir en los flujos comprobados. VALIDATION registra los gates y la instalación. TalkBack se activó y quedó ligado al servicio, con exploración táctil y etiquetas de aplicación identificables; sus ajustes se restauraron. No se comprobó lectura audible, foco completo ni recorrido integral. Esto sigue pendiente junto con sonido/rendimiento físicos. El APK quedó instalado en Samsung con Success y sus 7 WAV intactos.

### Último retoque visual validado

En Diseños se observó un borde rojo de la papelera en reposo por las diferentes esquinas redondeadas del fondo y la tarjeta. El fondo ahora es transparente en reposo y destructivo solo durante el arrastre. `finalDesignCardsAndResonancesAppearance` comprobó cero píxeles rojos en la tarjeta y generó captura real; swipe/Deshacer se repitieron en el mismo foco (2/2). La presentación de resonancias del diseñador usa los mismos valores del motor con formato decimal español, sin modificar sus cálculos. Capturas `v31-close-disenos.png` y `v31-close-simulacion.png`.
