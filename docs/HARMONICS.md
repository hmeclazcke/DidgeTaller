# Candidatos armónicos — fase A

Entrada: PSD promedio real del WAV y fundamental representativa obtenida por periodicidad. Una fundamental incierta deja todos los picos **sin asignar**. No se infiere la F0 desde el pico más intenso ni se crean picos en sus múltiplos teóricos.

## Criterios implementados

1. Máximos locales interiores, por encima de −50 dB respecto del máximo y con prominencia local ≥6 dB aproximadamente (factor 4 de potencia) frente a los valles en ±6 bins.
2. Posición refinada mediante parábola en log-potencia de los tres bins vecinos. Precisión sub-bin solamente justificada para picos aislados y estables; no separa componentes que la ventana no resuelve.
3. Orden candidato: entero más cercano a `pico/F0`. Se acepta si el error en Hz queda dentro de `min(0,20×F0, max(0,75×binHz, esperado×(2^(35/1200)−1)))`. El límite evita tolerancias que se acerquen arbitrariamente al armónico vecino. El binning también influye; no son 35 cents de exactitud garantizada.
4. Se conservan hasta 64 picos por potencia; el panel inicial muestra los ocho más intensos, ordenados por frecuencia. Cada uno declara Hz medidos, dB relativos, orden si corresponde y desviación `1200 log2(medido/esperado)` en cents.
5. Energía en bandas de candidatos: suma de PSD en la unión de ±2 bins alrededor de picos asignados, dividida por energía total. Se etiqueta con ese nombre y ancho. Incluye ruido que caiga allí y excluye energía fuera de esas bandas: **no es THD ni una medida validada HNR/tonalidad**.

## Interpretación y ambigüedad

- Una H1 débil puede coexistir con H2/H3 intensas. Con H2+H3 puede recuperarse la periodicidad común aunque no haya pico H1.
- Una señal con solo 138,6 y 277,2 Hz no prueba una fundamental física de 69,3 Hz. Su periodicidad observable también corresponde a 138,6 Hz. Existe un test explícito para no forzar el subarmónico ausente.
- Un pico ajeno a la familia armónica queda sin asignar. Mezclas, ruido, ataques, barridos y vocalizaciones pueden volver ambiguas la periodicidad y la atribución.
- Armónico de sonido radiado, resonancia de impedancia del conducto y registro interpretado/toot son conceptos distintos. No se identifican voces, técnicas ni toots automáticamente.
- El espectro promedio mezcla momentos; no describe estabilidad de cada parcial. El explorador 3.0 ya ofrece espectrograma, selección de intervalos y comparación para estudiar la evolución; no implementa seguimiento automático/estabilidad individual de cada parcial. La fracción de bandas no califica calidad musical.

Normalización, ventanas, fuentes y tolerancias de pruebas: [AUDIO_ANALYSIS.md](AUDIO_ANALYSIS.md). La comparación con el modelo geométrico se prepara en [ACOUSTIC_VALIDATION.md](ACOUSTIC_VALIDATION.md).
