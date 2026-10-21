# Recovery 3.2.5 — evidencia nativa

22 capturas reales de emulador, revisadas visualmente. Sin imágenes generadas ni composiciones. APK actual3.2.5-beta1/11, SHA-256 `0a9bbfb7a780900432b3872e693031d38b3af924743ea0dc68bfc112ea1a149a`. Antes: APK3.2.2/8 y3.2.4/10, instalados exclusivamente en emulador. Mismo tamaño1080×2400/densidad411 y fuente1,0 salvo matriz explícita.

Las tres series históricas conservan exactamente sus lecturas/tiempos/duración del respaldo privado; los WAV del emulador son silenciosos, no audio del Samsung. Estas imágenes prueban presentación, no afinación física. No se modificaron originales.

| Serie |3.2.2|3.2.4|3.2.5|
|---|---|---|---|
|Re·D2|[Antes](before322-D2-detail.png)|[Verde uniforme](before324-D2-detail.png)|[Recuperada](regression-D2-detail.png)|
|Mib·Eb2|[Antes](before322-Eb2-detail.png)|[Tendencia recortada](before324-Eb2-detail.png)|[Originales respaldados y color](regression-Eb2-detail.png)|
|Reciente51,5s|[Antes](before322-Latest-detail.png)|[Escala distorsionada](before324-Latest-detail.png)|[Escala útil, huecos conservados](regression-Latest-detail.png)|

Fullscreen actual: [D2](current-D2-fullscreen.png), [reciente](current-Latest-fullscreen.png). Un ataque aislado fuera de General ya no dibuja un barrido hacia el tramo principal; permanece accesible al inspeccionar/rango completo. La reciente sigue fragmentada donde sus lecturas no respaldan continuidad; no se inventó una línea durante la «I».

Serie sintética para separar variación/periodicidad: [Biblioteca](colours-library.png), [detalle](colours-detail.png), [Ajustada](colours-fitted.png). Verde estable, amarillo moderado/contexto inicial, naranja variación fuerte o periodicidad débil. La oscilación grande de Ajustada es intencional en el fixture, no un defecto ni audio físico. La prueba comprueba píxeles reales de los tres colores y el hueco.

Medir con PCM controlado por el callback de producción: [drone](measure-drone.png), [armónicos reforzados](measure-harmonics.png), [sin señal](measure-silence.png), [flauta](measure-flute.png). El test inyecta estado de captura aislado, sin escritor ni AudioRecord; son estados nativos verificables de UI, no una grabación acústica ni vocalización real.

Matriz: [360dp/fuente1,3 reciente](360-f130-Latest.png), [420dp/fuente1,5 Eb2](420-f150-Eb2.png). Regresión: [espectrograma horizontal compacto](spectrogram-landscape.png), [melodía de flauta con pausa](flute-melody.png), ambos con audio sintético. Ejes legibles, márgenes y barras conservados; scroll del detalle puede dejar contenido anterior fuera de pantalla.

Manifiesto de fuentes/SHA de cada PNG: `checkpoints/v325-screenshots.json`. Conteos, reglas y diagnóstico de últimos15s reales: [VALIDATION](../../VALIDATION.md). Instalación Samsung no equivale a una validación musical del resultado.
