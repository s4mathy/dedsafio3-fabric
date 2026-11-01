#!/usr/bin/env python3
"""
Genera TODOS los recursos del mod (texturas placeholder, modelos, blockstates,
loot tables, recetas, tags e idiomas) en src/main/resources.

Uso (desde la raíz del proyecto):   python3 tools/generate_assets.py

Las texturas son placeholders procedurales: reemplaza los PNG por los tuyos
(mismos nombres) y no hace falta tocar nada más. Si vuelves a ejecutar el script
se sobreescriben, así que edita este archivo si quieres cambiar algo de forma permanente.
"""
import json
import math
import os
import random

from PIL import Image, ImageDraw

MOD = "dedsafio3"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", MOD)
DATA = os.path.join(ROOT, "data", MOD)

COLORS = {  # id -> (rgb, tinte vanilla de tinte para la receta)
    "red": ((0xE5, 0x39, 0x35), "minecraft:red_dye"),
    "orange": ((0xFB, 0x8C, 0x00), "minecraft:orange_dye"),
    "yellow": ((0xFD, 0xD8, 0x35), "minecraft:yellow_dye"),
    "green": ((0x43, 0xA0, 0x47), "minecraft:green_dye"),
    "blue": ((0x1E, 0x88, 0xE5), "minecraft:blue_dye"),
    "purple": ((0x8E, 0x24, 0xAA), "minecraft:purple_dye"),
}

ITEMS = ["dedita", "dedita_plata", "dedita_oro", "ficha_ruleta",
         "totem_rojo", "totem_azul", "totem_dorado", "espada_caotica"]

# ----------------------------------------------------------------------------- idiomas
# Fuente única de nombres. Las claves de premios deben coincidir con Roulette.java
# (tools/verify.py lo comprueba).
PRIZES = {
    "red": {
        "explosion": ("¡Explosión!", "Explosion!"),
        "lightning": ("Rayo", "Lightning"),
        "zombies": ("Horda de zombis", "Zombie horde"),
        "hunger": ("Hambre y náuseas", "Hunger and nausea"),
        "wither": ("Marchitamiento", "Withering"),
        "weakness": ("Debilidad y fatiga", "Weakness and fatigue"),
        "mobs_stronger": ("Mobs más fuertes (mundo)", "Stronger mobs (world)"),
        "no_regen": ("Sin regeneración natural (mundo)", "No natural regeneration (world)"),
        "eternal_night": ("Noche eterna (mundo)", "Eternal night (world)"),
    },
    "orange": {
        "ignite": ("Prendido en llamas", "Set on fire"),
        "magma_floor": ("Suelo de magma", "Magma floor"),
        "blazes": ("Dos blazes", "Two blazes"),
        "fire_resistance": ("Resistencia al fuego", "Fire resistance"),
        "strength": ("Fuerza II", "Strength II"),
        "steak": ("8 bistecs cocinados", "8 cooked steaks"),
    },
    "yellow": {
        "deditas": ("3 deditas", "3 deditas"),
        "silver_dedita": ("Dedita de plata", "Silver dedita"),
        "gold_dedita": ("¡Dedita de oro!", "Gold dedita!"),
        "gold": ("8 lingotes de oro", "8 gold ingots"),
        "golden_apples": ("3 manzanas doradas", "3 golden apples"),
        "experience": ("5 niveles de experiencia", "5 experience levels"),
        "luck": ("Suerte II", "Luck II"),
    },
    "green": {
        "regeneration": ("Regeneración II", "Regeneration II"),
        "speed": ("Velocidad y salto", "Speed and jump"),
        "heal": ("Curación total", "Full heal"),
        "feast": ("16 panes", "16 bread"),
        "saturation": ("Saturación", "Saturation"),
        "bees": ("Enjambre de abejas", "Bee swarm"),
        "truce": ("Tregua: se quitan las modificaciones", "Truce: world modifiers removed"),
    },
    "blue": {
        "water_breathing": ("Respirar bajo el agua", "Water breathing"),
        "freeze": ("Congelación", "Freeze"),
        "absorption": ("Absorción IV", "Absorption IV"),
        "levitation": ("Levitación", "Levitation"),
        "rain": ("Tormenta", "Thunderstorm"),
        "fish": ("8 salmones cocinados", "8 cooked salmon"),
    },
    "purple": {
        "teleport": ("Teletransporte aleatorio", "Random teleport"),
        "totem": ("Tótem aleatorio", "Random totem"),
        "chaos_sword": ("¡Espada Caótica!", "Chaos Sword!"),
        "invisibility": ("Invisibilidad brillante", "Glowing invisibility"),
        "night": ("Noche cerrada", "Dead of night"),
        "token": ("2 fichas de ruleta", "2 roulette tokens"),
    },
}

COLOR_NAMES = {
    "red": ("Roja", "Red"), "orange": ("Naranja", "Orange"), "yellow": ("Amarilla", "Yellow"),
    "green": ("Verde", "Green"), "blue": ("Azul", "Blue"), "purple": ("Morada", "Purple"),
}

COLOR_PLAIN = {
    "red": ("Rojo", "Red"), "orange": ("Naranja", "Orange"), "yellow": ("Amarillo", "Yellow"),
    "green": ("Verde", "Green"), "blue": ("Azul", "Blue"), "purple": ("Morado", "Purple"),
}

MODIFIERS = {
    "mobs_stronger": ("Mobs más fuertes", "Stronger mobs"),
    "no_regen": ("Sin regeneración natural", "No natural regeneration"),
    "eternal_night": ("Noche eterna", "Eternal night"),
}

