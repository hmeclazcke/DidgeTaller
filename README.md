# Didge Taller Lab · Android Kotlin + Jetpack Compose 🎵

**Proyecto estrictamente PERSONAL, NO COMERCIAL.** Para la actualización de simulación y búsqueda evolutiva leé también [`README-LAB.md`](README-LAB.md) y [`docs/DIDGELAB_STUDY.md`](docs/DIDGELAB_STUDY.md).

Aplicación **nativa** para medir el drone de un didgeridoo, estudiar su espectro y experimentar con diseños de conductos interiores. **Sin Internet, cuentas, publicidad ni pagos.**

## Versión 3.1.0-beta1 — 20/10/2026

- **Medir:** un botón inicia captura, nota/Hz y WAV; detener o salir guarda automáticamente. Escritura progresiva, límite inicial 15 minutos, ajustes 1/5/15/30 y recuperación de capturas interrumpidas propias. Sin escucha automática ni grabación oculta.
- **Visor inmersivo 3.1:** cuatro gráficos a pantalla completa, pan en dos ejes, pinch, inspección, reset, seguimiento opcional y horizontal conservando reproducción. Acciones adaptables, resumen compacto y navegación de herramientas. [Auditoría visual](docs/UI_AUDIT.md), [arquitectura del visor](docs/IMMERSIVE_VIEWER.md).
- **Explorador de sesión:** fundamental temporal, espectrograma real a color, onda con zoom hasta muestras y espectro con picos/Hn candidatos. Un gráfico grande por vez; zoom, desplazamiento, rango Hz lineal/log, selección de posición/intervalo y reanálisis cancelable.
- **Audio:** reproducción/pausa, reinicio, búsqueda y bucle de fragmentos, cursores desde MediaPlayer, exportación WAV por bloques. El bucle no garantiza continuidad entre muestras; sincronía física/audible pendiente.
- **Biblioteca:** sesiones/diseños y lecturas históricas compatibles. Tarjetas sin fecha repetida, swipe izquierdo y Deshacer mediante papelera persistente; WAV y relaciones no se borran hasta vaciado explícito.
- **Comparar:** dos sesiones o fragmentos, espectros A/B superpuestos, normalización opcional y métricas reales (F0/nota, estabilidad/rango, centroide, bandas, candidatos).
- **Laboratorio:** diseñador, puntos/presets, simulación y búsqueda evolutiva conservados. Asociación de sesiones con diseños, predicciones y mediciones separadas. Marcadores manuales de vocalizaciones/posibles toots y glosario en español. Sin clasificador automático no validado.
- **Análisis original de fase A reutilizado**, caché persistente por versión/SHA del WAV, 64 MiB regenerables. No altera el audio ni reemplaza el resumen histórico.

Detalles, algoritmos y límites: [SESSION_EXPLORER](docs/SESSION_EXPLORER.md), [AUDIO_ANALYSIS](docs/AUDIO_ANALYSIS.md), [ROADMAP](docs/ROADMAP.md). Pruebas realmente ejecutadas, APK e instalación: [VALIDATION](docs/VALIDATION.md). Hay [capturas reales del emulador](docs/screenshots/), identificadas cuando usan señales sintéticas. La precisión física, reproducción audible, sincronía fina, accesibilidad y grabación sostenida de 15 minutos requieren comprobación manual. La simulación es propia y aproximada, no un motor CADSD/DidgeLab validado.

## Android Studio Rabbit 1 — Windows 11

1. Descomprimí el ZIP entero en una carpeta de tu PC (p. ej. `C:\dev\DidgeTaller`).
2. Abrí **Android Studio → Open** y elegí la carpeta raíz (la que contiene `settings.gradle.kts`).
3. Configurá JDK 17 (o el JDK integrado compatible) y **Android SDK Platform 35** desde SDK Manager. La primera sincronización requiere conexión para descargar dependencias.
4. Abrí la terminal en la raíz del proyecto. Ejecutá primero `gradlew.bat :app:testDebugUnitTest` y luego `gradlew.bat :app:assembleDebug`.
   - `gradlew.bat` descarga el **Gradle Wrapper JAR oficial** si falta y verifica su SHA-256. La distribución configurada es Gradle 8.13.
   - `:app:testDebugUnitTest` corre los tests JUnit; `:app:assembleDebug` compila el APK debug.
5. El resultado, si compila, está en `app\build\outputs\apk\debug\app-debug.apk`.
6. Con depuración USB autorizada, identificar el serial y actualizar con `adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk`. Verificar firma/versión y respaldo; no desinstalar para resolver incompatibilidades. No usar una tarea que seleccione indiscriminadamente dispositivos conectados.
7. En Medir, tocá **Iniciar medición** y concedé permiso. Tocá **Detener medición** para guardar automáticamente WAV y lecturas en Biblioteca → Sesiones. Exportar crea una copia elegida por vos.

No ejecutes `connectedDebugAndroidTest`, desinstalaciones, limpieza de datos ni reinstalaciones en el Samsung real sin autorización explícita: el 20/10/2026 una tarea instrumentada dejó la app desinstalada. Las pruebas instrumentadas se hacen solo en emulador y cada comando ADB debe indicar el serial del emulador. El teléfono real se reserva para comprobaciones manuales no destructivas.

Si Android Studio se queja de que no encuentra el Gradle Wrapper, **primero ejecutá `gradlew.bat --version` en la terminal de Android Studio**. Esto obtiene el JAR oficial y vuelve a habilitar la sincronización. El hash esperado está en `scripts/setup-wrapper.ps1` y se puede comprobar en https://gradle.org/release-checksums/ .

## Codex CLI (desde Android Studio o terminal)

1. Abrí la terminal **en la raíz del proyecto**.
2. Ejecutá `codex` (instalado y autenticado previamente).
3. Codex lee automáticamente `AGENTS.md`. Como primer prompt, usá:

```text
Leé AGENTS.md, docs/DIDGELAB_STUDY.md, docs/PRODUCT_CONTEXT.md, docs/ROADMAP.md y docs/VALIDATION.md.
Primero ejecutá los tests y compilá el APK debug. Si falla, corregí los errores hasta
obtener una compilación real y registrá evidencia en docs/VALIDATION.md.
Después continuá con la fase 0 del roadmap sin introducir funciones de pago ni red.
Explicame el propósito de cada comando o flag nuevo antes de usarlo.
```

## Proyecto

- Java → Kotlin nativo; Android Views → Compose Material 3.
- Gradle 8.13 (JAR del wrapper de arranque 8.11.1), AGP 8.13.2, Kotlin 2.2.21, Compose BOM 2025.04.01.
- `compileSdk 35`, `targetSdk 35`, `minSdk 26`, Java target 17.
- Paquete `ar.didgetaller` y claves locales conservadas de v1; sin permiso INTERNET.
- Licencia del código: MIT. Código independiente, **sin incorporar dependencias de DidgeLab**.

Ver `AGENTS.md` (instrucciones a Codex), `docs/PRODUCT_CONTEXT.md` (historia/referencias), `docs/ROADMAP.md` (prioridades), `docs/ARCHITECTURE.md` (arquitectura) y `docs/VALIDATION.md` (pruebas reales).
