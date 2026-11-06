# Local data safety

Original WAVs, session indexes, user names and preferences are user-owned data. Never translate them, rewrite analysis to improve presentation, clear storage, uninstall a data-bearing installation or replace its signing identity. Tests run only on an explicitly selected emulator. Instrumentation may uninstall packages; an unqualified connected-test task is unsafe when a physical phone is attached.

A separately authorized physical update requires device/package/version/signature checks, confirmation that capture is inactive, a fresh read-only backup and SHA-256 baseline, an install-r update with the explicit serial, then startup and integrity checks. Stop on incompatibility or unexplained differences. Do not treat arbitrary changed preferences as Android-managed metadata. No physical-device operation is authorized by documentation cleanup.

The session index is stored in SharedPreferences; retain its UUID relationships to recordings. Trash keys include `trash_session` and `trash_design`; tools/associations use `session_tools`. Derived analysis cache is regenerable under `files/analysis-cache`. Progressive `.part` recordings have their own recovery policy; historical WAVs must not be rewritten.

For recovery, preserve the original backup and hashes. Validate archive paths, rejecting absolute paths, parent traversal and links, and extract only into a new local directory. Test complete restoration in isolated emulator storage before proposing it on a physical device. Never combine audio and an unrelated index or fabricate missing readings. Overwriting physical-device data requires explicit authorization and another fresh backup. App Trash is not an external backup.

## Historical device evidence

The original Spanish delivery records below retain their dates, counts and original wording. They are evidence of earlier updates, not observations about the current device or commands to repeat.

# Datos locales, respaldo y recuperación

## Actualización 3.1 — 20/10/2026

Respaldo original 3.0: `backups/samsung-before-3.1-20261020-203046/`, **24 archivos / 7 WAV**, tar 9.607.168 bytes. Se contrastaron SHA-256 con Samsung antes de actualizar y después de iniciar 3.1. Los 7 WAV, índice, ajustes, relaciones y cachés anteriores permanecieron idénticos (22 de 24 archivos). Solo cambiaron dos metadatos del runtime: ActivityThread.IDS y AndroidX `files/profileInstalled`; este último tiene 24 bytes y su fecha decodificada coincide con la actualización del paquete. No excluir otras diferencias automáticamente.

Certificado contrastado contra el APK instalado respaldado; versión 4→5, mismo paquete. Actualización exclusiva por `adb -s R5CT21KTQ8W install -r app/build/outputs/apk/debug/app-debug.apk` / **Success**. Arranque sin captura/reproducción/fatal. No se desinstaló, limpió ni restauró el teléfono.

Las pruebas usan datos aislados, salvo ViewerRotationTest, que recrea MainActivity real **solo en emulador**, crea un UUID propio y elimina exclusivamente ese fixture al terminar; no limpia preferencias ni otros registros. Nunca ejecutar esos tests en un teléfono de uso.

Respaldo inmediatamente anterior al último retoque: `backups/samsung-before-3.1-20261020-210152/`, señalado por `backups/latest.txt`. La segunda actualización 5→5 también conservó originales: 23/24 archivos idénticos respecto de ese respaldo, con cambio exclusivamente del marcador ProfileVerifier. Ambas actualizaciones usaron solamente install -r.

## Regla de dispositivos

**Toda instrumentación se ejecuta solo en `emulator-5554`.** Las clases instrumentadas verifican hardware ranchu/goldfish y usan directorios/preferencias de prueba aislados. No usar `connectedDebugAndroidTest` con un teléfono conectado, porque puede seleccionar el Samsung y desinstalar paquetes al finalizar.

En Samsung están prohibidos desinstalar, `pm clear`, borrar archivos privados o sustituir por otra firma. Actualizar únicamente mediante `adb -s SERIAL install -r APK`, luego de verificar paquete, versión y certificado. Ante incompatibilidad se detiene la instalación, sin remedios destructivos. No se inicia captura/análisis/reproducción de sus sesiones automáticamente.

## Respaldo de esta entrega

Se creó un tar local en `backups/samsung-before-3.0-20261020-162418/private-data.tar`, con **5 WAV**, preferencias y archivos privados existentes (9 archivos, 5.900.288 bytes incluyendo cabeceras/bloques tar). La lectura usó `adb -s R5CT21KTQ8W exec-out run-as ar.didgetaller tar -cf - shared_prefs files`, guardando el flujo binario mediante Python, sin redirección binaria de Windows PowerShell. No se escribió nada en el teléfono.

Se abrió el tar, se calcularon SHA-256 de cada miembro y se compararon con `sha256sum` en el dispositivo para preferencias y WAV. Se conservan `hashes.json`, `device-hashes-before.txt`, `package-before.txt` y el APK previamente instalado. `backups/latest.txt` indica la carpeta completa. Los audios personales y estos respaldos están ignorados por Git y no deben publicarse.

## Compatibilidad

No hay reemplazo de esquema. El índice y WAV históricos permanecen tal cual al actualizar. Papelera usa nuevas claves `trash_session`/`trash_design`; asociaciones/ajustes/anotaciones usan `session_tools`. La caché es regenerable en `files/analysis-cache`. Los archivos `.part` nuevos son la única captura reparable automáticamente; no se reescriben WAV anteriores. Las pruebas comparan originales antes/después de analizar y de mover/restaurar papelera.

## Recuperación verificable, si fuera necesaria

1. Conservar el tar original y sus hashes en la PC. Abrirlo y verificar SHA-256 antes de extraer. Extraer únicamente en una carpeta nueva local; rechazar rutas absolutas, `..` y enlaces. No extraer directamente sobre datos del Samsung.
2. Un respaldo de audio puede copiarse desde PC a un destino elegido y abrirse con un lector WAV independiente, sin modificar el original. El JSON del índice vive dentro del XML de SharedPreferences; conservarlo con sus UUID.
3. Probar cualquier restauración completa en un contexto aislado del emulador y comprobar índice, UUID, formatos y hashes. No mezclar una carpeta `recordings` con un índice de otro respaldo ni inventar lecturas faltantes.
4. Una restauración que sobrescriba datos del Samsung requiere autorización explícita y un nuevo respaldo de su estado actual. No se ejecutó una restauración en el teléfono en esta entrega. La verificación del tar no equivale a un ensayo de restauración integral sobre el Samsung.

La papelera no es un respaldo externo: hasta vaciarla permite restaurar desde la app, pero desaparece si se borra el almacenamiento de la aplicación. Evitar tareas Gradle/ADB que eliminen datos reales.
