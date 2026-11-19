# Calculadora Científica (proyecto TipTime)

Calculadora científica en Jetpack Compose. Abre la carpeta en Android Studio y ejecuta `app`.

- Operaciones: + − × ÷, paréntesis, ^, √, %, sen/cos/tan (DEG/RAD), ln, log, π
- Extras: historial, modo oscuro, sonido, parallax por inclinación, diseño responsivo, gráficas
- Huevo de pascua: escribe `666` y pulsa `=` para abrir un minijuego de disparos en primera persona (raycasting)
- Idiomas: español, inglés, italiano (sigue el idioma del sistema)
- Motor puro en `engine/` con pruebas en `app/src/test` (`./gradlew test`)

---

## Doom real (Mocha Doom en la JVM, sin NDK)

Al escribir `666` y pulsar `=` se abre **Doom de verdad**, ahora **sin código nativo**: el motor es *Mocha Doom* (port de Doom a Java), adaptado a Android y ubicado en los paquetes `doom`, `rr`, `v`, `p`, `w`, `m`... dentro de `app/src/main/java/`. Se maneja desde Kotlin con `doom/DoomEngine.kt` (arranque, pausa, teclas, cuadros) y se dibuja en `ui/screens/DoomScreen.kt`. Usa el **Doom original shareware** (`app/src/main/assets/doom1.wad`, episodio 1, distribuible libremente sin modificar). Si tienes el juego completo, copia tu `DOOM.WAD` a `Android/data/com.example.tiptime/files/` en el teléfono y la app lo usará automáticamente.

**Ya no hace falta NDK, CMake ni JNI**, así que no hay problemas de arquitecturas (arm/x86): basta con abrir el proyecto, *Sync* y *Run*.

- El motor es Java (con la glue en Kotlin), porque Mocha Doom está escrito en Java; el interior se puede convertir a Kotlin con *Code → Convert Java File to Kotlin File*, aunque no es necesario.
- Resolución 640x400 (constante `DoomEngine.SCALE`; usa 1 si el teléfono va lento).
- Si el juego falla al iniciar, la app usa automáticamente el minijuego en Kotlin (raycaster).
- Sin sonido ni música en el Doom real (solo en el minijuego).
- Las partidas guardadas y la configuración van a la carpeta privada de la app.
- El motor solo puede iniciarse una vez por ejecución: si sales desde el menú del juego, hay que reiniciar la app para volver a jugar (el botón ✕ solo regresa a la calculadora y pausa la partida).
- Controles: ▲▼ avanzar/retroceder, ◀▶ girar, «» desplazarse de lado, DISPARAR, USAR, ⏎ y Esc (menú). Para empezar: ⏎ (menú) → New Game. El personaje corre siempre.

**Licencias:** Mocha Doom es GPL-3.0 (`LICENSE-mochadoom.txt`; copyright id Software, Victor Epitropou, Alexandre-Xavier Labonté-Lamoureux y Good Sign), por lo que el código completo de este proyecto debe distribuirse bajo GPL si publicas el APK. El WAD shareware es © id Software y se incluye sin modificar (`app/src/main/assets/DOOM1-SHAREWARE-NOTICE.txt`); el juego completo no se incluye.

---

## Más funciones

- **Teclado ampliado**: `2nd` (sen⁻¹, cos⁻¹, tan⁻¹, senh, cosh, tanh), `x!`, `abs`, `e`, `Ans` (resultado anterior) y memoria `MC` `MR` `M+` `M−` (aparece una "M" en pantalla cuando hay algo guardado).
- **Gráficas**: *Puntos notables* (raíces, máximos, mínimos e intersecciones f∩g), *Integral* (área sombreada entre a y b) y, al tocar la curva, coordenadas y pendiente.
- **Herramientas** (menú ⋮): conversor de bases (BIN/OCT/DEC/HEX), conversor de unidades (8 categorías).
- **Historial completo**: guarda todos los cálculos (sin límite) hasta que lo borres desde el menú ⋮. Las operaciones anteriores aparecen como una "cinta" sobre la pantalla: desliza hacia arriba para verlas todas; tocar una la recupera. El botón de reloj abre además la lista completa.

Arquitectura y decisiones de diseño: ver `ARCHITECTURE.md`.
