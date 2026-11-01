# Dedsafio 3 (fan mod) — Fabric 1.21.1

Mod de fans inspirado en Dedsafio 3 (serie hardcore PvE en la que una ruleta diaria anuncia modificaciones que hacen el
mundo cada vez más difícil). Es un proyecto no oficial: los premios, números y texturas son **diseño propio** y todo es
editable desde el menú.

## Compilar

Requisitos: **JDK 21**. `./gradlew build` (Linux/macOS) o `gradlew.bat build` (Windows). El `.jar` queda en
`build/libs/dedsafio3-1.0.0.jar` (no uses el `-sources`). Ponlo en `mods/` junto a **Fabric API**.
Este repo también compila solo en GitHub Actions y publica el `.jar` en **Releases → latest**.

## Menú interactivo (tecla **K** o `/dedsafio menu`)

Solo para operadores (nivel 2) o el anfitrión del mundo local. Cada cambio lo valida y aplica el servidor.

- **General:** dificultad del mundo (pacífico/fácil/normal/difícil), PvP, mantener inventario, animación de muerte,
  tótems activos, espera entre giros y duración del giro.
- **Ruletas:** peso de cada color en la rueda (0 = desactivado) y botón de giro por color o aleatorio.
- **Mundo:** modificaciones persistentes (mobs más fuertes, sin regeneración natural, noche eterna), quitar todas y
  probar la animación de muerte. Se guardan en `config/dedsafio3.json`.

## Ruleta circular

La rueda tiene un sector por color; el ángulo de cada sector es proporcional a su peso. Gira, frena y el puntero (arriba)
elige el color; el servidor aplica entonces un premio de ese color. Hay una **Ruleta de Colores** (la rueda decide) y seis
ruletas de color fijo (la rueda siempre cae en su color). La **Ficha de Ruleta** gira la rueda aleatoria.

Colores (temática propia): rojo = castigos y modificaciones del mundo, naranja = fuego, amarillo = botín, verde = mejoras y
tregua (quita las modificaciones), azul = agua y ayuda, morado = azar.

## Otros contenidos

- **Ítems:** Dedita, Dedita de Plata, Dedita de Oro; Ficha de Ruleta; Tótem Rojo/Azul/Dorado (evitan la muerte desde
  cualquier casilla); Espada Caótica (efecto aleatorio por golpe).
- **Animación de muerte:** calavera animada a pantalla completa para quien muere y aviso para los demás.
- **Comandos (nivel 2):** `/dedsafio menu`, `/dedsafio ruleta <color|random> [jugadores]`, `/dedsafio muerte [jugadores]`,
  `/dedsafio quitar_modificaciones`.

## Personalizar

- **Premios:** `src/main/java/com/dedsafio3/Roulette.java`, una línea por premio:
  `add(RouletteColor.RED, "id", peso, (jugador, mundo) -> ...)`. Añade su traducción en `tools/generate_assets.py`
  (`PRIZES`) y ejecuta `python3 tools/generate_assets.py`.
- **Modificaciones del mundo:** `WorldModifier.java`.
- **Texturas:** sustituye los PNG de `src/main/resources/assets/dedsafio3/textures/` (mismos nombres). Animación de muerte:
  `textures/gui/death/frame_00..11.png` (64x64).
- `python3 tools/verify.py` comprueba que idiomas, modelos y premios estén sincronizados.

## Novedades 1.2.0 y 1.1.1 — Cambios de dificultad, mobs y Momentos Reviil

Menú **K → Cambios** (4 páginas). Cada cambio tiene un interruptor y un botón **Avisar**:
interruptor = activa/desactiva y, al activarlo, gira la **ruleta naranja** para todos; Avisar = solo lo escribe en el chat.

- **Dificultad:** Blazes naturales en el Nether · Sin miel (la botella no saca miel y colmenas/nidos/bloque de panal explotan al romperse) · Magma mortal (tocar un bloque de magma por cualquier cara mata) · Zombis trepadores (una parte escala paredes).
- **Mobs** (huevos en la pestaña creativa): Creeper Nuclear (nieve) · Reptisaurio Salvaje (cuevas) · Reptisaurio Guerrero / Arquero / con Lanza (asentamientos, aplican Veneno Primitivo) · Plumosaurio (pacífico, golpea fuertísimo si lo atacas) · Dromoraptor Rojo (manadas). Usan modelos de vanilla con textura propia: sustituye `textures/entity/<id>.png` por las tuyas.
- **Momento Reviil:** construye un búnker cerca, intercambia al azar las posiciones de los jugadores, oculta la vida y, al acabar el contador, mata a quien no esté dentro. Duración configurable en el menú.
