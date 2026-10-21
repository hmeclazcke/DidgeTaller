# Validación acústica: estructura de continuidad

## Estado real

Fase A valida procesamiento digital con señales conocidas, una DFT de referencia independiente escrita en los tests y conservación de potencia analítica. No hay todavía comparación numérica ejecutada contra DidgeLab/CADSD ni grabaciones físicas etiquetadas. SciPy se consultó como documentación; no está instalado en este entorno y no se ejecutó como oráculo. El motor `AcousticModel` y el evolutivo permanecen sin cambios.

## Separar los experimentos

| Experimento | Referencia y datos necesarios | Qué puede concluirse |
| --- | --- | --- |
| F0/PSD de WAV | WAV original, SHA-256, fs/canales, intervalo exacto en frames, ventana/hop, detrend, normalización, herramienta/versiones | Error de F0 en Hz/cents, picos de audio y diferencias numéricas de PSD bajo configuración equivalente |
| Resonancias del conducto | Perfil por puntos mm, velocidad del sonido, fronteras, radiación, pérdidas, resolución, espectro de impedancia de referencia | Diferencias entre **modelos de impedancia** para el mismo perfil y condiciones |
| Instrumento tocado vs modelo | Perfil realmente medido, temperatura, boquilla, técnica, distancia/orientación de teléfono y protocolo repetible | Comparación exploratoria de drone/registros y predicción; no igualdad entre picos de sonido e impedancia |

El caso de 150 cm × 40 mm del pedido es ilustrativo y no se añadió como instrumento real del usuario. Las lecturas previas informadas (66,1; 66,9; 69,6; ocasionalmente 71–73 Hz) son observaciones, no tonos de referencia ni registros reconstruidos.

## Protocolo para siguientes fases

1. B/C: exportar datos de espectro con frecuencia y PSD lineal, no solo el gráfico normalizado; guardar parámetros y límites efectivos del intervalo. Comparar contra Welch con Hann periódica explícita. Para igualar exactamente el oráculo, usar intervalos de ventanas completas o reproducir nuestra cola parcial ponderada.
2. D: conservar WAV etiquetados de drone, registros y vocalizaciones; comparar fragmentos equivalentes, con normalización opcional y protocolo de micrófono. No introducir un clasificador de toots como confiable sin ese conjunto.
3. E: añadir fixtures de perfiles/impedancia externos con URL, versión/commit, licencia, unidades y tolerancias justificadas. Comparar cilindro, cono y perfiles variables; reportar Hz/cents y condiciones de radiación/pérdidas. Las actuales aproximaciones cilíndricas no equivalen a formulaciones cónicas analíticas ni a labios/cavidad bucal.
4. Antes de cualquier migración persistente: copia verificable y prueba de recuperación en emulador; conservar originales. La fase A no hace migraciones.

Referencias consultadas: [DidgeLab, modelo/evolución y referencias bibliográficas](https://github.com/Didgitaldoo/didge-lab), [Didgeridoo Physik/CADSD](https://www.didgeridoo-physik.de/), [Welch/SciPy](https://docs.scipy.org/doc/scipy/reference/generated/scipy.signal.welch.html). El README de DidgeLab remite a Mapes-Riordan (1991), elementos cónicos/cilíndricos de línea de transmisión. Es un punto de estudio para E; no se implementó ni validó esa formulación en esta fase. Ver [DIDGELAB_STUDY.md](DIDGELAB_STUDY.md) para las fórmulas actuales y licencias. Didgeridoo Breath se revisó como contexto de instrumentos; Didge Project no respondió durante la consulta. Ninguno se usa como oráculo de PSD.