GUI_ES = {
    "gui.dedsafio3.title": "Menú de Dedsafio 3",
    "gui.dedsafio3.tab.general": "General",
    "gui.dedsafio3.tab.wheels": "Ruletas",
    "gui.dedsafio3.tab.world": "Mundo",
    "gui.dedsafio3.difficulty": "Dificultad del mundo",
    "gui.dedsafio3.difficulty.0": "Pacífico",
    "gui.dedsafio3.difficulty.1": "Fácil",
    "gui.dedsafio3.difficulty.2": "Normal",
    "gui.dedsafio3.difficulty.3": "Difícil",
    "gui.dedsafio3.pvp": "PvP",
    "gui.dedsafio3.keep_inventory": "Mantener inventario",
    "gui.dedsafio3.death_animation": "Animación de muerte",
    "gui.dedsafio3.totems": "Tótems activos",
    "gui.dedsafio3.cooldown": "Espera entre giros (s)",
    "gui.dedsafio3.spin": "Duración del giro (s)",
    "gui.dedsafio3.on": "Sí",
    "gui.dedsafio3.off": "No",
    "gui.dedsafio3.wheels_hint": "Peso de cada color en la rueda (0 = desactivado)",
    "gui.dedsafio3.spin_button": "Girar",
    "gui.dedsafio3.spin_random": "Girar rueda aleatoria",
    "gui.dedsafio3.random": "aleatoria",
    "gui.dedsafio3.wheel_title": "RULETA",
    "gui.dedsafio3.modifiers": "Modificaciones del mundo",
    "gui.dedsafio3.clear_modifiers": "Quitar todas las modificaciones",
    "gui.dedsafio3.death_preview": "Probar animación de muerte",
    "gui.dedsafio3.close": "Cerrar",
    "key.dedsafio3.menu": "Abrir menú de Dedsafio",
    "key.categories.dedsafio3": "Dedsafio 3",
    "message.dedsafio3.no_permission": "No tienes permiso para usar el menú.",
    "message.dedsafio3.no_colors": "Todos los colores de la rueda están desactivados.",
    "message.dedsafio3.modifier_on": "§cModificación del mundo: %s",
    "message.dedsafio3.truce": "§aTregua: se quitaron todas las modificaciones del mundo.",
}

GUI_EN = {
    "gui.dedsafio3.title": "Dedsafio 3 Menu",
    "gui.dedsafio3.tab.general": "General",
    "gui.dedsafio3.tab.wheels": "Roulettes",
    "gui.dedsafio3.tab.world": "World",
    "gui.dedsafio3.difficulty": "World difficulty",
    "gui.dedsafio3.difficulty.0": "Peaceful",
    "gui.dedsafio3.difficulty.1": "Easy",
    "gui.dedsafio3.difficulty.2": "Normal",
    "gui.dedsafio3.difficulty.3": "Hard",
    "gui.dedsafio3.pvp": "PvP",
    "gui.dedsafio3.keep_inventory": "Keep inventory",
    "gui.dedsafio3.death_animation": "Death animation",
    "gui.dedsafio3.totems": "Totems enabled",
    "gui.dedsafio3.cooldown": "Cooldown between spins (s)",
    "gui.dedsafio3.spin": "Spin duration (s)",
    "gui.dedsafio3.on": "On",
    "gui.dedsafio3.off": "Off",
    "gui.dedsafio3.wheels_hint": "Weight of each color on the wheel (0 = disabled)",
    "gui.dedsafio3.spin_button": "Spin",
    "gui.dedsafio3.spin_random": "Spin random wheel",
    "gui.dedsafio3.random": "random",
    "gui.dedsafio3.wheel_title": "ROULETTE",
    "gui.dedsafio3.modifiers": "World modifiers",
    "gui.dedsafio3.clear_modifiers": "Remove all modifiers",
    "gui.dedsafio3.death_preview": "Preview death animation",
    "gui.dedsafio3.close": "Close",
    "key.dedsafio3.menu": "Open Dedsafio menu",
    "key.categories.dedsafio3": "Dedsafio 3",
    "message.dedsafio3.no_permission": "You don't have permission to use the menu.",
    "message.dedsafio3.no_colors": "All wheel colors are disabled.",
    "message.dedsafio3.modifier_on": "§cWorld modifier: %s",
    "message.dedsafio3.truce": "§aTruce: all world modifiers were removed.",
}

