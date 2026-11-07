<p align="center">
  <img src="docs/screenshots/showcase/didge-taller-mark.svg" alt="Ícono de Didge Taller" width="88">
</p>

<h1 align="center">Didge Taller</h1>

<p align="center"><strong>Una app Android sin conexión para escuchar, grabar y explorar el sonido del didgeridoo y la flauta nativa americana.</strong></p>

<p align="center"><a href="README.md">Documentación en inglés</a> · <a href="docs/DEVELOPMENT.md">Guía de desarrollo</a> · <a href="LICENSE">Licencia MIT</a></p>

Didge Taller reúne lecturas de frecuencia en vivo, grabación WAV y análisis de sesiones guardadas. El audio y los análisis quedan en el dispositivo, salvo que decidas exportar un WAV. Es un proyecto personal, sin fines comerciales y todavía en beta: sus lecturas acústicas sirven como evidencia para practicar, no como mediciones físicas calibradas.

## La app en uso

<table>
<tr>
<td width="50%" valign="top">
  <a href="docs/screenshots/showcase-es/session-analysis.png"><img src="docs/screenshots/showcase-es/session-analysis.png" alt="Sesión guardada con reproducción, pestañas compactas y una curva variable de la componente del dron" width="420"></a>
  <p><strong>Sesión guardada.</strong> Tocá el gráfico para buscar o arrastrá para recorrer el audio. Cursor, tiempo y lectura de la componente respaldada siguen la misma posición.</p>
</td>
<td width="50%" valign="top">
  <a href="docs/screenshots/showcase-es/spectrogram.png"><img src="docs/screenshots/showcase-es/spectrogram.png" alt="Espectrograma de la sesión con bandas armónicas variables y un intervalo de silencio" width="420"></a>
  <p><strong>Espectrograma.</strong> Observá cómo cambian la energía espectral y sus bandas armónicas durante la grabación.</p>
</td>
</tr>
<tr>
<td width="50%" valign="top">
  <a href="docs/screenshots/showcase-es/live-measurement.png"><img src="docs/screenshots/showcase-es/live-measurement.png" alt="Medición en vivo de didgeridoo con una lectura sintética estable de 70 Hz" width="420"></a>
  <p><strong>Medición en vivo.</strong> Leé una estimación de frecuencia aceptada, su nota y el estado de la señal mientras tocás.</p>
</td>
<td width="50%" valign="top">
  <a href="docs/screenshots/showcase-es/harmonic-analysis.png"><img src="docs/screenshots/showcase-es/harmonic-analysis.png" alt="Espectro armónico de un WAV analizado con frecuencia en Hz y nivel relativo en dB" width="420"></a>
  <p><strong>Análisis armónico.</strong> Inspeccioná los picos de un WAV analizado, con frecuencia en Hz y nivel relativo en dB.</p>
</td>
</tr>
<tr><td colspan="2" align="center">
  <a href="docs/screenshots/showcase-es/library.png"><img src="docs/screenshots/showcase-es/library.png" alt="Biblioteca con un estudio sintético del dron y su vista previa canónica" width="420"></a>
  <p>Biblioteca. Audio original, vista previa de la componente respaldada y acciones de sesión en un mismo lugar.</p>
</td></tr>
</table>

Las cinco imágenes destacadas son capturas originales de la app real en un emulador configurado en español. Las vistas de sesión usan audio sintético procesado por la app; la lectura en vivo usa PCM sintético controlado. Ilustran el funcionamiento, no la precisión física. [Condiciones y procedencia de las capturas](docs/screenshots/showcase-es/README.md).

## Qué podés hacer

- **Medir y grabar.** Elegí el perfil de didgeridoo o flauta nativa americana, seguí lecturas de frecuencia en vivo basadas en YIN con indicaciones de incertidumbre y guardá sesiones WAV PCM16 mono. El límite de grabación se configura entre 1 y 30 minutos.
- **Volver a tus sesiones.** Biblioteca muestra grabaciones guardadas con vistas previas de la traza musical. Podés reproducir, buscar una posición, cambiar nombres, exportar WAV, comparar sesiones o fragmentos y recuperar elementos de la Papelera.
- **Explorar el sonido.** Alterná entre Dron, Espectrograma, Onda y Armónicos. Analizá el WAV completo o un intervalo; usá la vista a pantalla completa para ampliar, desplazar e inspeccionar con el cursor. Herramientas reúne escalas, rangos, fragmentos y marcadores.
- **Examinar evidencia técnica.** Una sección separada de Diagnósticos avanzados permite consultar lecturas guardadas, la interpretación YIN y una comparación experimental opcional con SWIPE′ sin alterar el gráfico ordinario del dron.
- **Experimentar con diseños.** El Laboratorio de didgeridoo permite editar el perfil interior, simular resonancias aproximadas y realizar búsquedas exploratorias. No ofrece especificaciones de construcción validadas. Consultá la [guía del Laboratorio](README-LAB.md), en inglés.

