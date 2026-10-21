# Graph Intelligence & UX Stability 3.2.2 — 21/10/2026

Capturas auténticas del emulador API37, revisadas visualmente. Datos sintéticos de prueba identificados como fixtures; no son grabaciones físicas de instrumentos.

- Didgeridoo estable: [General](drone-general.png), [Ajustada](drone-fitted.png), [tras zoom extremo y Reset](after-extreme-reset.png), [nueva visita](reopened-general.png).
- Misma tarjeta [antes](card-before.png) y [después](card-after.png) de explorar; [preview al volver del fullscreen](preview-after-fullscreen.png). Se verifica igualdad de sus viewports completos mediante semántica de Compose, además de la inspección de imágenes.
- [Melodía de flauta](flute-melody.png), con frecuencias 392 → 440 → 523 → 440 Hz; instrumento de la sesión.
- [Estimación incierta original](uncertain-original.png), visible como círculo al habilitar Mostrar todos; [lectura completa](uncertain-reading.png), sin nota ni cents confirmados.
- [0 Hz sin nota](zero-hz-no-note.png): rango exacto 0–1 Hz solicitado explícitamente para probar los bordes, con aviso de datos fuera. No es una vista automática. Reset recupera el rango de datos.
- Horizontal: [Fundamental](regression-fundamental-landscape.png), espectrograma [360 dp / fuente 1,3](360-large-spectrogram-landscape.png) y [420 dp / fuente 1,5](420-large-spectrogram-landscape.png).
- Fuente grande: [Reset en 360 dp](360-large-after-extreme-reset.png), [lectura incierta en 420 dp](420-large-uncertain-reading.png).

La vista General de este drone queda aproximadamente en 58,9–83,3 Hz y Ajustada en 69,01–70,99 Hz. No hay suavizado: las pequeñas variaciones de la misma serie se amplifican en Ajustada. El círculo a 42,9 Hz permanece en el dato original, sin líneas que dibujen una V falsa. Los colores no son SPL absoluto; el espectrograma sigue mostrando solo Hz.

Se conservan barras de 48 dp e insets de 3.2. Área de datos STFT, sin ejes, sobre altura útil medida por Compose: 66,4% en 420dp/fuente1,0; 71,2% en 360dp/1,3 y 73,8% en 420dp/1,5. Logs v322-*-layout-metrics.log. No se presenta el 71,6% físico de 3.2 como una nueva medición en Samsung.

Las iteraciones descartadas y capturas redundantes están en checkpoints/v322-visual-iterations. [Validación](../../VALIDATION.md).