LANG_BASE = {
    "es_es": {
        "itemGroup.dedsafio3": "Dedsafio 3",
        "item.dedsafio3.dedita": "Dedita",
        "item.dedsafio3.dedita.tooltip": "Moneda de la economía. Cámbiala por premios.",
        "item.dedsafio3.dedita_plata": "Dedita de Plata",
        "item.dedsafio3.dedita_plata.tooltip": "Vale 9 deditas.",
        "item.dedsafio3.dedita_oro": "Dedita de Oro",
        "item.dedsafio3.dedita_oro.tooltip": "Vale 9 deditas de plata. La más rara.",
        "item.dedsafio3.ficha_ruleta": "Ficha de Ruleta",
        "item.dedsafio3.ficha_ruleta.tooltip": "Clic derecho: gira una ruleta de color aleatorio.",
        "item.dedsafio3.totem_rojo": "Tótem Rojo",
        "item.dedsafio3.totem_rojo.tooltip": "Evita la muerte desde cualquier casilla. Regeneración, resistencia al fuego y fuerza.",
        "item.dedsafio3.totem_azul": "Tótem Azul",
        "item.dedsafio3.totem_azul.tooltip": "Evita la muerte desde cualquier casilla. Respiración acuática, gracia del delfín y resistencia.",
        "item.dedsafio3.totem_dorado": "Tótem Dorado",
        "item.dedsafio3.totem_dorado.tooltip": "Evita la muerte desde cualquier casilla. Vida completa, absorción y resistencia.",
        "item.dedsafio3.espada_caotica": "Espada Caótica",
        "item.dedsafio3.espada_caotica.tooltip": "Cada golpe aplica un efecto aleatorio al objetivo.",
        "message.dedsafio3.busy": "Ya hay una ruleta girando, espera un momento.",
        "death.dedsafio3.you": "HAS MUERTO",
        "death.dedsafio3.other": "%s ha muerto",
        "command.dedsafio3.unknown_color": "Color desconocido: %s (usa red, orange, yellow, green, blue, purple o random)",
        "command.dedsafio3.spin": "Rueda (%2$s) iniciada para %1$s jugador(es)",
        "command.dedsafio3.death": "Animación de muerte enviada (%s jugador(es))",
    },
    "en_us": {
        "itemGroup.dedsafio3": "Dedsafio 3",
        "item.dedsafio3.dedita": "Dedita",
        "item.dedsafio3.dedita.tooltip": "Economy coin. Trade it for prizes.",
        "item.dedsafio3.dedita_plata": "Silver Dedita",
        "item.dedsafio3.dedita_plata.tooltip": "Worth 9 deditas.",
        "item.dedsafio3.dedita_oro": "Gold Dedita",
        "item.dedsafio3.dedita_oro.tooltip": "Worth 9 silver deditas. The rarest one.",
        "item.dedsafio3.ficha_ruleta": "Roulette Token",
        "item.dedsafio3.ficha_ruleta.tooltip": "Right click: spins a random-colored roulette.",
        "item.dedsafio3.totem_rojo": "Red Totem",
        "item.dedsafio3.totem_rojo.tooltip": "Prevents death from any inventory slot. Regeneration, fire resistance and strength.",
        "item.dedsafio3.totem_azul": "Blue Totem",
        "item.dedsafio3.totem_azul.tooltip": "Prevents death from any inventory slot. Water breathing, dolphin's grace and resistance.",
        "item.dedsafio3.totem_dorado": "Golden Totem",
        "item.dedsafio3.totem_dorado.tooltip": "Prevents death from any inventory slot. Full health, absorption and resistance.",
        "item.dedsafio3.espada_caotica": "Chaos Sword",
        "item.dedsafio3.espada_caotica.tooltip": "Every hit applies a random effect to the target.",
        "message.dedsafio3.busy": "A roulette is already spinning, wait a moment.",
        "death.dedsafio3.you": "YOU DIED",
        "death.dedsafio3.other": "%s has died",
        "command.dedsafio3.unknown_color": "Unknown color: %s (use red, orange, yellow, green, blue, purple or random)",
        "command.dedsafio3.spin": "Wheel (%2$s) started for %1$s player(s)",
        "command.dedsafio3.death": "Death animation sent (%s player(s))",
    },
}


def build_lang():
    langs = {k: dict(v) for k, v in LANG_BASE.items()}
    for color, (es, en) in COLOR_NAMES.items():
        langs["es_es"][f"block.{MOD}.roulette_{color}"] = f"Ruleta {es}"
        langs["en_us"][f"block.{MOD}.roulette_{color}"] = f"{en} Roulette"
    langs["es_es"][f"block.{MOD}.roulette_random"] = "Ruleta de Colores"
    langs["en_us"][f"block.{MOD}.roulette_random"] = "Color Roulette"
    for color, (es, en) in COLOR_PLAIN.items():
        langs["es_es"][f"color.{MOD}.{color}"] = es
        langs["en_us"][f"color.{MOD}.{color}"] = en
    for mod, (es, en) in MODIFIERS.items():
        langs["es_es"][f"modifier.{MOD}.{mod}"] = es
        langs["en_us"][f"modifier.{MOD}.{mod}"] = en
    langs["es_es"].update(GUI_ES)
    langs["en_us"].update(GUI_EN)
    v111_lang(langs)
    for color, prizes in PRIZES.items():
        for pid, (es, en) in prizes.items():
            langs["es_es"][f"roulette.{MOD}.{color}.{pid}"] = es
            langs["en_us"][f"roulette.{MOD}.{color}.{pid}"] = en
    return langs


