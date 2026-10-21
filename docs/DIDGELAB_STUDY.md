# DidgeLab vs. Didge Taller Lab — referencia para Codex

## Estado 3.0 beta1 — 20/10/2026

El análisis del **sonido grabado** (PSD/picos/candidatos armónicos) permanece separado del espectro de **impedancia simulado**. `AcousticModel` y `BoreOptimizer` no se modificaron. No asociar el pico de sonido número n con el pico de impedancia número n. Ya existe asociación opcional diseño/sesiones, con predicciones y mediciones explícitamente separadas; la validación de perfiles contra referencias externas sigue pendiente. El explorador incorpora espectrograma/onda/espectro/comparación y no contiene código portado de DidgeLab/CADSD/SciPy. Véanse [SESSION_EXPLORER.md](SESSION_EXPLORER.md), [AUDIO_ANALYSIS.md](AUDIO_ANALYSIS.md) y el [protocolo de validación](ACOUSTIC_VALIDATION.md).

**Fuente pública estudiada:** https://github.com/Didgitaldoo/didge-lab (README, introducción, organización de módulos y documentación/tutoriales). DidgeLab 3 fue anunciado en 2026. Licencia declarada por el autor: CC BY-NC-SA 4.0.

**Propósito de este Android:** uso personal y educativo, 100 % offline; aprender a diseñar didgeridoos (actualmente el usuario toca un PVC todos los días y quiere probar madera/agave). NO monetizar.

| Función | DidgeLab (según su documentación) | Didge Taller Lab (esta entrega) |
|---|---|---|
| Definir geometría interna | `geo`, segmentos y formas parametrizables | `BorePoint` y editor interactivo de puntos en mm |
| Calcular espectro de impedancia | `sim`, modelo de líneas de transmisión con backend Python/Cython | `AcousticModel` original, línea de transmisión 1D por secciones cilíndricas con pérdidas y radiación aproximadas |
| Localizar resonancias | picos simulados | picos interpolados en Hz, nota y CSV de geometría |
| Definir sonido objetivo | funciones de pérdida / objetivos de resonancia | fundamental + resonancias 2 y 3 por sliders |
| Optimización inversa | `evo`, formas, mutadores, función de pérdida y evolución | `BoreOptimizer` original con elitismo, mutación, penalización por saltos y objetivo en cents |
| Audios / reconstrucción acústica | módulo `fft` y experimentos de síntesis/reconstrucción | Medir/WAV, STFT/onda/espectro y comparación; no sintetiza/reconstruye instrumentos |
| Rendimiento | Python + backend Cython | Kotlin puro, búsqueda fuera del hilo principal |
| Validación acústica | referencias a Didgmo/DidjiImp | validación física externa **pendiente** |

## Ecuaciones/decisiones de nuestro modelo (NO portado)

Aproximamos la geometría continua mediante tramos cilíndricos de radio local `r`. En aire, `Zc = rho * c / (pi*r²)`. La propagación de un segmento usa:

`Zin = (Zload*cosh(gamma*dx) + Zc*sinh(gamma*dx)) / (cosh(gamma*dx) + (Zload/Zc)*sinh(gamma*dx))`, con `gamma = alpha + i*2*pi*f/c`.

`alpha` aproxima pérdidas termoviscosas en paredes según viscosidad del aire, radio y frecuencia. La carga de salida usa una aproximación de baja frecuencia `Zrad/Zc = 0.5*(k*a)^2 + i*0.6133*(k*a)`. La validez disminuye al subir `ka`. Se normaliza respecto a `Zc` en boquilla y se extraen máximos relativos. Este es un **modelo educativo**: ignora pérdidas en uniones y paredes irregulares, dispersión compleja, labios, cavidad bucal y dinámica no lineal. No predice timbre ni contrapresión real.

La búsqueda inversa evalúa 6 diámetros de control y longitud, usa una semilla pseudoaleatoria fija, conserva los mejores y muta; minimiza una suma ponderada de errores entre los tres primeros picos y las frecuencias objetivo, en cents. Se penalizan cambios geométricos abruptos. La convergencia depende de parámetros y **no garantiza el óptimo global**.

## Siguientes capacidades que queremos estudiar e incorporar

1. A/B comparación de curvas reales y simuladas; trazado logarítmico de impedancia y magnitud, tooltip de picos.
2. Perfil arbitrario editable (más puntos, importación/exportación CSV/JSON, guardar diseños con metadatos y unidades claras).
3. Objetivos flexibles (picos deseados/indeseados, amplitud y anchura, contrapresión aproximada, estabilidad del drone); multiobjetivo con frente de Pareto.
4. Optimización ajustable (población, generaciones, restricciones de longitudes/boquilla/campana, perfiles monotónicos o no, puntos congelados), cancelación y reanudación.
5. Comprobación cuantitativa contra DidgeLab con el **mismo perfil**, frecuencia y condiciones de frontera; reportar discrepancias y mejorar físicamente el motor.
6. Copia acústica de un instrumento existente a partir de mediciones/grabaciones: investigación exploratoria, NO suponer reconstrucción exacta a partir de la nota sola.
7. Visualización del volumen/cantidad de madera, forma de las dos mitades para fabricar, lista de dimensiones de taller, posibles cortes.
8. Mediciones reales del PVC del usuario y guardado con temperatura / dispositivo de micrófono; calibración del retardo y sesgos del sensor.
9. Introducción musical visual para alguien que toca de oído: diferencia entre armónico espectral y resonancia/toot, fundamental, nota, octava, cents, sin requerir partituras.

## Licencia y ética

DidgeLab: https://github.com/Didgitaldoo/didge-lab, CC BY-NC-SA 4.0. El autor permite copiar, adaptar y redistribuir de manera no comercial bajo atribución y compartir igual. Este paquete todavía no incluye fuentes de DidgeLab: implementa desde cero métodos estándar en Kotlin. Mantener mención al proyecto y URL por reconocimiento. Si se incorpora un módulo derivado directamente de DidgeLab, cumplir los términos CC BY-NC-SA 4.0 del original y documentar el cambio.

## Estado reproducible

`docs/LAB_JVM_CHECK.kt` verifica el núcleo sin Android SDK. Antes de continuar: compilar con Gradle en Android Studio y corregir errores de Compose / persistencia. Ver `docs/VALIDATION.md`.
