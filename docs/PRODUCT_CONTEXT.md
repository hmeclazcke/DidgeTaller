# Contexto funcional y recorrido del proyecto

## ¿Por qué existe Didge Taller?

En octubre de 2026, durante una conversación sobre fabricar didgeridoos desde una habitación con luz natural en Tandil, surgió la posibilidad de empezar con madera local (álamo, paraíso, eucalipto) o tallos florales secos de agave. El usuario toca didgeridoo **unos 15 minutos diarios desde hace tiempo**, tiene un didgeridoo funcional de **PVC** y se considera principiante en teoría musical, no necesariamente en ejecución. Pretende aprender a fabricar instrumentos afinados, comprender la geometría interna y eventualmente evaluar venderlos artesanalmente. Quiere evitar gastos iniciales.

### Conversaciones que conducen a la app

1. Fabricación con método *split-and-hollow*: cortar longitudinalmente madera, ahuecar, probar sujetando mitades, sellar interior, pegar y afinar. No es un método improvisado: artesanos profesionales lo utilizan.
2. Alternativas sin abrir: eucalipto previamente hueco por termitas (Australia), perforación longitudinal, tallos florales de agave/pita cuyo centro es blando y fibroso; al secarse pueden vaciarse. Materiales locales y seguridad importan.
3. Interés por instrumentos donde el canal interno se ensancha (cónico, campana, geometrías variadas). Las notas, resonancias, armónicos, toots y contrapresión son conceptos que el usuario quiere entender sin leer partituras.
4. Encontramos DidgeLab, un software Python gratuito de simulación e investigación de geometrías; después CADSD en Didgeridoo Physik, que tiene una herramienta de diseño directo y otra de búsqueda de geometrías por optimización. Se mencionaron Didge Project y Didgeridoo Breath como ejemplos de fabricación/proceso.
5. El usuario preguntó cómo saber qué nota toca su PVC. Se creó primero una **PWA Didge Nota** que detectaba frecuencia/notas y luego un **prototipo nativo Android Java (Didge Taller)** de cinco pantallas y simulador acústico propio simplificado.
6. El usuario decidió trabajar con **Android Studio Rabbit 1 + Codex CLI**, solicitó migración completa a **Kotlin + Jetpack Compose**, optimizaciones, una app más ambiciosa y archivos duraderos para Codex que expliquen el contexto y el rumbo.

## Requisitos explícitos

- Android nativo, instalación en su celular por APK debug inicialmente, editor Android Studio; evitar dependencia de VS Code.
- Offline, sin cuotas ni compras; sin cuentas, publicidad ni transmisión de audio.
- Interfaz agradable, clara y en español. El afinador debe poder funcionar con un didgeridoo grave, no solo guitarras.
- Medir drone en Hz y nota (Do/Re/Mi, C/D/E), desviación en cents, confianza; mostrar espectro y registrar audio local.
- Diseñador de conductos, análisis de geometría, resonancias y comparación con medición real.
- Explicar lo técnico sin asumir saber música. Facilitar pruebas antes de comprar madera o herramientas.
- Código mantenible, pruebas verificables, futuro producto que pueda crecer y no infrinja licencias.

## Lo existente y lo que NO existe

Esta versión 2.0-beta1 incorpora un simulador independiente de física simplificada. No usa ni porta DidgeLab; tampoco incorpora CADSD o un modelo de aprendizaje automático. No hay generador evolutivo o inverso de perfiles todavía. Los diseños disponibles son presets y tres diámetros editables + longitud. La estimación no puede garantizar cómo sonará un didge real.

## Comparación responsable con herramientas de referencia

| Herramienta | Qué aporta | Qué podemos estudiar para nuestra hoja de ruta |
| --- | --- | --- |
| DidgeLab | Simulación de resonancias y búsqueda evolutiva de geometrías; toolkit Python | Precisión, definición de geometrías, objetivos de diseño, optimización independiente |
| CADSD / Didgeridoo Physik | Física del didge, predicción de timbre/resonancias, evolución dirigida | Documentación conceptual, pruebas y calibración; **no** integrar su software propietario |
| Didgeridoo Breath | Instrumentos profesionales, ejemplos de materiales y fabricación | Educación sobre geometría, tratamiento interior, tipos de madera/agave |
| Didge Project | Guías y ejemplos de construcción (PVC y otros) | Taller didáctico, ejercicios prácticos y tablas de medidas |
| Didge Nota PWA | Primer afinador web para teléfono | Simplicidad, medición del drone, UX rápida |

## Aspectos de seguridad y precisión

Una app que muestra nota precisa con micrófono de celular puede equivocarse por filtrado de graves o armónicos intensos; nunca ocultar confianza o margen de error. Los gráficos de espectro no identifican automáticamente toots, ya que los toots son notas tocadas mediante otra técnica. El trabajo de madera/fuego/colas debe tener precauciones específicas; no sugerir brasas en interiores.

## Actualización 20/10/2026

El alcance vigente es **uso personal y no comercial**. El código actual sí incluye perfil por puntos y búsqueda evolutiva original; la descripción anterior corresponde al prototipo previo. La interfaz se organiza en Afinar, Laboratorio y Biblioteca; mediciones, WAV y diseños tienen recorridos separados. El usuario informó dos lecturas iniciales reales de su PVC, C2 66,9 y 66,1 Hz, sin validación científica. El rediseño aún requiere interacción y contraste acústico en teléfono. Ver `VALIDATION.md` para la incidencia de datos del dispositivo conectado.

### Nueva decisión de producto: sesiones

El flujo vigente reemplaza Afinar por **Medir**. Una acción inicia lectura y WAV, y detener crea una sesión con audio y análisis temporal. Biblioteca se centra en Sesiones y Diseños. Una sesión sin lecturas confiables conserva el audio y declara que no se pudo determinar la nota. Grabaciones previas y mediciones antiguas se presentan como legado honesto, sin inventar correspondencia entre ellas. Laboratorio mantiene sus herramientas acústicas. La frase anterior sobre recorridos separados describe la versión previa.