La interfaz está disponible en español e inglés mediante los ajustes de idioma de Android. Didge Taller no requiere cuenta, servicios publicitarios ni permiso de Internet.

Durante la grabación o reproducción activa en primer plano, la app solicita mantener la pantalla encendida. Al pausar o terminar libera esa solicitud; no habilita el micrófono en segundo plano.

## Una sesión habitual

1. Elegí un instrumento en **Medir** e iniciá la medición y la grabación.
2. Guardá para conservar el WAV original y las lecturas capturadas en **Biblioteca**, o descartá después de confirmar.
3. Abrí la sesión para escucharla y seguir su gráfico. Usá las pestañas horizontales para explorar el espectrograma, la onda o el espectro armónico.
4. Ejecutá el análisis WAV cuando necesites más detalle espectral y armónico. Abrí **Herramientas** para rangos, fragmentos, marcadores y diagnósticos avanzados.

Para didgeridoo, la curva ordinaria de **Dron** sigue una componente espectral de baja frecuencia respaldada por armónicos. **No** es una fundamental de la oscilación labial confirmada físicamente, ni recibe automáticamente una nota musical. Las lecturas YIN en vivo y los diagnósticos avanzados YIN/SWIPE′ responden preguntas distintas; las lecturas históricas originales siguen disponibles. La [guía del explorador de sesiones](docs/SESSION_EXPLORER.md) y las [notas sobre incertidumbre acústica](docs/ACOUSTIC_UNCERTAINTY.md), en inglés, explican estas diferencias.

## Compilar desde el código fuente

Necesitás Android Studio, un JDK 21 instalado para el daemon de Gradle, Android SDK Platform 35, Build Tools 36.0.0, NDK 27.0.12077973 y CMake 3.22.1. El repositorio fija Gradle 9.6.0 y AGP 9.4.1; el bytecode de la aplicación sigue apuntando a Java 17. La guía de calidad explica las opciones temporales de compatibilidad Kotlin/DSL.

```sh
git clone https://github.com/hmeclazcke/DidgeTaller.git
cd DidgeTaller
```

Abrí el repositorio en Android Studio y ejecutá la configuración `app` en un emulador con Android 8.0 o superior, o en un dispositivo propio. También podés compilar el APK de depuración desde la raíz:

```powershell
.\gradlew.bat :app:assembleDebug
```

```sh
sh ./gradlew :app:assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`. Es una compilación de depuración, no una versión de producción firmada ni una distribución en Google Play. Antes de actualizar una instalación con grabaciones, conservá sus datos y verificá la compatibilidad del paquete y la firma. Ejecutá pruebas instrumentadas solo en un emulador seleccionado explícitamente. La [guía de desarrollo](docs/DEVELOPMENT.md), en inglés, contiene los comandos de prueba y las reglas de protección de dispositivos.

## Ingeniería y límites

La app usa Kotlin, Jetpack Compose/Material 3, `AudioRecord` y `MediaPlayer`. El procesamiento en Kotlin incluye estimación de periodicidad basada en YIN, FFT/STFT, análisis WAV y modelado acústico exploratorio; SWIPE′ usa un pequeño componente C/JNI con [avisos de licencia de origen](app/src/main/assets/third_party/SWIPE.txt). Para empezar por la parte técnica, consultá el [resumen de arquitectura](docs/ARCHITECTURE.md), el [análisis de audio](docs/AUDIO_ANALYSIS.md) y el [registro de validación](docs/VALIDATION.md), en inglés.

Los micrófonos de los teléfonos, el ruido de la sala, los armónicos intensos, las vocalizaciones y los ataques breves pueden producir estimaciones inciertas o ausentes. La app conserva los huecos en lugar de inventar lecturas. Los niveles espectrales son valores digitales relativos, no niveles calibrados de presión sonora; las simulaciones son exploratorias. Las pruebas sintéticas y las capturas de emulador no establecen precisión acústica física ni compatibilidad universal con dispositivos.

Los controles rápidos de GitHub Actions ejecutan ambas variantes JVM y controles medidos de regresión. Los cambios relevantes en main también ejecutan la suite completa de emulador con un informe separado de cobertura Android; compilar su APK no equivale a aprobarla. Consultá la [base de calidad](docs/quality/QUALITY_BASELINE.md), [Desarrollo](docs/DEVELOPMENT.md) y [comparación SWIPE](docs/SWIPE_COMPARISON.md).

## Prácticas de ingeniería - ub-mode oficial de Uncle Bob

Hay una [reconstrucción de ingeniería](docs/engineering/REBUILD_CASE_STUDY.md), documentada en inglés, **en curso** en `refactor/uncle-bob-rebuild`. Conserva main como referencia de comportamiento. El estudio registra cambios en DSP y trazas, reproducción independiente de Android, grabación con propiedad por captura, publicación atómica independiente de caché, contratos diferenciales y evidencia real de CI. La [revisión por archivo](docs/engineering/REBUILD_REVIEW.md), en inglés, registra el trabajo arquitectónico pendiente; la reconstrucción completa de la aplicación sigue en curso.

