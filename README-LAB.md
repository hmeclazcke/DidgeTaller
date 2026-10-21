# Didge Taller Lab — Android Kotlin / Jetpack Compose

Proyecto personal y gratuito. **No es comercial**. Integra afinador, espectro de audio, grabador WAV, diseñador de perfil interior por puntos, simulación de resonancias y búsqueda evolutiva para aproximar tres notas.

## Para empezar en Android Studio

1. Abrí esta carpeta como proyecto.
2. Esperá a que Gradle sincronice. Usá JDK 17 y SDK Android 35.
3. En Windows corré desde el directorio raíz:

   `gradlew.bat :app:testDebugUnitTest`

   Ejecuta las pruebas unitarias del módulo `app` en configuración `debug`.

   `gradlew.bat :app:assembleDebug`

   Compila un APK de prueba sin publicar ni firmar para Play Store. El resultado suele estar en `app/build/outputs/apk/debug/app-debug.apk`.

4. Con un Android conectado y depuración USB habilitada, `gradlew.bat :app:installDebug` instala en el dispositivo de pruebas (solicitar permiso previo).

**Estado 20/10/2026:** APK debug compilado; siete pruebas JVM y dos instrumentadas de persistencia aprobadas. La interacción de audio del rediseño requiere validación manual. Antes de usar pruebas instrumentadas en el teléfono personal, leé la incidencia de conservación de datos en `docs/VALIDATION.md`.

## Tres secciones

- **Afinar:** nota, frecuencia, cents, control del micrófono, guardado de mediciones y grabador WAV.
- **Laboratorio:** Diseñar, Espectro, Buscar y Guía en pestañas internas.
- **Biblioteca:** Mediciones, Grabaciones y Diseños en pestañas internas.

## Codex CLI

Iniciar `codex` en este directorio. Leer `AGENTS.md` y `docs/DIDGELAB_STUDY.md` antes de cambiar código.

Prompt inicial: «Leé AGENTS.md y docs/DIDGELAB_STUDY.md. Primero compilá el proyecto Android y corregí errores hasta pasar `:app:testDebugUnitTest` y `:app:assembleDebug`. No agregues funciones mientras esté roto. Registrá lo verificado en docs/VALIDATION.md».

## Inspiración / crédito

DidgeLab: https://github.com/Didgitaldoo/didge-lab — kit de Python para diseño asistido de didgeridoos y evolución de perfiles, licencia **CC BY-NC-SA 4.0**.

CADSD: https://www.didgeridoo-physik.de/ — trabajos de Frank Geipel.

El motor Kotlin incluido **no copia directamente** código del repositorio Python; es una implementación original de técnicas matemáticas clásicas y búsqueda evolutiva. Si más adelante se derivan partes de DidgeLab, aplicar la atribución y licencia share-alike correspondientes.
