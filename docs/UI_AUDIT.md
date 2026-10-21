# Auditoría de interfaz — Didge Taller 3.1

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
