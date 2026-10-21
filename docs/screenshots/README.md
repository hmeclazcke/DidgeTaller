# Capturas reales del emulador

## Entrega 3.1 — antes y después

Las imágenes de `v31-final/` provienen de Compose ejecutado o screencap del emulador. Fixture: 70 Hz con componentes 140/210 Hz y silencio entre 5–6 s; también WAV sin análisis, nombres largos y biblioteca vacía. **No son mediciones del PVC ni maquetas.** Se completó el recorrido en cuatro configuraciones y focos posteriores de permiso/foco/formularios.

| Evidencia | Antes | Después |
|---|---|---|
| Marcar fin, 360 dp/fuente 1,3 | [Comprimido](v31-before/ui-audit/v31-before-360-f130-marcar-fin.png) | [Horizontal adaptable](v31-final/v31-final-360-f130-marcar-fin.png) |
| Detalle con nombre largo | [Cabecera extensa](v31-before/ui-audit/v31-before-360-f130-detalle.png) | [Resumen y reproductor](v31-final/v31-final-360-f130-detalle.png) |
| Laboratorio | [Diseñador como entrada](v31-before/ui-audit/v31-before-360-f130-disenar.png) | [Portada de herramientas](v31-final/v31-final-420-f150-laboratorio.png) |

- [Fundamental, escala útil](v31-final/v31-final-420-f150-visor-fundamental.png).
- [Espectrograma con leyenda](v31-final/v31-final-420-f150-visor-espectrograma.png).
- [Forma de onda](v31-final/v31-final-420-f150-visor-onda.png).
- [Armónicos y reproductor](v31-final/v31-final-420-f150-visor-armonicos.png).
- [Horizontal con audio activo](v31-final/v31-after-landscape.png).
- [Medir en foco con acción principal visible](v31-final/v31-final-360-f130-medir-foco.png).
- [Perfil y controles](v31-final/v31-final-360-f130-perfil-controles.png), [dimensiones](v31-final/v31-final-360-f130-dimensiones.png).
- [Comparador con nombres largos y fuente 1,5](v31-final/v31-final-420-f150-comparar.png).
- [Diseños sin fondo destructivo en reposo](v31-final/v31-close-disenos.png), [resonancias con formato español](v31-final/v31-close-simulacion.png).
- [Biblioteca vacía](v31-final/v31-final-420-f150-biblioteca-vacia.png).
- [Servicio TalkBack activado](v31-final/talkback-service-emulator.png): smoke parcial, sin evaluación de lectura audible.

Prefijos `360-f100`, `360-f130`, `420-f100`, `420-f150`: ancho en dp y factor de fuente. El inventario completo queda en `v31-final/`; previo en `v31-before/ui-audit/`; primeras iteraciones posteriores en `v31-after/`. Algunas capturas de matriz preceden ajustes menores finales de texto/fecha; los focos posteriores y VALIDATION identifican qué se volvió a comprobar. Las capturas históricas siguientes pertenecen a versiones anteriores.

AVD `Medium_Phone_API_37.0`, 20/10/2026. Son capturas de la app ejecutada, no maquetas. La entrada de audio del emulador no representa un didgeridoo real.

## Flujo Medir y sesiones vigente

- [Análisis WAV, fase A](analisis-wav-sintetico-emulador.png): captura real de Compose en emulador durante prueba aislada, sobre **un seno sintético de 66,1 Hz / 20 s** presentado como WAV recuperado. No es una medición del PVC ni evidencia de exactitud física; no es maqueta.
- [Detalle con flecha de navegación en barra superior](sesion-navegacion-emulador.png) — corrección Navigation Compose; captura real sobre sesión silenciosa existente en el emulador.
- [Medir en reposo](medir-reposo-emulador.png)
- [Medir en Vista amplia con el botón principal visible](medir-vista-amplia-emulador.png)
- [Medir a 900×1800 y fuente 1,3×](medir-pantalla-chica-emulador.png)
- [Medición en curso, con entrada silenciosa](medir-activa-emulador.png)
- [Biblioteca de sesiones](sesiones-emulador.png)
- [Detalle de una sesión sin nota confiable](sesion-detalle-emulador.png)

Estas capturas son del flujo nuevo antes del ajuste final de etiquetas del gráfico; la entrada era silencio y por eso no hay curva de frecuencia ni nota.

## Capturas históricas del flujo Afinar anterior

- [Afinar](afinar-emulador.png)
- [Afinar con pantalla más chica y fuente 1,3×](afinar-pantalla-chica-emulador.png)
- [Modo de máxima legibilidad](pantalla-completa-emulador.png)
- [Laboratorio](laboratorio-emulador.png)
- [Resultado de la búsqueda](buscar-resultado-emulador.png)
- [Biblioteca](biblioteca-emulador.png)
- [Grabaciones](grabaciones-emulador.png)

La captura de grabaciones muestra un WAV de prueba del emulador, luego eliminado del almacenamiento privado. Se exportó una copia a Descargas del emulador como parte de la validación.
# Captura 3.0 beta1

`v3-spectrogram-synthetic-66hz.png`: captura **real del emulador**, tomada durante `explorerRendersRealGraphsSeeksPausesLoopsAndCompares`. WAV sintético de 66,1 Hz/20 s, no una grabación del PVC ni una maqueta. Muestra espectrograma real Hann/STFT, etiquetas Hz y la navegación del detalle. El archivo fue obtenido con ADB desde el resultado de la prueba. No demuestra precisión física ni audio audible.

