# Arquitectura de SciCalc

Calculadora científica para Android hecha con Kotlin y Jetpack Compose. Este documento explica cómo está organizada, por qué, y cómo se verificó.

## Idea central: un motor puro y una interfaz delgada

Toda la matemática vive en `engine/`, escrita en **Kotlin puro** (sin imports de Android). Eso permite probarla con pruebas unitarias normales, sin emulador, y reutilizarla sin cambios en la calculadora, en las gráficas y en las herramientas.

```
┌────────────────────────── ui/ (Jetpack Compose) ───────────────────────────┐
│  SciCalcApp ── navegación (Screen) + conexión de eventos                   │
│    ├─ CalculatorScreen  (teclado, pantalla, historial)                     │
│    ├─ GraphScreen       (curvas, puntos notables, integral)                │
│    ├─ ToolsScreen       (bases, unidades)                                  │
│    └─ DoomScreen / GameScreen (huevo de pascua: escribir 666 y "=")        │
│  ViewModels: CalculatorViewModel · GraphViewModel · GameViewModel          │
└───────────────┬─────────────────────────────────────────────┬──────────────┘
                │ estado hacia abajo, eventos hacia arriba     │
┌───────────────▼────────────── engine/ (Kotlin puro) ─────────▼──────────────┐
│  Expression.kt        tokenizador → parser → árbol → evaluación            │
│  ExpressionEditor     reglas de edición del texto (qué tecla es válida)    │
│  NumberFormatter      12 cifras significativas, sin ruido binario          │
│  Calculus             derivada, integral, raíces, extremos, intersecciones │
│  Viewport             matemática de zoom/desplazamiento de la gráfica      │
│  tools/               BaseConverter · UnitConverter                       │
│  game/                raycaster del minijuego                              │
└────────────────────────────────────────────────────────────────────────────┘
  data/CalculatorStore (SharedPreferences) · audio/SoundManager · sensor/TiltSensor
  doom/DoomEngine.kt + paquetes Java de Mocha Doom : Doom real en la JVM, con respaldo al minijuego en Kotlin
```

## El motor de expresiones

`ExpressionEngine.compile(texto)` produce una `CompiledExpression` que puede evaluarse muchas veces (una por columna de píxeles al graficar):

1. **Tokenizador**: reconoce números (incluida notación científica), operadores, funciones (`sin`/`sen`, `cos`, `tan`, `asin`, `acos`, `atan`, `sinh`, `cosh`, `tanh`, `√`, `ln`, `log`, `abs`), constantes (`π`, `e`), `Ans`, la variable `x` y los sufijos `%` y `!`.
2. **Parser de descenso recursivo** con esta precedencia (menor a mayor): `+ −` · `× ÷` y multiplicación implícita · signo unario · `^` (asociativa a la derecha) · sufijos `% !`.
3. **Evaluación** con errores tipados (`SYNTAX`, `DIVISION_BY_ZERO`, `DOMAIN`, `OVERFLOW`) que la interfaz traduce a mensajes en el idioma del usuario.

Decisiones con motivo:
- `−2^2 = −4` (el menos unario pesa menos que la potencia, como en matemáticas).
- `200 + 10% = 220`: el porcentaje a la derecha de `+`/`−` es relativo al operando izquierdo, como en una calculadora de bolsillo.
- Las funciones trigonométricas ajustan a **0 exacto** los valores "redondos" (sen 180° da 0, no 1.2E-16).
- Las inversas devuelven la unidad activa (grados o radianes) y `acos(0.5)` da exactamente 60.
- `n!` solo acepta enteros ≥ 0; 171! produce `OVERFLOW` en lugar de infinito.

## Cálculo numérico (`Calculus`)

| Operación | Método | Por qué |
|---|---|---|
| Derivada | Diferencias centrales con extrapolación de Richardson | Error del orden h⁴ con solo 4 evaluaciones |
| Integral | Simpson **adaptativo** (tolerancia 1e-10) | Refina solo donde la función cambia rápido (p. ej. picos de \|x\|) |
| Raíces | Muestreo + bisección; mínimos de \|f\| para raíces dobles | Detecta x² = 0 aunque no cambie de signo; descarta asíntotas (1/x) |
| Extremos | Muestreo + sección áurea | No necesita derivada analítica |
| Intersecciones | Raíces de f − g | Reutiliza el buscador de raíces |

## Herramientas

- **Bases**: `BigInteger`, así que no hay desbordamiento (hasta 64 dígitos).
- **Unidades**: cada unidad define `toBase`/`fromBase`; esto permite modelar la temperatura (que tiene desplazamiento, no solo factor). Una prueba comprueba que ida y vuelta entre **todas** las parejas de unidades devuelve el valor original.

## Gráficas

`Viewport` guarda centro y escala y convierte entre coordenadas del mundo y de pantalla. El zoom está **anclado a los dedos**: el punto bajo el centroide del gesto no se mueve. Las curvas se muestrean una vez cada 1.5 px, levantando el "lápiz" donde la función no está definida o cruza la pantalla de un salto (asíntotas). Los puntos notables se calculan solo sobre el rango de x visible.

## Interfaz y estado

- Un estado inmutable por pantalla (`CalculatorUiState`, `GraphUiState`) expuesto desde un `ViewModel`; la UI solo dibuja y emite eventos.
- Tres disposiciones responsivas (vertical, horizontal, expandida con historial fijo).
- Tema claro/oscuro, parallax con el acelerómetro (se desactiva si el sistema pide "quitar animaciones"), sonido y vibración, tres idiomas (es, en, it) con el mismo conjunto de claves.

## Verificación

Las pruebas unitarias cubren el motor (evaluación, precedencia, errores, formato, edición), el cálculo numérico, los conversores y la lógica del minijuego. El código de Android (Compose, el motor Doom corriendo en ART, sensores) no se pudo compilar en el entorno donde se desarrolló, así que se revisó leyéndolo; **la primera compilación en Android Studio es la verificación pendiente**.

## Licencias

El motor Doom (Mocha Doom) es GPL-3.0, por lo que distribuir un APK obliga a publicar el código bajo GPL. El WAD incluido es el shareware original de id Software, sin modificar (ver `app/src/main/assets/DOOM1-SHAREWARE-NOTICE.txt`). Ver `LICENSE-mochadoom.txt`.


## Doom (Mocha Doom en la JVM)

Mocha Doom se adaptó para correr en Android sin AWT ni javax.sound: la ventana, el audio, ImageIO y `java.nio.file` se reemplazaron o eliminaron; `System.exit` lanza `mochadoom.Exit.Quit`; el render es solo el indexado de 8 bits en modo serial y cada cuadro se entrega a `Engine.FrameSink` como ARGB. `mochadoom.Storage` redirige configuración, partidas y el mapa de translucidez a la carpeta privada de la app. `DoomEngine` (Kotlin) crea el motor en un hilo con pila de 32 MB, pausa el juego bloqueando el hilo en cada cambio de cuadro y traduce los botones táctiles a `ScanCode`. Verificado en una JVM de escritorio (arranque con el doom1.wad original, partida, pausa/reanudación, salida limpia, 64 MB de heap); **no probado en un dispositivo Android**.
