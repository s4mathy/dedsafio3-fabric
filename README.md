# Dedsafio 3 (fan mod) — Fabric 1.21.1

Mod de fans inspirado en Dedsafio 3. Es un proyecto no oficial: los premios, números y texturas son **diseño propio**
(no tengo la lista oficial de la serie), pensado para que lo ajustes fácilmente.

## Compilar

Requisitos: **JDK 21**. Desde la carpeta del proyecto:

```
./gradlew build        # Linux / macOS
gradlew.bat build      # Windows
```

El `.jar` queda en `build/libs/dedsafio3-1.0.0.jar` (no uses el `-sources`). Ponlo en `mods/` junto a **Fabric API**.
Para probarlo en desarrollo: `./gradlew runClient`.

> Si Gradle no resuelve alguna versión, revisa `gradle.properties` (`yarn_mappings`, `loader_version`, `fabric_version`)
> contra <https://fabricmc.net/develop/> para 1.21.1.

## Contenido

**Ítems** (pestaña creativa "Dedsafio 3")
- Dedita, Dedita de Plata, Dedita de Oro (moneda; 9 de una = 1 de la siguiente, y al revés).
- Ficha de Ruleta: clic derecho gira una ruleta de color aleatorio.
- Tótem Rojo / Azul / Dorado: evitan la muerte desde cualquier casilla del inventario y dan efectos.
- Espada Caótica: cada golpe aplica un efecto aleatorio.

**Ruletas** (6 bloques: roja, naranja, amarilla, verde, azul, morada)
- Clic derecho → animación de tira que se desplaza y frena (5 s) con sonido; el premio se aplica al terminar.
- 37 premios en total, cada color con su temática (roja = castigos, naranja = fuego, amarilla = botín, verde = mejoras, azul = agua/ayuda, morada = azar).

**Animación de muerte**
- Quien muere: destello rojo, pantalla oscura, calavera animada de 12 fotogramas y "HAS MUERTO".
- Los demás jugadores: aviso compacto arriba con el nombre. Partículas de almas y sonido en el sitio de la muerte.

**Comandos** (nivel 2)
- `/dedsafio ruleta <color> [jugadores]`
- `/dedsafio muerte [jugadores]` — prueba la animación sin matar.

## Personalizar

- **Premios:** `src/main/java/com/dedsafio3/Roulette.java`. Una línea por premio:
  `add(RouletteColor.RED, "id", peso, (jugador, mundo) -> ...)`. Añade su traducción
  `roulette.dedsafio3.<color>.<id>` en `tools/generate_assets.py` (diccionario `PRIZES`) y ejecuta el script.
- **Texturas:** las actuales son placeholders generados por `tools/generate_assets.py`. Sustituye los PNG de
  `src/main/resources/assets/dedsafio3/textures/` por los tuyos (mismos nombres). Para la animación de muerte basta con
  reemplazar `textures/gui/death/frame_00..11.png` (64x64) o cambiar `FRAME_COUNT` en `DeathOverlay.java`.
- **Regenerar recursos:** `python3 tools/generate_assets.py` (necesita Pillow). `python3 tools/verify.py` comprueba que
  idiomas, modelos y premios estén sincronizados.