Didge Taller utiliza el [ub-mode](https://github.com/unclebob/ub-mode) original de Robert C. Martin, copiado sin modificaciones con su licencia MIT y [procedencia fijada](.agents/skills/ub-mode/UPSTREAM.md). La seguridad del proyecto y la ejecución para Kotlin se definen aparte en AGENTS.md y la [guía de calidad](docs/quality/QUALITY_GUIDE.md).

La [reconstrucción de persistencia](docs/engineering/PERSISTENCE_REBUILD.md), documentada en inglés, separa la validación de metadatos, la publicación del índice y la gestión de los WAV. Un índice dañado no puede autorizar el borrado de audio ni convertirse silenciosamente en una base vacía. La lectura de la última copia válida, la recuperación explícita que conserva el texto dañado, las pruebas de publicación concurrente y una única instantánea de biblioteca protegen los formatos existentes. Los controles de recuperación siguen siendo bilingües; la reconstrucción general del ViewModel y los renderizadores continúa en curso.

La [reconstrucción de eventos de captura](docs/engineering/CAPTURE_EVENT_OWNERSHIP.md), documentada en inglés, identifica los eventos terminales de cada trabajador y lleva las fases de grabación, las solicitudes inmutables de guardado y la finalización del ciclo de vida a un coordinador Kotlin puro. Una sola instantánea de Compose publica el estado de grabación. Una finalización tardía puede guardar su propio audio sin detener al monitor que la reemplaza. La conversión de potencia espectral a píxeles tiene pruebas independientes y admite cancelación; la reconstrucción general del espacio de sesión y los renderizadores continúa en curso.

- **First Make it Work:** modelar el dominio, validar entradas, definir quién controla el estado y aplicar TDD/rojo-verde-refactorización a cambios verificables.
- **Then Make it Right:** conservar el comportamiento al refactorizar; usar escenarios de aceptación BDD, cobertura, complejidad y mutaciones para investigar riesgo y conocimiento duplicado (DRY).
- **Practice:** investigar con foco, crear commits revisables, registrar evidencia concisa y validar según el riesgo. Los escenarios BDD no implican Cucumber.

Las herramientas reales son Gradle/JUnit, instrumentación Android, JaCoCo, Detekt, PMD CPD y PIT gratuito. CI verifica ambas variantes JVM, lint, localización, compilaciones y controles medidos de regresión cuando cambia código relevante. CPD detecta clones de tokens. PIT exige el umbral del alcance original de gestos Kotlin y genera informes de una selección ampliada de lógica acústica, reproducción, grabación y WAV mientras se investigan supervivientes. La suite completa de emulador se ejecuta en cambios relevantes de main, la rama de reconstrucción y manualmente; las mutaciones se ejecutan manualmente. Los cambios exclusivos de README evitan esos costos.

Las declaraciones del compilador PSI y las correspondencias inequívocas de fuente, propietario, parámetros, cuerpo y bytecode producen **CRAP-L** por método, usando explícitamente cobertura de líneas JaCoCo. Las transformaciones no soportadas quedan clasificadas y visibles. No es cobertura de caminos base de Crapper. Cobertura, complejidad, CRAP-L y duplicación tienen bases medidas para prevenir regresiones. Las mutaciones ampliadas de dominio siguen siendo informativas mientras se revisan supervivientes; el umbral del alcance original continúa obligatorio. La guía describe umbrales, alcance, informes y comandos.

**Límites de medición:** las coberturas JVM y de emulador se informan por separado. CRAP-L se controla solamente donde hay correspondencias de funciones revisadas; las demás quedan visibles sin puntuar. La fortaleza de las mutaciones corresponde a su alcance Kotlin seleccionado. La [base y deuda](docs/quality/QUALITY_BASELINE.md) conserva la complejidad histórica y los caminos sin cubrir; no se cambiaron algoritmos acústicos para mejorar métricas. Consultá los registros de ejecución para conocer el resultado real de CI.

La [auditoría de verificación](docs/quality/QUALITY_AUDIT_VERIFICATION.md), en inglés, compara informes numéricos, trabajos de CI ejecutados y pruebas de fallo. Las regresiones hacen fallar los trabajos; la protección de la rama es una configuración independiente. La caché exclusiva de descargas y los límites de concurrencia evitan reutilizar compilaciones o coberturas antiguas.

## Licencia y colaboración

Didge Taller se distribuye bajo la [licencia MIT](LICENSE). DidgeLab y CADSD son referencias de investigación, no motores incluidos; el contexto de atribución está en la [guía del Laboratorio](README-LAB.md). Antes de colaborar, leé [AGENTS.md](AGENTS.md), mantené los cambios acotados y dejá fuera de Git el audio privado, los diagnósticos locales y las credenciales.