# ----------------------------------------------------------------------------- utilidades
def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def save_png(img, *parts):
    path = os.path.join(ASSETS, *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def shade(rgb, f):
    return tuple(max(0, min(255, int(c * f))) for c in rgb)


def px(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c + (255,) if len(c) == 3 else c)


# ----------------------------------------------------------------------------- texturas de ítems
TIERS = {
    "dedita": (245, 198, 160),
    "dedita_plata": (205, 214, 224),
    "dedita_oro": (255, 213, 79),
}


def make_dedita(base):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    outline = shade(base, 0.45)
    dark = shade(base, 0.78)
    light = shade(base, 1.12)
    # dedo apuntando arriba: x 5..10, y 3..14 con punta redondeada
    for y in range(3, 15):
        for x in range(5, 11):
            if y == 3 and x in (5, 10):
                continue
            edge = x in (5, 10) or y in (3, 14)
            px(img, x, y, outline if edge else base)
    for y in range(4, 14):
        px(img, 9, y, dark)  # sombra lateral
    for x in range(6, 10):  # uña
        for y in range(4, 7):
            px(img, x, y, (255, 235, 238) if base == TIERS["dedita"] else light)
    for x in range(6, 9):  # nudillo
        px(img, x, 9, dark)
        px(img, x, 11, dark)
    for y in range(4, 6):  # brillo
        px(img, 6, y, (255, 255, 255))
    return img


def make_token():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx = cy = 7.5
    names = list(COLORS)
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            if r > 7.4:
                continue
            if r > 6.2:
                px(img, x, y, (60, 50, 20))
            elif r < 2.0:
                px(img, x, y, (255, 213, 79))
            else:
                ang = (math.degrees(math.atan2(dy, dx)) + 360) % 360
                sector = int(ang // 60)
                px(img, x, y, COLORS[names[sector]][0])
    return img


def make_totem(base):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    outline = shade(base, 0.4)
    dark = shade(base, 0.7)
    light = shade(base, 1.2)
    # cuerpo
    for y in range(1, 15):
        for x in range(4, 12):
            edge = x in (4, 11) or y in (1, 14)
            px(img, x, y, outline if edge else base)
    for y in range(2, 14):
        px(img, 10, y, dark)
        px(img, 5, y, light)
    # alas laterales
    for y in range(5, 9):
        px(img, 3, y, outline)
        px(img, 12, y, outline)
    # cara
    px(img, 6, 5, (20, 20, 20))
    px(img, 9, 5, (20, 20, 20))
    for x in range(6, 10):
        px(img, x, 9, (20, 20, 20))
    px(img, 7, 7, shade(base, 0.55))
    px(img, 8, 7, shade(base, 0.55))
    # gema
    px(img, 7, 12, (255, 255, 255))
    px(img, 8, 12, (255, 255, 255))
    return img


def make_sword():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    blade = (170, 90, 220)
    edge = (90, 30, 140)
    # hoja diagonal de (4,11) a (13,2)
    for i in range(0, 10):
        x, y = 4 + i, 11 - i
        px(img, x, y, blade)
        px(img, x + 1, y, edge)
        px(img, x, y - 1, edge)
    for i in range(1, 9):
        px(img, 4 + i, 11 - i, (225, 190, 255))  # filo claro
    # guarda (diagonal perpendicular a la hoja)
    for i in range(-2, 3):
        px(img, 3 + i, 12 + i, (255, 213, 79))
    # empuñadura y pomo
    px(img, 2, 13, (110, 70, 40))
    px(img, 1, 14, (110, 70, 40))
    px(img, 0, 15, (255, 213, 79))
    return img


# ----------------------------------------------------------------------------- texturas de bloques
def make_roulette_top(rgb, seed):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            if r > 7.6:
                px(img, x, y, (40, 40, 48))
            elif r > 6.6:
                px(img, x, y, (212, 175, 55))
            elif r < 1.8:
                px(img, x, y, (255, 213, 79))
            elif r < 2.8:
                px(img, x, y, (30, 30, 30))
            else:
                ang = (math.degrees(math.atan2(dy, dx)) + 360) % 360
                sector = int(ang // 30)  # 12 sectores
                px(img, x, y, rgb if sector % 2 == 0 else shade(rgb, 0.35))
    return img


def make_roulette_side(rgb, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for y in range(16):
        for x in range(16):
            n = rnd.randint(-5, 5)
            base = 46 + n
            px(img, x, y, (base, base, base + 8))
    for x in range(16):
        for y in (0, 1, 14, 15):
            px(img, x, y, rgb if y in (0, 15) else shade(rgb, 0.6))
    for x in (3, 7, 11):
        px(img, x, 7, (255, 213, 79))
        px(img, x, 8, (255, 213, 79))
    return img


def make_random_top():
    """Rueda con los 6 colores de la serie."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    names = list(COLORS)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            if r > 7.6:
                px(img, x, y, (40, 40, 48))
            elif r > 6.6:
                px(img, x, y, (212, 175, 55))
            elif r < 1.8:
                px(img, x, y, (255, 213, 79))
            elif r < 2.8:
                px(img, x, y, (30, 30, 30))
            else:
                ang = (math.degrees(math.atan2(dy, dx)) + 360) % 360
                px(img, x, y, COLORS[names[int(ang // 60)]][0])
    return img


def make_random_side():
    rnd = random.Random(99)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for y in range(16):
        for x in range(16):
            n = rnd.randint(-5, 5)
            base = 46 + n
            px(img, x, y, (base, base, base + 8))
    names = list(COLORS)
    for x in range(16):
        c = COLORS[names[(x * 6) // 16]][0]
        px(img, x, 0, c)
        px(img, x, 15, c)
        px(img, x, 1, shade(c, 0.6))
        px(img, x, 14, shade(c, 0.6))
    for x in (3, 7, 11):
        px(img, x, 7, (255, 213, 79))
        px(img, x, 8, (255, 213, 79))
    return img


# ----------------------------------------------------------------------------- animación de muerte
def make_death_frame(f, total=12):
    """Calavera que aparece (frames 0-4), brilla en rojo y queda latiendo."""
    t = f / (total - 1)
    appear = min(1.0, t / 0.4)
    scale = 0.45 + 0.55 * (1 - (1 - appear) ** 3)
    alpha = min(1.0, 0.15 + appear)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    # resplandor rojo
    glow_strength = 0.25 + 0.55 * t + (0.1 if f % 2 == 0 and f > 6 else 0)
    for y in range(64):
        for x in range(64):
            d = math.hypot(x - 32, y - 30) / 30.0
            if d < 1.0:
                a = int(255 * (1 - d) ** 2 * glow_strength * 0.8)
                img.putpixel((x, y), (200, 20, 20, a))

    draw = ImageDraw.Draw(img)

    def sx(v):
        return 32 + (v - 32) * scale

    def sy(v):
        return 30 + (v - 30) * scale

    bone = (236, 232, 220, 255)
    shadow = (170, 165, 150, 255)
    black = (12, 8, 8, 255)

    # cráneo
    draw.ellipse([sx(15), sy(10), sx(49), sy(44)], fill=bone, outline=shadow)
    # mandíbula
    draw.rectangle([sx(22), sy(38), sx(42), sy(54)], fill=bone, outline=shadow)
    # dientes
    for i in range(5):
        tx = sx(24 + i * 4)
        draw.line([tx, sy(46), tx, sy(53)], fill=shadow)
    # cuencas oculares
    draw.ellipse([sx(20), sy(22), sx(30), sy(34)], fill=black)
    draw.ellipse([sx(34), sy(22), sx(44), sy(34)], fill=black)
    # brillo rojo de los ojos (crece con t)
    eye = (255, int(40 + 30 * (1 - t)), 30, 255)
    r = max(1, int(1 + 3 * t * scale))
    for ex, ey in ((25, 28), (39, 28)):
        draw.ellipse([sx(ex) - r, sy(ey) - r, sx(ex) + r, sy(ey) + r], fill=eye)
    # nariz
    draw.polygon([(sx(32), sy(34)), (sx(29), sy(41)), (sx(35), sy(41))], fill=black)

    # aplicar transparencia global de aparición
    if alpha < 1.0:
        a_chan = img.getchannel("A").point(lambda v: int(v * alpha))
        img.putalpha(a_chan)
    return img


def make_icon():
    size = 128
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    names = list(COLORS)
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            dx, dy = x - c, y - c
            r = math.hypot(dx, dy)
            if r > 62:
                continue
            if r > 56:
                img.putpixel((x, y), (212, 175, 55, 255))
            elif r < 12:
                img.putpixel((x, y), (255, 213, 79, 255))
            else:
                ang = (math.degrees(math.atan2(dy, dx)) + 360) % 360
                img.putpixel((x, y), COLORS[names[int(ang // 60)]][0] + (255,))
    return img


# ----------------------------------------------------------------------------- v1.1.1: cambios, mobs, efecto
# id -> (tipo, nombre es, descripción es, nombre en, descripción en)
CHANGES = {
    "blazes": ("difficulty", "Blazes naturales", "Ahora los Blaze aparecen naturalmente en el Nether.",
               "Natural blazes", "Blazes now spawn naturally in the Nether."),
    "creeper_nuclear": ("mob", "Creeper Nuclear", "Provoca grandes explosiones. Aparece en la Nieve.",
                        "Nuclear Creeper", "Causes huge explosions. Spawns in the snow."),
    "reptisaurio_salvaje": ("mob", "Reptisaurio Salvaje", "Ataca Cuerpo a Cuerpo. Aparece en las Cuevas del Centro de Quiu.",
                            "Savage Reptisaur", "Melee attacker. Spawns in the caves."),
    "plumosaurio": ("mob", "Plumosaurio", "Coloso pacífico, pero si lo golpeas te hará mucho daño.",
                    "Plumosaur", "Peaceful colossus, but if you hit it, it will hurt you badly."),
    "dromoraptor_rojo": ("mob", "Dromoraptor Rojo", "Depredador que posee una fuerza brutal. Se desplaza en manada.",
                         "Red Dromoraptor", "Predator with brutal strength. Moves in packs."),
    "reptisaurio_guerrero": ("mob", "Reptisaurio Guerrero",
                             "Ataca Cuerpo a Cuerpo. Aparece en los Asentamientos Reptisaurios. Cuidado: Aplica Veneno Primitivo.",
                             "Warrior Reptisaur", "Melee attacker. Spawns in Reptisaur Settlements. Beware: applies Primitive Poison."),
    "reptisaurio_arquero": ("mob", "Reptisaurio Arquero",
                            "Ataca con Arco y Flechas. Aparece en los Asentamientos Reptisaurios. Cuidado: Aplica Veneno Primitivo.",
                            "Archer Reptisaur", "Attacks with a bow and arrows. Spawns in Reptisaur Settlements. Beware: applies Primitive Poison."),
    "reptisaurio_lanza": ("mob", "Reptisaurio con Lanza",
                          "Ataca con Lanza. Aparece en los Asentamientos Reptisaurios. Cuidado: Aplica Veneno Primitivo.",
                          "Spear Reptisaur", "Attacks with a spear. Spawns in Reptisaur Settlements. Beware: applies Primitive Poison."),
    "no_honey": ("difficulty", "Sin miel", "Ya no se puede conseguir Miel. Los Panales ahora explotan al romperse.",
                 "No honey", "Honey can no longer be obtained. Honeycombs now explode when broken."),
    "magma_deadly": ("difficulty", "Magma mortal",
                     "Ahora los Bloques de Magma hacen 3.000.000.000.000.000.000.000.000.000.000.000.000.000 de daño si lo tocas en cualquiera de sus caras.",
                     "Deadly magma", "Magma Blocks now deal 3,000,000,000,000,000,000,000,000,000,000,000,000,000 damage if you touch any of their faces."),
    "zombie_climb": ("difficulty", "Zombis trepadores", "Los zombies tienen probabilidad de escalar paredes.",
                     "Climbing zombies", "Zombies have a chance to climb walls."),
    "reviil": ("moment", "Momento Reviil",
               "¡Reviil nos está observando y quiere lanzar un rayo mortal! Eón lo vio justo a tiempo y ha construido un Búnker para refugiarse de él. "
               "Antes que termine el contador TODOS deben estar dentro o morirán. Además, lanzó un conjuro para intercambiar aleatoriamente las "
               "posiciones de todos los humanos y que no puedan observar su vida.",
               "Reviil Moment",
               "Reviil is watching us and wants to launch a deadly beam! Eón saw it just in time and built a Bunker to shelter from it. "
               "Before the counter ends EVERYONE must be inside or they will die. He also cast a spell to randomly swap the positions of "
               "all humans so they cannot observe their life."),
}

TITLES = {
    "es_es": {"difficulty": "Cambio de Dificultad", "mob": "Nuevo Mob", "moment": "Momento"},
    "en_us": {"difficulty": "Difficulty Change", "mob": "New Mob", "moment": "Moment"},
}

ENTITY_NAMES = {  # id -> (es, en): coinciden con los ids de ModEntities / ReptilianEntity.Kind
    "creeper_nuclear": ("Creeper Nuclear", "Nuclear Creeper"),
    "plumosaurio": ("Plumosaurio", "Plumosaur"),
    "reptisaurio_salvaje": ("Reptisaurio Salvaje", "Savage Reptisaur"),
    "reptisaurio_guerrero": ("Reptisaurio Guerrero", "Warrior Reptisaur"),
    "reptisaurio_arquero": ("Reptisaurio Arquero", "Archer Reptisaur"),
    "reptisaurio_lanza": ("Reptisaurio con Lanza", "Spear Reptisaur"),
    "dromoraptor_rojo": ("Dromoraptor Rojo", "Red Dromoraptor"),
}

V111_GUI = {
    "es_es": {
        "gui.dedsafio3.tab.changes": "Cambios 1.1.1",
        "gui.dedsafio3.changes_hint": "Activar gira la ruleta naranja para todos · Avisar solo escribe en el chat",
        "gui.dedsafio3.notify": "Avisar",
        "gui.dedsafio3.notify_tip": "Anuncia este cambio en el chat, sin ruleta",
        "gui.dedsafio3.reviil_seconds": "Contador del Momento Reviil (s)",
        "gui.dedsafio3.page": "Página %s/%s",
        "message.dedsafio3.change_notice": "§6[%1$s] §e%2$s§f: %3$s",
        "message.dedsafio3.change_off": "§7Cambio desactivado: %s",
        "message.dedsafio3.reviil_bar": "Reviil lanzará su rayo en %s s: ¡entra al búnker!",
        "message.dedsafio3.reviil_bunker": "§c¡Momento Reviil! Eón construyó un búnker en %s, %s, %s. ¡Entren antes de que acabe el contador!",
        "message.dedsafio3.reviil_cancel": "§7El Momento Reviil fue cancelado.",
        "message.dedsafio3.reviil_strike": "§4¡El rayo de Reviil ha caído!",
        "effect.dedsafio3.veneno_primitivo": "Veneno Primitivo",
    },
    "en_us": {
        "gui.dedsafio3.tab.changes": "Changes 1.1.1",
        "gui.dedsafio3.changes_hint": "Enabling spins the orange wheel for everyone · Notify only writes in chat",
        "gui.dedsafio3.notify": "Notify",
        "gui.dedsafio3.notify_tip": "Announces this change in chat, without the wheel",
        "gui.dedsafio3.reviil_seconds": "Reviil Moment countdown (s)",
        "gui.dedsafio3.page": "Page %s/%s",
        "message.dedsafio3.change_notice": "§6[%1$s] §e%2$s§f: %3$s",
        "message.dedsafio3.change_off": "§7Change disabled: %s",
        "message.dedsafio3.reviil_bar": "Reviil will fire its beam in %s s: get inside the bunker!",
        "message.dedsafio3.reviil_bunker": "§cReviil Moment! Eón built a bunker at %s, %s, %s. Get inside before the counter ends!",
        "message.dedsafio3.reviil_cancel": "§7The Reviil Moment was cancelled.",
        "message.dedsafio3.reviil_strike": "§4Reviil's beam has struck!",
        "effect.dedsafio3.veneno_primitivo": "Primitive Poison",
    },
}


def v111_lang(langs):
    for i, code in enumerate(("es_es", "en_us")):
        entries = langs[code]
        entries.update(V111_GUI[code])
        for kind, title in TITLES[code].items():
            entries[f"change.{MOD}.title.{kind}"] = title
        for cid, data in CHANGES.items():
            kind = data[0]
            name, desc = data[1 + 2 * i], data[2 + 2 * i]
            entries[f"change.{MOD}.{cid}.name"] = name
            entries[f"change.{MOD}.{cid}.desc"] = desc
            if kind == "mob":
                entries[f"change.{MOD}.{cid}.wheel"] = f"{TITLES[code]['mob']} - {name}: {desc}"
            elif kind == "moment":
                entries[f"change.{MOD}.{cid}.wheel"] = f"{name}: {desc}"
            else:
                entries[f"change.{MOD}.{cid}.wheel"] = f"{TITLES[code]['difficulty']}: {desc}"
        for eid, names in ENTITY_NAMES.items():
            entries[f"entity.{MOD}.{eid}"] = names[i]
            entries[f"item.{MOD}.spawn_{eid}"] = ("Generar " if i == 0 else "Spawn ") + names[i]
    return langs


def skin(size, base, var, seed):
    """Textura base con ruido; sirve para cualquier modelo porque no depende del mapa UV."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", size, (0, 0, 0, 255))
    for y in range(size[1]):
        for x in range(size[0]):
            n = rnd.randint(-var, var)
            img.putpixel((x, y), tuple(max(0, min(255, c + n)) for c in base) + (255,))
    return img


def fill(img, x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            px(img, x, y, color)


def make_biped(base, var, seed, chest, paint, eye):
    """Modelo de zombi (64x64): frente de la cabeza en (8,8)-(16,16), pecho en (20,20)-(28,32)."""
    img = skin((64, 64), base, var, seed)
    fill(img, 20, 20, 28, 32, chest)                 # pecho
    fill(img, 8, 8, 16, 16, shade(base, 0.85))       # cara
    for x in (9, 10, 13, 14):
        px(img, x, 11, eye)
    fill(img, 10, 14, 14, 15, (25, 18, 14))          # boca
    if paint:
        for x in range(8, 16, 2):
            px(img, x, 9, paint)
        fill(img, 8, 12, 16, 13, paint)
    return img


def make_nuclear_creeper():
    """Creeper (64x32) amarillo con franjas negras de aviso y cara de creeper."""
    img = skin((64, 32), (232, 210, 26), 10, 71)
    for y in range(32):
        for x in range(64):
            if (x + y) % 10 < 3:
                px(img, x, y, (30, 30, 24))
    fill(img, 8, 8, 16, 16, (232, 210, 26))
    for ex in (9, 13):
        fill(img, ex, 10, ex + 2, 12, (20, 20, 20))
    fill(img, 11, 12, 13, 15, (20, 20, 20))
    fill(img, 10, 14, 11, 16, (20, 20, 20))
    fill(img, 13, 14, 14, 16, (20, 20, 20))
    return img


def make_plumosaurio():
    """Golem (128x128) morado con 'plumas' claras; cara en la parte frontal de la cabeza (8,8)-(16,18)."""
    img = skin((128, 128), (123, 63, 160), 14, 33)
    rnd = random.Random(34)
    for _ in range(900):
        x, y = rnd.randrange(128), rnd.randrange(128)
        px(img, x, y, (201, 160, 220))
    fill(img, 8, 8, 16, 18, (140, 80, 175))
    for x in (9, 10, 13, 14):
        px(img, x, 11, (255, 220, 60))
    fill(img, 11, 14, 13, 16, (40, 20, 50))
    return img


def make_poison_icon():
    """Icono de efecto 18x18: gota verde."""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    cx, cy = 8.5, 10.5
    for y in range(18):
        for x in range(18):
            r = math.hypot(x - cx, (y - cy) * 0.95)
            tip = (y < 10 and abs(x - cx) < (y - 1) * 0.55 and y > 1)
            if r < 6.2 or tip:
                edge = r > 5.0 or (tip and abs(x - cx) > (y - 1) * 0.55 - 1.2)
                px(img, x, y, (30, 80, 20) if edge else (110, 190, 60))
    for (x, y) in ((6, 10), (6, 11), (7, 9)):
        px(img, x, y, (220, 255, 190))
    return img


# ----------------------------------------------------------------------------- JSON
def item_model(name, parent="minecraft:item/generated"):
    return {"parent": parent, "textures": {"layer0": f"{MOD}:item/{name}"}}


def recipe_shapeless(ingredients, result, count=1):
    return {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [{"item": i} for i in ingredients],
        "result": {"id": result, "count": count},
    }


def recipe_shaped(pattern, key, result, count=1):
    return {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": pattern,
        "key": {k: {"item": v} for k, v in key.items()},
        "result": {"id": result, "count": count},
    }


def main():
    # --- texturas de ítems
    for name, base in TIERS.items():
        save_png(make_dedita(base), "textures", "item", f"{name}.png")
    save_png(make_token(), "textures", "item", "ficha_ruleta.png")
    save_png(make_totem((220, 60, 60)), "textures", "item", "totem_rojo.png")
    save_png(make_totem((60, 120, 230)), "textures", "item", "totem_azul.png")
    save_png(make_totem((255, 200, 60)), "textures", "item", "totem_dorado.png")
    save_png(make_sword(), "textures", "item", "espada_caotica.png")

    # --- texturas de bloques, modelos, blockstates, loot
    for idx, (color, (rgb, _dye)) in enumerate(COLORS.items()):
        save_png(make_roulette_top(rgb, idx), "textures", "block", f"roulette_{color}_top.png")
        save_png(make_roulette_side(rgb, idx), "textures", "block", f"roulette_{color}_side.png")
        write_json(os.path.join(ASSETS, "models", "block", f"roulette_{color}.json"), {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {
                "top": f"{MOD}:block/roulette_{color}_top",
                "side": f"{MOD}:block/roulette_{color}_side",
                "bottom": f"{MOD}:block/roulette_{color}_side",
            },
        })
        write_json(os.path.join(ASSETS, "models", "item", f"roulette_{color}.json"),
                   {"parent": f"{MOD}:block/roulette_{color}"})
        write_json(os.path.join(ASSETS, "blockstates", f"roulette_{color}.json"),
                   {"variants": {"": {"model": f"{MOD}:block/roulette_{color}"}}})
        write_json(os.path.join(DATA, "loot_table", "blocks", f"roulette_{color}.json"), {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1.0,
                "bonus_rolls": 0.0,
                "entries": [{"type": "minecraft:item", "name": f"{MOD}:roulette_{color}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
        })

    # --- ruleta aleatoria (la rueda decide el color)
    save_png(make_random_top(), "textures", "block", "roulette_random_top.png")
    save_png(make_random_side(), "textures", "block", "roulette_random_side.png")
    write_json(os.path.join(ASSETS, "models", "block", "roulette_random.json"), {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {
            "top": f"{MOD}:block/roulette_random_top",
            "side": f"{MOD}:block/roulette_random_side",
            "bottom": f"{MOD}:block/roulette_random_side",
        },
    })
    write_json(os.path.join(ASSETS, "models", "item", "roulette_random.json"),
               {"parent": f"{MOD}:block/roulette_random"})
    write_json(os.path.join(ASSETS, "blockstates", "roulette_random.json"),
               {"variants": {"": {"model": f"{MOD}:block/roulette_random"}}})
    write_json(os.path.join(DATA, "loot_table", "blocks", "roulette_random.json"), {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0,
            "bonus_rolls": 0.0,
            "entries": [{"type": "minecraft:item", "name": f"{MOD}:roulette_random"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })

    # --- modelos de ítems
    for name in ITEMS:
        parent = "minecraft:item/handheld" if name == "espada_caotica" else "minecraft:item/generated"
        write_json(os.path.join(ASSETS, "models", "item", f"{name}.json"), item_model(name, parent))

    # --- animación de muerte (12 fotogramas) e icono
    for f in range(12):
        save_png(make_death_frame(f), "textures", "gui", "death", f"frame_{f:02d}.png")
    save_png(make_icon(), "icon.png")

    # --- tags
    write_json(os.path.join(ROOT, "data", "minecraft", "tags", "block", "mineable", "pickaxe.json"), {
        "replace": False,
        "values": [f"{MOD}:roulette_{c}" for c in list(COLORS) + ["random"]],
    })

    # --- recetas
    R = os.path.join(DATA, "recipe")
    d, dp, do = f"{MOD}:dedita", f"{MOD}:dedita_plata", f"{MOD}:dedita_oro"
    write_json(os.path.join(R, "dedita.json"),
               recipe_shapeless(["minecraft:gold_nugget", "minecraft:gold_nugget", "minecraft:pink_dye"], d, 2))
    write_json(os.path.join(R, "dedita_plata.json"), recipe_shapeless([d] * 9, dp))
    write_json(os.path.join(R, "dedita_from_plata.json"), recipe_shapeless([dp], d, 9))
    write_json(os.path.join(R, "dedita_oro.json"), recipe_shapeless([dp] * 9, do))
    write_json(os.path.join(R, "dedita_plata_from_oro.json"), recipe_shapeless([do], dp, 9))
    write_json(os.path.join(R, "ficha_ruleta.json"), recipe_shapeless([d, "minecraft:paper"], f"{MOD}:ficha_ruleta"))
    write_json(os.path.join(R, "totem_rojo.json"),
               recipe_shapeless(["minecraft:totem_of_undying", dp, "minecraft:red_dye"], f"{MOD}:totem_rojo"))
    write_json(os.path.join(R, "totem_azul.json"),
               recipe_shapeless(["minecraft:totem_of_undying", dp, "minecraft:blue_dye"], f"{MOD}:totem_azul"))
    write_json(os.path.join(R, "totem_dorado.json"),
               recipe_shapeless(["minecraft:totem_of_undying", do, "minecraft:golden_apple"], f"{MOD}:totem_dorado"))
    write_json(os.path.join(R, "espada_caotica.json"),
               recipe_shapeless(["minecraft:diamond_sword", do, do, "minecraft:ender_pearl"], f"{MOD}:espada_caotica"))
    write_json(os.path.join(R, "roulette_random.json"), recipe_shaped(
        ["III", "GDG", "III"],
        {"I": "minecraft:iron_ingot", "G": "minecraft:gold_ingot", "D": "minecraft:amethyst_shard"},
        f"{MOD}:roulette_random"))
    for color, (_rgb, dye) in COLORS.items():
        write_json(os.path.join(R, f"roulette_{color}.json"), recipe_shaped(
            ["III", "GDG", "III"],
            {"I": "minecraft:iron_ingot", "G": "minecraft:gold_ingot", "D": dye},
            f"{MOD}:roulette_{color}"))

    # --- v1.1.1: texturas de mobs, icono del efecto, modelos de huevos
    save_png(make_nuclear_creeper(), "textures", "entity", "creeper_nuclear.png")
    save_png(make_plumosaurio(), "textures", "entity", "plumosaurio.png")
    save_png(make_biped((60, 85, 45), 12, 41, (70, 55, 35), None, (255, 150, 30)), "textures", "entity", "reptisaurio_salvaje.png")
    save_png(make_biped((70, 112, 50), 12, 42, (110, 75, 40), (180, 40, 30), (255, 220, 60)), "textures", "entity", "reptisaurio_guerrero.png")
    save_png(make_biped((50, 120, 100), 12, 43, (200, 190, 140), None, (255, 240, 120)), "textures", "entity", "reptisaurio_arquero.png")
    save_png(make_biped((110, 122, 50), 12, 44, (90, 90, 60), (200, 160, 40), (255, 80, 40)), "textures", "entity", "reptisaurio_lanza.png")
    save_png(make_biped((172, 52, 36), 16, 45, (120, 28, 20), (240, 150, 60), (255, 240, 80)), "textures", "entity", "dromoraptor_rojo.png")
    save_png(make_poison_icon(), "textures", "mob_effect", "veneno_primitivo.png")
    for eid in ENTITY_NAMES:
        write_json(os.path.join(ASSETS, "models", "item", f"spawn_{eid}.json"), {"parent": "minecraft:item/template_spawn_egg"})

    # --- idiomas
    for code, entries in build_lang().items():
        write_json(os.path.join(ASSETS, "lang", f"{code}.json"), entries)

    print("Recursos generados en", os.path.normpath(ROOT))


if __name__ == "__main__":
    main()
