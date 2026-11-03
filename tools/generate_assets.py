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
         "totem_rojo", "totem_azul", "totem_dorado", "espada_caotica", "cuchara_resurreccion",
         "fruta_espacio", "indicador_insomnia", "tenedor_resurreccion",
         "nave_espacial", "fruta_quiu", "bolsa_primitiva", "bolsa_glebanoide"]

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
        "gui.dedsafio3.reviil_intro": "MOMENTO REVIIL",
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
        "gui.dedsafio3.reviil_intro": "REVIIL MOMENT",
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
    "reviil": ("moment", "Momento Reviil: Rayo",
               "¡Reviil nos está observando y quiere lanzar un rayo mortal! Eón lo vio justo a tiempo y ha construido un Búnker para refugiarse de él. "
               "Antes que termine el contador TODOS deben estar dentro o morirán. Además, lanzó un conjuro para intercambiar aleatoriamente las "
               "posiciones de todos los humanos y que no puedan observar su vida.",
               "Reviil Moment: Beam",
               "Reviil is watching us and wants to launch a deadly beam! Eón saw it just in time and built a Bunker to shelter from it. "
               "Before the counter ends EVERYONE must be inside or they will die. He also cast a spell to randomly swap the positions of "
               "all humans so they cannot observe their life."),
    # --- v1.2.0 (Dedsafio 4, jueves 24.SEP.26)
    "dactilo_bebe": ("mob", "Dáctylo Bebé",
                     "Criatura que surca los cielos del Centro de Quiu. Dispara proyectiles que taparán tu visión durante algunos segundos.",
                     "Baby Dactyl",
                     "Creature that soars the skies of the Quiu Center. Shoots projectiles that will cover your vision for a few seconds."),
    "creeper_magma": ("mob", "Creeper de Magma", "Al explotar, pone Lava en su posición.",
                      "Magma Creeper", "When it explodes, it leaves Lava where it stood."),
    "dromoraptor_azul": ("mob", "Dromoraptor Azul",
                         "Se ha visto una nueva criatura en el Centro de Quiu: Dromoraptor Azul. Depredador extremadamente veloz que se desplaza en manada. "
                         "Aparece en la Sabana del Centro de Quiu.",
                         "Blue Dromoraptor",
                         "A new creature has been seen in the Quiu Center: Blue Dromoraptor. An extremely fast predator that moves in packs. "
                         "Spawns in the Savanna of the Quiu Center."),
    "zombie_elite": ("difficulty", "Zombis de élite",
                     "Ahora TODOS los Zombies pueden escalar y spawnean con una Espada de Hierro con Filo V.",
                     "Elite zombies", "Now ALL Zombies can climb and spawn with an Iron Sword with Sharpness V."),
    "herobrine": ("difficulty", "Herobrine", "Herobrine fue añadido al servidor.",
                  "Herobrine", "Herobrine was added to the server."),
    "narval": ("difficulty", "Narvales en el Overworld",
               "¡Los Narvales han conseguido salir del Centro de Quiu! Ahora los Narvales aparecen en los Océanos del Overworld.",
               "Narwhals in the Overworld",
               "The Narwhals have managed to leave the Quiu Center! Narwhals now spawn in the Overworld Oceans."),
    "cobweb_spider": ("difficulty", "Arañas en la telaraña",
                      "Al romper una Tela de Araña existe la probabilidad de que salga una Araña de Cueva.",
                      "Spiders in the web", "Breaking a Cobweb has a chance of releasing a Cave Spider."),
    "reviil_luna": ("moment", "Momento Reviil: Maldición lunar",
                    "¡Vaya horrible noche para tener una maldición! El primer jugador en mirar a la Luna recibirá daño instantáneo y morirá. "
                    "Este cambio permanecerá activo hasta que alguien muera por esa razón. Luego será desactivado.",
                    "Reviil Moment: Moon Curse",
                    "What a horrible night to have a curse! The first player to look at the Moon will take instant damage and die. "
                    "This change stays active until someone dies for that reason. Then it is deactivated."),
    "reviil_meteoros": ("moment", "Momento Reviil: Meteoritos",
                        "¡Reviil ha invocado una lluvia de meteoritos y ha DESACTIVADO el Nether! Eón ha creado un refugio para resistir. "
                        "Deben llegar antes que termine el tiempo… o morirán.",
                        "Reviil Moment: Meteors",
                        "Reviil has summoned a meteor shower and DISABLED the Nether! Eón has built a shelter to hold out. "
                        "You must get there before time runs out… or you will die."),
    # --- v1.3.0 (Dedsafio 4, lunes 21.SEP.26)
    "no_saplings": ("difficulty", "Sin brotes", "Ahora no caen Brotes de Árboles. Replantar ya no es una opción.",
                    "No saplings", "Tree Saplings no longer drop. Replanting is no longer an option."),
    "warden_natural": ("difficulty", "Warden natural", "Ahora los Warden aparecen naturalmente en DeepDark.",
                       "Natural Wardens", "Wardens now spawn naturally in the Deep Dark."),
    "bomba_warden": ("mob", "Bomba Warden", "Las Bombas Warden aparecen en Deepdark.",
                     "Warden Bomb", "Warden Bombs spawn in the Deep Dark."),
    "no_milk": ("difficulty", "Sin leche", "No se le puede sacar leche a las vacas.",
                "No milk", "Cows can no longer be milked."),
    "doors_no_air": ("difficulty", "Sin aire en puertas",
                     "Las puertas y trampillas no pueden utilizarse para respirar bajo el agua.",
                     "No air in doors", "Doors and trapdoors can no longer be used to breathe underwater."),
    "pod_phora": ("mob", "Pod Phora", "Nave que dispara Proyectiles Láser. Aparece en los Cubos Phora.",
                  "Phora Pod", "Ship that fires Laser Projectiles. Spawns in the Phora Cubes."),
    "skiff_phora": ("mob", "Skiff Phora", "Nave que ataca Cuerpo a Cuerpo. Aparece en los Cubos Phora.",
                    "Phora Skiff", "Ship that attacks in melee. Spawns in the Phora Cubes."),
    "warden_shrieker": ("difficulty", "Warden con Shrieker", "Ahora los Warden generan un Sculk Shrieker al morir.",
                        "Warden Shrieker", "Wardens now generate a Sculk Shrieker when they die."),
    "pearl_pierce": ("difficulty", "Perlas perforantes", "Ahora las Ender Pearls ignoran la Protección de la Armadura.",
                     "Piercing pearls", "Ender Pearls now ignore Armor Protection."),
    "creeper_electric": ("difficulty", "Creepers eléctricos",
                         "Ahora aparecen Creepers Eléctricos en el Overworld en lugar de los comunes.",
                         "Electric creepers", "Electric Creepers now spawn in the Overworld instead of common ones."),
    "reviil_gas_alto": ("moment", "Momento Reviil: Gas (arriba)",
                        "¡Reviil ha liberado un gas tóxico invisible en todas las dimensiones! Antes que termine el temporizador TODOS deben estar "
                        "POR ENCIMA de la Capa 250, o recibirán daño hasta morir.",
                        "Reviil Moment: Gas (high)",
                        "Reviil has released an invisible toxic gas in all dimensions! Before the timer ends EVERYONE must be ABOVE Layer 250, "
                        "or they will take damage until they die."),
    "reviil_gas_bajo": ("moment", "Momento Reviil: Gas (abajo)",
                        "¡Reviil ha liberado un gas tóxico invisible! Antes que termine el temporizador TODOS deben DESCENDER a una Capa Negativa "
                        "(desde la -1 hacia abajo), o recibirán daño hasta morir.",
                        "Reviil Moment: Gas (low)",
                        "Reviil has released an invisible toxic gas! Before the timer ends EVERYONE must DESCEND to a Negative Layer "
                        "(from -1 downward), or they will take damage until they die."),
    # --- v1.4.0 (Dedsafio 4, miércoles 23.SEP.26)
    "resurrection_spoon": ("difficulty", "Cuchara de la Resurrección",
                           "Ahora las Cucharas de la Resurrección deben ser utilizadas en un Ritual el mismo día en el que son reclamadas. "
                           "Cada jugador tiene un alma: en una Fogata del Ritual, la Cuchara y tu alma se gastan para revivir a un jugador muerto.",
                           "Resurrection Spoon",
                           "Resurrection Spoons must now be used in a Ritual on the same day they are claimed. "
                           "Each player has one soul: at a Ritual Campfire, the Spoon and your soul are spent to revive a dead player."),
    "creeper_pasto": ("mob", "Creeper de Pasto",
                      "Pequeña variante de los Creepers que explota rápidamente y es muy veloz. Tiene probabilidad de salir cuando se rompen "
                      "Bloques de Pasto, Pasto Corto, Pasto Alto, Arbustos, entre otras hierbas.",
                      "Grass Creeper",
                      "A small Creeper variant that explodes quickly and is very fast. It has a chance to appear when Grass Blocks, "
                      "Short Grass, Tall Grass, Bushes and other plants are broken."),
    "narval_acuifero": ("mob", "Narval",
                        "Habita los acuíferos del Centro de Quiu. Si detecta intrusos embestirá a gran velocidad, con la probabilidad de "
                        "empujarte y jugar contigo.",
                        "Narwhal",
                        "Lives in the aquifers of the Quiu Center. If it detects intruders it will charge at great speed, with a chance "
                        "to push you and play with you."),
    "pearl_10": ("difficulty", "Perlas de 10 de daño", "Ahora las Ender Pearls hacen 10 de daño (5 corazones).",
                 "10-damage pearls", "Ender Pearls now deal 10 damage (5 hearts)."),
    "reviil_creepers": ("moment", "Momento Reviil: Creepers eléctricos",
                        "¡Reviil lanzó una lluvia de Creepers Eléctricos! De manera aleatoria aparecerán Creepers Eléctricos con 1 de Vida en tu "
                        "posición. Además, Reviil intercambió las posiciones de los humanos y lanzó un conjuro para que los humanos no puedan "
                        "ver su vida.",
                        "Reviil Moment: Electric Creepers",
                        "Reviil launched a rain of Electric Creepers! Electric Creepers with 1 Health will randomly appear at your position. "
                        "Reviil also swapped the humans' positions and cast a spell so humans cannot see their health."),
    # --- v1.5.0 (textos interpretados a partir de las capturas del Dedsafio 4)
    "wither_skeleton_natural": ("difficulty", "Wither Skeletons naturales",
                                "Ahora los Wither Skeletons aparecen naturalmente en el Nether, no solo en las fortalezas.",
                                "Natural Wither Skeletons",
                                "Wither Skeletons now spawn naturally in the Nether, not only in fortresses."),
    "pesadillas": ("difficulty", "Pesadillas",
                   "Si pasas días sin dormir sufrirás Insomnia (bajo, medio o alto) y tendrás pesadillas cada vez peores. "
                   "Un Indicador de Insomnia te muestra tu nivel. Dormir lo cura.",
                   "Nightmares",
                   "If you go days without sleeping you will suffer Insomnia (low, medium or high) and increasingly worse nightmares. "
                   "An Insomnia Indicator shows your level. Sleeping cures it."),
    "dactilo_adulto": ("mob", "Dáctylo Adulto",
                       "Amo de los cielos. Con Insomnia alta aparece y te atrapa, te eleva por los aires y te suelta desde lo alto. "
                       "La Fruta del Espacio entre Dimensiones te teletransporta lejos de él; para que desaparezca, duerme.",
                       "Adult Dactyl",
                       "Lord of the skies. With high Insomnia it appears, grabs you, lifts you into the air and drops you from above. "
                       "The Interdimensional Space Fruit teleports you away from it; to make it vanish, sleep."),
    "creeper_wither_rose": ("mob", "Creeper de Wither Rose",
                            "Aparece en el Nether. Al explotar aplica Wither a todos los cercanos y deja una Wither Rose.",
                            "Wither Rose Creeper",
                            "Spawns in the Nether. When it explodes it applies Wither to everyone nearby and leaves a Wither Rose."),
    "dromoraptor_verde": ("mob", "Dromoraptor Verde",
                          "Depredador de la selva. Su ataque aplica Veneno II durante 2 segundos. Se desplaza en manada.",
                          "Green Dromoraptor",
                          "Jungle predator. Its attack applies Poison II for 2 seconds. Moves in packs."),
    "dromoraptor_cherry": ("difficulty", "Dromoraptores en el bioma Cherry",
                           "Los Dromoraptores que ataquen en un bioma Cherry hacen 7 de daño extra.",
                           "Dromoraptors in the Cherry biome",
                           "Dromoraptors attacking in a Cherry biome deal 7 extra damage."),
    "animals_extinct": ("difficulty", "Animales extintos",
                        "Un gas misterioso extingue a los animales: todos los animales cercanos a los jugadores mueren (las mascotas se salvan).",
                        "Extinct animals",
                        "A mysterious gas wipes out animals: every animal near players dies (pets are spared)."),
    "bogged_night": ("difficulty", "Bogged de noche",
                     "Ahora los esqueletos Bogged aparecen por la noche en cualquier lugar del mundo.",
                     "Bogged at night",
                     "Bogged skeletons now spawn at night anywhere in the world."),
    "stray_night": ("difficulty", "Stray de noche",
                    "Ahora los esqueletos Stray aparecen por la noche en cualquier lugar del mundo.",
                    "Stray at night",
                    "Stray skeletons now spawn at night anywhere in the world."),
    "creeper_fluido": ("mob", "Creeper de Fluido", "Se ha visto una nueva criatura en Gleba, el Planeta Orgánico: Creeper de Fluido. Al explotar, introduce fluidos en tu computadora, provocando que tus componentes fallen y te desconecten de la realidad. Puede escalar.",
          "Fluid Creeper", "A new creature has been seen on Gleba, the Organic Planet: Fluid Creeper. When it explodes it pours fluids into your computer, making your components fail and disconnecting you from reality. Can climb."),
    "nautilus_oseo": ("mob", "Nautilus Óseo Pequeño", "Se ha visto una nueva criatura en Gleba, el Planeta Orgánico: Nautilus Óseo Pequeño. Criatura voladora que atacará si te acercas. No puedes empujarla. Aplica Levitación. Son MUY peligrosos en grupo.",
          "Small Bone Nautilus", "A new creature has been seen on Gleba, the Organic Planet: Small Bone Nautilus. Flying creature that attacks if you get close. You cannot push it. Applies Levitation. VERY dangerous in groups."),
    "garrapata_cerebral": ("mob", "Garrapata Cerebral Pequeña", "Se ha visto una nueva criatura en Gleba, el Planeta Orgánico: Garrapata Cerebral Pequeña. Vaga por el planeta en busca de su próxima víctima. Ataca de inmediato si detecta una presencia desconocida. Puede escalar.",
          "Small Brain Tick", "A new creature has been seen on Gleba, the Organic Planet: Small Brain Tick. Wanders the planet looking for its next victim. Attacks immediately if it detects an unknown presence. Can climb."),
    "gusano_carne": ("mob", "Gusano de Carne", "Se ha visto una nueva criatura en Gleba, el Planeta Orgánico: Gusano de Carne. Criatura pequeña y lenta. Ataca directamente a quien se le acerque. Probabilidad de aparecer al romper los bloques de Grasa Líquida o Coagulada (aquí: barro).",
          "Meat Worm", "A new creature has been seen on Gleba, the Organic Planet: Meat Worm. Small and slow creature. Attacks anyone who gets close. Chance to appear when breaking Liquid or Coagulated Fat blocks (here: mud)."),
    "parasito": ("mob", "Parásito", "Se ha visto una nueva criatura en Gleba, el Planeta Orgánico: Parásito. Criatura que habita en los Jugos Gástricos de Gleba. Ataca cuerpo a cuerpo sin pensarlo.",
          "Parasite", "A new creature has been seen on Gleba, the Organic Planet: Parasite. Creature that lives in Gleba's Gastric Juices. Attacks in melee without thinking."),
    "cerebro_amarillo": ("mob", "Cerebro Amarillo", "Se ha visto una nueva criatura en Gleba, el Planeta Orgánico: Cerebro Amarillo. Se abalanza sobre tu cabeza, cegándote y drenando tu vida hasta matarte. Necesitas ayuda de otra persona para quitártelo.",
          "Yellow Brain", "A new creature has been seen on Gleba, the Organic Planet: Yellow Brain. It leaps onto your head, blinding you and draining your life until you die. You need another person's help to remove it."),
    "parasito_volador": ("mob", "Parásito Volador", "Se ha visto una nueva criatura en Gleba, el Planeta Orgánico: Parásito Volador. Habita dentro de las Verrugas Claras y Oscuras de Gleba. Al romperlas, saldrá y te atacará. Es muy veloz.",
          "Flying Parasite", "A new creature has been seen on Gleba, the Organic Planet: Flying Parasite. Lives inside Gleba's Light and Dark Warts. When you break them, it comes out and attacks you. Very fast."),
    "flashbang": ("mob", "Flashbang", "Se ha visto una nueva criatura enviada por Reviil: Flashbang. Corre velozmente hacia ti. Al explotar, cubre tu pantalla de blanco por algunos segundos. Sólo aparece de noche en el Overworld.",
          "Flashbang", "A new creature sent by Reviil has been seen: Flashbang. It runs fast toward you. When it explodes it covers your screen in white for a few seconds. Only appears at night in the Overworld."),
    "creeper_nuclear_nether": ("difficulty", "Creepers Nucleares en el Techo del Nether", "Los Creepers Nucleares ahora aparecen en el Techo del Nether. Además, pueden ver a través de las paredes y romper bloques para ir hacia tí.",
          "Nuclear Creepers on the Nether Roof", "Nuclear Creepers now spawn on the Nether Roof. They can also see through walls and break blocks to reach you."),
    "dactilo_pantano": ("difficulty", "Dáctylo Bebé en el Pantano", "El Dáctylo Bebé ahora aparece naturalmente en el bioma Pantano (Overworld).",
          "Baby Dactyl in the Swamp", "The Baby Dactyl now spawns naturally in the Swamp biome (Overworld)."),
    "reaper_deepdark": ("difficulty", "Reapers Pequeños en Deep Dark", "Los Reapers Pequeños aparecen naturalmente en Deep Dark.",
          "Small Reapers in the Deep Dark", "Small Reapers spawn naturally in the Deep Dark."),
    "flashbang_limbo": ("difficulty", "Flashbangs en el Limbo", "Ahora los Flashbangs aparecen en el Laberinto del Limbo. Además se ha duplicado su rango de explosión.",
          "Flashbangs in the Limbo", "Flashbangs now spawn in the Limbo Labyrinth. Their explosion range has also been doubled."),
    "spider_strength": ("difficulty", "Arañas con Fuerza III", "Ahora las Arañas y Arañas de Cueva tienen Fuerza III.",
          "Strength III spiders", "Spiders and Cave Spiders now have Strength III."),
    "fall_damage_x2": ("difficulty", "Daño de caída x2", "El Daño de Caída ha sido multiplicado x2.",
          "Fall damage x2", "Fall Damage has been multiplied x2."),
    "parasito_volador_explota": ("difficulty", "Parásitos Voladores explosivos", "Ahora los Parásitos Voladores explotan al morir.",
          "Exploding Flying Parasites", "Flying Parasites now explode when they die."),
    "spider_night_speed": ("difficulty", "Arañas de noche y veloces", "Ahora las Arañas de Cueva aparecen en el Overworld durante la Noche. Además, todas las arañas tienen Velocidad I infinita.",
          "Fast night spiders", "Cave Spiders now spawn in the Overworld at Night. Also, all spiders have infinite Speed I."),
    "cerebro_amarillo_planeta": ("difficulty", "Cerebros Amarillos por todo Gleba", "Los Cerebros Amarillos consiguieron salir de las Cuevas de Gleba y ahora aparecen por todo el Planeta.",
          "Yellow Brains across Gleba", "Yellow Brains escaped Gleba's Caves and now spawn across the whole Planet."),
    "reaper_pequeno": ("mob", "Reaper Pequeño", "Se ha visto una nueva criatura salida del Limbo: Reaper Pequeño. Al golpear a su objetivo con una Hoz, desterrará a su víctima directamente a un Laberinto en el Limbo del que deberás escapar cuanto antes y encontrar la salida.",
          "Small Reaper", "A new creature from the Limbo has been seen: Small Reaper. When it hits its target with a Scythe, it banishes the victim straight into a Labyrinth in the Limbo that you must escape as soon as possible by finding the exit."),
    "medusa_osea": ("mob", "Médusa Ósea", "Se ha visto una nueva criatura en Gleba, el Planeta Orgánico: Médusa Ósea. Una criatura con capacidades únicas. Si elimina a un jugador, se convertirá en él y adoptará su inventario. Será EXTREMADAMENTE inteligente, contará con patrones de ataque y estrategias de pelea utilizando todos sus objetos y recursos.",
          "Bone Medusa", "A new creature has been seen on Gleba, the Organic Planet: Bone Medusa. A creature with unique abilities. If it kills a player, it becomes them and adopts their inventory. It will be EXTREMELY smart, with attack patterns and fighting strategies using all its items and resources."),
    "cerebro_rojo": ("mob", "Cerebro Rojo", "Se ha visto una nueva criatura en Gleba, el Planeta Orgánico: Cerebro Rojo. Se abalanza sobre tu cabeza, cegándote y drenando tu vida hasta matarte. Necesitas ayuda de otra persona para quitártelo. IMPORTANTE: Si estás bajo el control mental del cerebro, comenzarás a atacar a tus compañeros cercanos. Incluso podrías llegar a matarlos.",
          "Red Brain", "A new creature has been seen on Gleba, the Organic Planet: Red Brain. It leaps onto your head, blinding you and draining your life until you die. You need another person's help to remove it. IMPORTANT: If you are under the brain's mind control, you will start attacking nearby teammates. You could even kill them."),
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
    "dromoraptor_azul": ("Dromoraptor Azul", "Blue Dromoraptor"),
    "creeper_magma": ("Creeper de Magma", "Magma Creeper"),
    "dactilo_bebe": ("Dáctylo Bebé", "Baby Dactyl"),
    "narval": ("Narval", "Narwhal"),
    "herobrine": ("Herobrine", "Herobrine"),
    "bomba_warden": ("Bomba Warden", "Warden Bomb"),
    "pod_phora": ("Pod Phora", "Phora Pod"),
    "skiff_phora": ("Skiff Phora", "Phora Skiff"),
    "creeper_pasto": ("Creeper de Pasto", "Grass Creeper"),
    "creeper_wither_rose": ("Creeper de Wither Rose", "Wither Rose Creeper"),
    "dactilo_adulto": ("Dáctylo Adulto", "Adult Dactyl"),
    "dromoraptor_verde": ("Dromoraptor Verde", "Green Dromoraptor"),
    "nave_espacial": ("Nave Espacial", "Spaceship"),
    "reaper_pequeno": ("Reaper Pequeño", "Small Reaper"),
    "medusa_osea": ("Médusa Ósea", "Bone Medusa"),
    "cerebro_rojo": ("Cerebro Rojo", "Red Brain"),
    "creeper_fluido": ("Creeper de Fluido", "Fluid Creeper"),
    "nautilus_oseo": ("Nautilus Óseo Pequeño", "Small Bone Nautilus"),
    "garrapata_cerebral": ("Garrapata Cerebral Pequeña", "Small Brain Tick"),
    "gusano_carne": ("Gusano de Carne", "Meat Worm"),
    "parasito": ("Parásito", "Parasite"),
    "cerebro_amarillo": ("Cerebro Amarillo", "Yellow Brain"),
    "parasito_volador": ("Parásito Volador", "Flying Parasite"),
    "flashbang": ("Flashbang", "Flashbang"),
}

V111_GUI = {
    "es_es": {
        "item.dedsafio3.bolsa_primitiva": "Bolsa Primitiva",
        "item.dedsafio3.bolsa_primitiva.tooltip": "Clic derecho para abrirla: guarda objetos (27 espacios).",
        "item.dedsafio3.bolsa_glebanoide": "Bolsa Glebanoide",
        "item.dedsafio3.bolsa_glebanoide.tooltip": "Tiene más espacio que las Bolsas Primitivas (54 espacios). Clic derecho para abrirla.",
        "message.dedsafio3.limbo_banished": "§5Has sido desterrado al Laberinto del Limbo. Encuentra la salida (suelo de luz ocre) para volver.",
        "message.dedsafio3.limbo_exit": "§aHas escapado del Limbo.",
        "item.dedsafio3.fruta_quiu": "Fruta de Quiu",
        "item.dedsafio3.fruta_quiu.tooltip": "Al comerla das Keep Inventory: conservas tu inventario al morir. El efecto se pierde al morir/revivir; hay que volver a comerla.",
        "effect.dedsafio3.keep_inventory": "Keep Inventory",
        "block.dedsafio3.plataforma_despegue": "Plataforma de Despegue",
        "item.dedsafio3.nave_espacial": "Nave Espacial",
        "item.dedsafio3.nave_espacial.tooltip": "Solo se puede colocar sobre una Plataforma de Despegue. Súbete con clic derecho: viaja a Gleba y de vuelta.",
        "message.dedsafio3.ship_only_platform": "§cLa nave espacial solo se puede colocar sobre una Plataforma de Despegue.",
        "message.dedsafio3.ship_unavailable": "§cLa nave no puede despegar aquí (solo desde la Tierra o Gleba).",
        "message.dedsafio3.ship_blocked": "§cNo hay espacio libre sobre la plataforma (necesita 3 bloques de altura y que no haya otra nave).",
        "message.dedsafio3.ship_countdown": "§eDespegue en %s…",
        "message.dedsafio3.ship_arrived_gleba": "§aHas aterrizado en Gleba, el Planeta Orgánico.",
        "message.dedsafio3.ship_arrived_home": "§aHas aterrizado de vuelta en la Tierra.",
        "gui.dedsafio3.tab.difficulty": "Dificultad",
        "gui.dedsafio3.tab.mobs": "Mobs",
        "gui.dedsafio3.tab.moments": "Momentos",
        "gui.dedsafio3.tab.search": "Buscar",
        "gui.dedsafio3.search.hint": "Escribe para buscar un cambio…",
        "gui.dedsafio3.search.empty": "Sin resultados",
        "message.dedsafio3.reminder_space_fruit": "§dRecordatorio: la Fruta del Espacio entre Dimensiones te teletransporta lejos de los Dáctylos Adultos. Para que desaparezcan, duerme.",
        "message.dedsafio3.space_fruit": "§dLa Fruta del Espacio te teletransportó lejos.",
        "command.dedsafio3.soul_given": "Alma devuelta a %s.",
        "command.dedsafio3.soul_already": "%s ya tenía su alma.",
        "command.dedsafio3.not_dead": "%s no está muerto (no es un alma).",
        "command.dedsafio3.revived": "%s ha sido revivido.",
        "item.dedsafio3.tenedor_resurreccion": "Tenedor de la Resurrección",
        "item.dedsafio3.tenedor_resurreccion.tooltip": "Como la Cuchara, pero no gasta tu alma. Clic derecho en una Fogata del Ritual.",
        "item.dedsafio3.fruta_espacio": "Fruta del Espacio entre Dimensiones",
        "item.dedsafio3.fruta_espacio.tooltip": "Al comerla te teletransporta lejos, para escapar de los Dáctylos Adultos. Para que desaparezcan, duerme.",
        "item.dedsafio3.indicador_insomnia": "Indicador de Insomnia",
        "item.dedsafio3.indicador_insomnia.tooltip": "Muestra tu nivel de Insomnia. Duerme para curarlo.",
        "item.dedsafio3.indicador_insomnia.level.none": "Indicador de Insomnia: sin Insomnia",
        "item.dedsafio3.indicador_insomnia.level.low": "Indicador de Insomnia: Bajo",
        "item.dedsafio3.indicador_insomnia.level.medium": "Indicador de Insomnia: Medio",
        "item.dedsafio3.indicador_insomnia.level.high": "Indicador de Insomnia: ¡Alto!",
        "gui.dedsafio3.tab.changes": "Cambios",
        "gui.dedsafio3.changes_hint": "Activar gira la ruleta naranja (roja en los Momentos Reviil) · Avisar lo activa y lo anuncia en el chat, sin ruleta",
        "gui.dedsafio3.notify": "Avisar",
        "gui.dedsafio3.notify_tip": "Activa este cambio sin ruleta y lo anuncia en el chat",
        "gui.dedsafio3.reviil_seconds": "Contador del Momento Reviil (s)",
        "gui.dedsafio3.page": "Página %s/%s",
        "message.dedsafio3.change_notice": "§6[%1$s] §e%2$s§f: %3$s",
        "message.dedsafio3.change_off": "§7Cambio desactivado: %s",
        "message.dedsafio3.reviil_bar": "Reviil lanzará su rayo en %s s: ¡entra al búnker!",
        "message.dedsafio3.reviil_bunker": "§c¡Momento Reviil! Eón construyó un búnker en %s, %s, %s. ¡Entren antes de que acabe el contador!",
        "message.dedsafio3.reviil_cancel": "§7El Momento Reviil fue cancelado.",
        "message.dedsafio3.reviil_strike": "§4¡El rayo de Reviil ha caído!",
        "effect.dedsafio3.veneno_primitivo": "Veneno Primitivo",
        "message.dedsafio3.reviil_meteor_bar": "Lluvia de meteoritos: ¡llega al refugio en %s s!",
        "message.dedsafio3.reviil_meteor_bunker": "§c¡Momento Reviil! Llueven meteoritos y el Nether está DESACTIVADO. Eón construyó un refugio en %s, %s, %s. ¡Lleguen antes de que acabe el tiempo!",
        "message.dedsafio3.nether_disabled": "§cEl Nether está desactivado.",
        "item.dedsafio3.cuchara_resurreccion": "Cuchara de la Resurrección",
        "item.dedsafio3.cuchara_resurreccion.tooltip": "Clic derecho en una Fogata del Ritual: revive a un jugador muerto gastando tu alma y esta Cuchara. Debe usarse el mismo día en que fue reclamada.",
        "block.dedsafio3.fogata_ritual": "Fogata del Ritual",
        "gui.dedsafio3.ritual.title": "Ritual de Resurrección",
        "gui.dedsafio3.ritual.hint": "Elige a quién revivir: se gastarán tu alma y la Cuchara",
        "message.dedsafio3.soul_taken": "§b%s ha muerto. Otro jugador puede revivirlo con un Ritual (Cuchara de la Resurrección + Fogata del Ritual).",
        "message.dedsafio3.ritual_soul_spent": "§cTu alma ya fue gastada: no puedes resucitar a nadie más.",
        "message.dedsafio3.ritual_no_souls": "§7No hay jugadores muertos que revivir.",
        "message.dedsafio3.ritual_far": "§cDebes estar cerca de la Fogata del Ritual.",
        "message.dedsafio3.ritual_expired": "§cLa Cuchara de la Resurrección se desvaneció: debía usarse el mismo día en que fue reclamada.",
        "message.dedsafio3.ritual_done": "§b%s ha vuelto a la vida: %s gastó su alma en el Ritual.",
        "message.dedsafio3.reviil_creepers": "§5¡Momento Reviil! Llueven Creepers Eléctricos con 1 de vida, las posiciones fueron intercambiadas y no puedes ver tu vida.",
        "message.dedsafio3.reviil_creepers_bar": "Lluvia de Creepers Eléctricos: %s s",
        "message.dedsafio3.reviil_creepers_end": "§7La lluvia de Creepers Eléctricos terminó.",
        "message.dedsafio3.reviil_gas_high": "§2¡Momento Reviil! Gas tóxico invisible en todas las dimensiones: ¡suban POR ENCIMA de la Capa 250 antes de que acabe el contador!",
        "message.dedsafio3.reviil_gas_low": "§2¡Momento Reviil! Gas tóxico invisible: ¡DESCIENDAN a una Capa Negativa (desde la -1 hacia abajo) antes de que acabe el contador!",
        "message.dedsafio3.reviil_gas_high_bar": "Gas tóxico en %s s: ¡sube por encima de la capa 250!",
        "message.dedsafio3.reviil_gas_low_bar": "Gas tóxico en %s s: ¡baja a una capa negativa!",
        "message.dedsafio3.reviil_gas_released": "§2¡El gas tóxico se ha liberado! Quien no esté a salvo recibirá daño hasta morir.",
        "message.dedsafio3.reviil_gas_end": "§7El gas tóxico se disipó.",
        "message.dedsafio3.reviil_hearts_hidden": "§7Mientras dure el Momento Reviil no podrás ver tus corazones.",
        "message.dedsafio3.reviil_moon_death": "§5%s miró a la Luna y la maldición de Reviil se cumplió. Maldición lunar desactivada.",
    },
    "en_us": {
        "item.dedsafio3.bolsa_primitiva": "Primitive Bag",
        "item.dedsafio3.bolsa_primitiva.tooltip": "Right click to open: stores items (27 slots).",
        "item.dedsafio3.bolsa_glebanoide": "Glebanoid Bag",
        "item.dedsafio3.bolsa_glebanoide.tooltip": "Has more space than Primitive Bags (54 slots). Right click to open.",
        "message.dedsafio3.limbo_banished": "§5You have been banished to the Limbo Labyrinth. Find the exit (ochre light floor) to return.",
        "message.dedsafio3.limbo_exit": "§aYou escaped the Limbo.",
        "item.dedsafio3.fruta_quiu": "Quiu Fruit",
        "item.dedsafio3.fruta_quiu.tooltip": "Eating it gives Keep Inventory: you keep your inventory on death. The effect is lost on death/revival; eat another one.",
        "effect.dedsafio3.keep_inventory": "Keep Inventory",
        "block.dedsafio3.plataforma_despegue": "Launch Platform",
        "item.dedsafio3.nave_espacial": "Spaceship",
        "item.dedsafio3.nave_espacial.tooltip": "Can only be placed on a Launch Platform. Right click to board: travels to Gleba and back.",
        "message.dedsafio3.ship_only_platform": "§cThe spaceship can only be placed on a Launch Platform.",
        "message.dedsafio3.ship_unavailable": "§cThe ship cannot take off here (only from Earth or Gleba).",
        "message.dedsafio3.ship_blocked": "§cNo free space above the platform (needs 3 blocks of height and no other ship).",
        "message.dedsafio3.ship_countdown": "§eLiftoff in %s…",
        "message.dedsafio3.ship_arrived_gleba": "§aYou have landed on Gleba, the Organic Planet.",
        "message.dedsafio3.ship_arrived_home": "§aYou have landed back on Earth.",
        "gui.dedsafio3.tab.difficulty": "Difficulty",
        "gui.dedsafio3.tab.mobs": "Mobs",
        "gui.dedsafio3.tab.moments": "Moments",
        "gui.dedsafio3.tab.search": "Search",
        "gui.dedsafio3.search.hint": "Type to search a change…",
        "gui.dedsafio3.search.empty": "No results",
        "message.dedsafio3.reminder_space_fruit": "§dReminder: the Interdimensional Space Fruit teleports you away from Adult Dactyls. To make them vanish, sleep.",
        "message.dedsafio3.space_fruit": "§dThe Space Fruit teleported you away.",
        "command.dedsafio3.soul_given": "Soul returned to %s.",
        "command.dedsafio3.soul_already": "%s already had their soul.",
        "command.dedsafio3.not_dead": "%s is not dead (not a soul).",
        "command.dedsafio3.revived": "%s has been revived.",
        "item.dedsafio3.tenedor_resurreccion": "Resurrection Fork",
        "item.dedsafio3.tenedor_resurreccion.tooltip": "Like the Spoon, but does not spend your soul. Right click on a Ritual Campfire.",
        "item.dedsafio3.fruta_espacio": "Interdimensional Space Fruit",
        "item.dedsafio3.fruta_espacio.tooltip": "Eating it teleports you far away, to escape Adult Dactyls. To make them vanish, sleep.",
        "item.dedsafio3.indicador_insomnia": "Insomnia Indicator",
        "item.dedsafio3.indicador_insomnia.tooltip": "Shows your Insomnia level. Sleep to cure it.",
        "item.dedsafio3.indicador_insomnia.level.none": "Insomnia Indicator: no Insomnia",
        "item.dedsafio3.indicador_insomnia.level.low": "Insomnia Indicator: Low",
        "item.dedsafio3.indicador_insomnia.level.medium": "Insomnia Indicator: Medium",
        "item.dedsafio3.indicador_insomnia.level.high": "Insomnia Indicator: High!",
        "gui.dedsafio3.tab.changes": "Changes",
        "gui.dedsafio3.changes_hint": "Enabling spins the orange wheel (red for Reviil Moments) · Notify enables it and announces it in chat, no wheel",
        "gui.dedsafio3.notify": "Notify",
        "gui.dedsafio3.notify_tip": "Enables this change without the wheel and announces it in chat",
        "gui.dedsafio3.reviil_seconds": "Reviil Moment countdown (s)",
        "gui.dedsafio3.page": "Page %s/%s",
        "message.dedsafio3.change_notice": "§6[%1$s] §e%2$s§f: %3$s",
        "message.dedsafio3.change_off": "§7Change disabled: %s",
        "message.dedsafio3.reviil_bar": "Reviil will fire its beam in %s s: get inside the bunker!",
        "message.dedsafio3.reviil_bunker": "§cReviil Moment! Eón built a bunker at %s, %s, %s. Get inside before the counter ends!",
        "message.dedsafio3.reviil_cancel": "§7The Reviil Moment was cancelled.",
        "message.dedsafio3.reviil_strike": "§4Reviil's beam has struck!",
        "effect.dedsafio3.veneno_primitivo": "Primitive Poison",
        "message.dedsafio3.reviil_meteor_bar": "Meteor shower: reach the shelter in %s s!",
        "message.dedsafio3.reviil_meteor_bunker": "§cReviil Moment! Meteors are raining and the Nether is DISABLED. Eón built a shelter at %s, %s, %s. Get there before time runs out!",
        "message.dedsafio3.nether_disabled": "§cThe Nether is disabled.",
        "item.dedsafio3.cuchara_resurreccion": "Resurrection Spoon",
        "item.dedsafio3.cuchara_resurreccion.tooltip": "Right click on a Ritual Campfire: revives a dead player by spending your soul and this Spoon. Must be used the same day it was claimed.",
        "block.dedsafio3.fogata_ritual": "Ritual Campfire",
        "gui.dedsafio3.ritual.title": "Resurrection Ritual",
        "gui.dedsafio3.ritual.hint": "Choose who to revive: your soul and the Spoon will be spent",
        "message.dedsafio3.soul_taken": "§b%s has died. Another player can revive them with a Ritual (Resurrection Spoon + Ritual Campfire).",
        "message.dedsafio3.ritual_soul_spent": "§cYour soul has already been spent: you cannot revive anyone else.",
        "message.dedsafio3.ritual_no_souls": "§7There are no dead players to revive.",
        "message.dedsafio3.ritual_far": "§cYou must be near the Ritual Campfire.",
        "message.dedsafio3.ritual_expired": "§cThe Resurrection Spoon faded away: it had to be used the same day it was claimed.",
        "message.dedsafio3.ritual_done": "§b%s came back to life: %s spent their soul in the Ritual.",
        "message.dedsafio3.reviil_creepers": "§5Reviil Moment! Electric Creepers with 1 health are raining, positions were swapped and you cannot see your health.",
        "message.dedsafio3.reviil_creepers_bar": "Electric Creeper rain: %s s",
        "message.dedsafio3.reviil_creepers_end": "§7The Electric Creeper rain is over.",
        "message.dedsafio3.reviil_gas_high": "§2Reviil Moment! Invisible toxic gas in all dimensions: get ABOVE Layer 250 before the counter ends!",
        "message.dedsafio3.reviil_gas_low": "§2Reviil Moment! Invisible toxic gas: DESCEND to a Negative Layer (from -1 downward) before the counter ends!",
        "message.dedsafio3.reviil_gas_high_bar": "Toxic gas in %s s: get above layer 250!",
        "message.dedsafio3.reviil_gas_low_bar": "Toxic gas in %s s: go down to a negative layer!",
        "message.dedsafio3.reviil_gas_released": "§2The toxic gas has been released! Anyone not safe will take damage until they die.",
        "message.dedsafio3.reviil_gas_end": "§7The toxic gas has dissipated.",
        "message.dedsafio3.reviil_hearts_hidden": "§7While the Reviil Moment lasts you will not be able to see your hearts.",
        "message.dedsafio3.reviil_moon_death": "§5%s looked at the Moon and Reviil's curse was fulfilled. Moon curse deactivated.",
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


def make_magma_creeper():
    """Creeper (64x32) de roca oscura con grietas de lava y cara naranja."""
    img = skin((64, 32), (58, 28, 24), 10, 81)
    rnd = random.Random(82)
    for _ in range(260):
        x, y = rnd.randrange(64), rnd.randrange(32)
        px(img, x, y, (235, 110, 25) if rnd.random() < 0.7 else (255, 190, 50))
    fill(img, 8, 8, 16, 16, (70, 32, 26))
    for ex in (9, 13):
        fill(img, ex, 10, ex + 2, 12, (255, 170, 40))
    fill(img, 11, 12, 13, 15, (255, 140, 30))
    fill(img, 10, 14, 11, 16, (255, 140, 30))
    fill(img, 13, 14, 14, 16, (255, 140, 30))
    return img


def make_spoon():
    """Cuchara de la Resurrección 16x16: mango de madera en diagonal y cuenco con brillo verde."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wood, dark = (150, 100, 55), (95, 60, 30)
    for i in range(0, 10):
        x, y = 2 + i, 13 - i
        px(img, x, y, wood)
        px(img, x + 1, y, dark)
    # cuenco (círculo arriba a la derecha)
    cx, cy = 11.5, 4.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, y - cy)
            if r < 3.6:
                px(img, x, y, (120, 230, 140) if r < 2.2 else (60, 150, 80))
            elif r < 4.4:
                px(img, x, y, dark)
    for (x, y) in ((11, 3), (12, 4)):
        px(img, x, y, (230, 255, 235))
    px(img, 1, 14, (60, 140, 60))   # hojita verde en el mango
    px(img, 2, 14, (90, 190, 90))
    return img


def make_soul_icon():
    """Icono de alma 16x16: llama fantasmal celeste con ojos."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(1, 15):
        w = 5 if y > 5 else max(1, y - 1)
        if y > 11:
            w = 5 - (y - 11)
        for x in range(8 - w, 8 + w):
            edge = x == 8 - w or x == 8 + w - 1 or y == 14
            px(img, x, y, (90, 190, 235) if edge else (170, 235, 255))
    for (x, y) in ((6, 7), (9, 7)):
        px(img, x, y, (20, 40, 80))
        px(img, x, y + 1, (20, 40, 80))
    px(img, 7, 10, (20, 40, 80))
    px(img, 8, 10, (20, 40, 80))
    return img


def make_flyer(base, accent, seed, eye):
    """Textura de nave/criatura voladora para el modelo de creeper (64x32), con paneles y luces."""
    img = skin((64, 32), base, 10, seed)
    rnd = random.Random(seed + 1)
    for _ in range(140):
        x, y = rnd.randrange(64), rnd.randrange(32)
        px(img, x, y, accent)
    fill(img, 8, 8, 16, 16, shade(base, 0.8))
    for ex in (9, 13):
        fill(img, ex, 10, ex + 2, 12, eye)
    fill(img, 10, 13, 14, 14, accent)
    return img


def make_dactilo():
    """Modelo de gallina (64x32): plumaje marrón anaranjado con pico amarillo."""
    img = skin((64, 32), (201, 138, 60), 14, 91)
    fill(img, 14, 0, 20, 6, (140, 90, 40))          # cresta/cabeza
    fill(img, 14, 4, 20, 6, (255, 200, 60))         # pico
    for x in (15, 18):
        px(img, x, 3, (20, 20, 20))
    return img


def make_narval():
    """Delfín (64x64) gris azulado con manchas claras."""
    img = skin((64, 64), (124, 143, 168), 10, 101)
    rnd = random.Random(102)
    for _ in range(500):
        x, y = rnd.randrange(64), rnd.randrange(64)
        px(img, x, y, (233, 228, 208))
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


def make_fork():
    """Tenedor de la Resurrección 16x16: como la cuchara pero con tres puntas y brillo celeste."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wood, dark = (150, 100, 55), (95, 60, 30)
    for i in range(0, 9):
        x, y = 2 + i, 13 - i
        px(img, x, y, wood)
        px(img, x + 1, y, dark)
    for (x, y) in ((9, 5), (10, 4), (11, 3), (12, 2), (11, 6), (12, 5), (13, 4), (14, 3), (8, 3), (9, 2), (10, 1), (11, 0)):
        px(img, x, y, (120, 200, 240))
    for (x, y) in ((10, 5), (11, 4), (12, 3), (13, 2)):
        px(img, x, y, (230, 250, 255))
    px(img, 1, 14, (60, 140, 60))
    return img


def make_space_fruit():
    """Fruta del Espacio 16x16: fruta violeta con estrellas y una hojita cian."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx, cy = 8, 9.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, (y - cy) * 1.05)
            if r < 5.6:
                edge = r > 4.5
                px(img, x, y, (60, 30, 110) if edge else (130, 70, 200))
    for (x, y) in ((6, 8), (10, 7), (9, 11), (6, 12)):
        px(img, x, y, (240, 230, 255))
    px(img, 8, 3, (90, 60, 40)); px(img, 8, 4, (90, 60, 40))
    for (x, y) in ((9, 2), (10, 2), (10, 3), (11, 3)):
        px(img, x, y, (80, 220, 220))
    return img


def make_insomnia_indicator():
    """Indicador de Insomnia 16x16: reloj/luna con barra de nivel."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if r < 6.4:
                px(img, x, y, (30, 30, 70) if r > 5.2 else (60, 60, 130))
    for y in range(16):
        for x in range(16):
            r2 = math.hypot(x - 9.5, y - 6.5)
            r1 = math.hypot(x - 7.5, y - 7.5)
            if r1 < 4.2 and r2 > 3.6:
                px(img, x, y, (250, 235, 150))
    for i, col in enumerate(((90, 200, 90), (240, 200, 60), (230, 60, 50))):
        fill(img, 3 + i * 4, 13, 6 + i * 4, 15, col)
    return img


def make_platform_top():
    """Plataforma de Despegue 16x16 (cara superior): chapa gris con franjas de aviso y marca circular."""
    img = skin((16, 16), (96, 100, 108), 6, 171)
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            px(img, x, y, (230, 190, 40) if (x + y) % 4 < 2 else (30, 30, 34))
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if 3.0 < r < 4.6:
                px(img, x, y, (230, 230, 235))
    return img


def make_platform_side():
    """Lateral de la Plataforma: chapa con remaches."""
    img = skin((16, 16), (84, 88, 96), 6, 172)
    for y in (0, 15):
        fill(img, 0, y, 16, y + 1, (40, 42, 48))
    for (x, y) in ((2, 3), (13, 3), (2, 12), (13, 12)):
        px(img, x, y, (170, 175, 185))
    fill(img, 0, 7, 16, 9, (230, 190, 40))
    return img


def make_ship_item():
    """Nave espacial 16x16: cohete blanco con ventana azul y aletas rojas."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(1, 13):
        w = 1 + min(y, 3) if y < 4 else 4
        for x in range(8 - w, 8 + w):
            edge = x == 8 - w or x == 8 + w - 1
            px(img, x, y, (170, 175, 185) if edge else (235, 238, 245))
    for (x, y) in ((7, 5), (8, 5), (7, 6), (8, 6)):
        px(img, x, y, (70, 150, 230))
    for y in range(9, 14):
        px(img, 3, y, (200, 50, 45)); px(img, 12, y, (200, 50, 45))
        px(img, 4, y + 1 if y < 13 else y, (200, 50, 45)); px(img, 11, y + 1 if y < 13 else y, (200, 50, 45))
    for (x, y) in ((7, 13), (8, 13), (7, 14), (8, 14), (7, 15)):
        px(img, x, y, (250, 160, 40))
    return img


def make_quiu_fruit():
    """Fruta de Quiu 16x16: fruta dorada con brillo y hojita verde."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 8, (y - 9.5) * 1.05)
            if r < 5.6:
                px(img, x, y, (160, 110, 20) if r > 4.5 else (240, 190, 50))
    for (x, y) in ((6, 7), (7, 6), (6, 8)):
        px(img, x, y, (255, 245, 190))
    px(img, 8, 3, (90, 60, 30)); px(img, 8, 4, (90, 60, 30))
    for (x, y) in ((9, 2), (10, 2), (10, 3), (11, 3)):
        px(img, x, y, (70, 170, 60))
    return img


def make_keep_icon():
    """Icono de efecto 18x18: cofre dorado."""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    fill(img, 3, 6, 15, 15, (200, 150, 40))
    fill(img, 3, 6, 15, 8, (235, 190, 70))
    fill(img, 3, 10, 15, 11, (120, 80, 20))
    fill(img, 8, 9, 10, 13, (240, 240, 240))
    for x in range(3, 15):
        px(img, x, 5, (120, 80, 20)); px(img, x, 15, (120, 80, 20))
    return img


def make_bag(base, trim):
    """Bolsa 16x16: saco con cuerda y solapa."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(4, 15):
        w = 4 + (y - 4) // 2 if y < 10 else 6 - (y - 10) // 3
        for x in range(8 - w, 8 + w):
            edge = x == 8 - w or x == 8 + w - 1 or y == 14
            px(img, x, y, shade(base, 0.7) if edge else base)
    fill(img, 5, 2, 11, 5, shade(base, 0.85))
    fill(img, 5, 5, 11, 6, trim)
    px(img, 8, 7, trim); px(img, 8, 8, trim)
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

    # --- icono de alma junto al nombre de los jugadores (fuente de bitmap con un solo carácter)
    save_png(make_soul_icon(), "textures", "font", "soul.png")
    write_json(os.path.join(ASSETS, "font", "icons.json"), {"providers": [{
        "type": "bitmap", "file": f"{MOD}:font/soul.png", "ascent": 7, "height": 8, "chars": ["\ue000"]}]})

    # --- Fogata del Ritual: una fogata más grande (troncos y llama a escala de bloque completo)
    def log(frm, to, axis_x):
        faces = {d: {"texture": "#log", "uv": [0, 0, 16, 4]} for d in ("north", "south", "east", "west")}
        faces["up"] = {"texture": "#log", "uv": [0, 0, 16, 4]}
        faces["down"] = {"texture": "#log", "uv": [0, 0, 16, 4]}
        return {"from": frm, "to": to, "faces": faces}
    elements = [
        log([0, 0, 1], [16, 4, 5], True), log([0, 0, 11], [16, 4, 15], True),
        log([1, 4, 0], [5, 8, 16], False), log([11, 4, 0], [15, 8, 16], False),
        {"from": [0, 1, 8], "to": [16, 15, 8], "faces": {
            "north": {"texture": "#fire", "uv": [0, 0, 16, 16]}, "south": {"texture": "#fire", "uv": [0, 0, 16, 16]}}},
        {"from": [8, 1, 0], "to": [8, 15, 16], "faces": {
            "east": {"texture": "#fire", "uv": [0, 0, 16, 16]}, "west": {"texture": "#fire", "uv": [0, 0, 16, 16]}}},
    ]
    write_json(os.path.join(ASSETS, "models", "block", "fogata_ritual.json"), {
        "ambientocclusion": False,
        "textures": {"particle": "minecraft:block/campfire_log", "log": "minecraft:block/campfire_log",
                     "fire": "minecraft:block/campfire_fire"},
        "elements": elements,
    })
    write_json(os.path.join(ASSETS, "blockstates", "fogata_ritual.json"),
               {"variants": {"": {"model": f"{MOD}:block/fogata_ritual"}}})
    write_json(os.path.join(ASSETS, "models", "item", "fogata_ritual.json"), {"parent": f"{MOD}:block/fogata_ritual"})
    write_json(os.path.join(DATA, "loot_table", "blocks", "fogata_ritual.json"), {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0,
            "bonus_rolls": 0.0,
            "entries": [{"type": "minecraft:item", "name": f"{MOD}:fogata_ritual"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })
    write_json(os.path.join(DATA, "recipe", "fogata_ritual.json"), recipe_shaped(
        ["LLL", "LCL", "LLL"], {"L": "minecraft:oak_log", "C": "minecraft:campfire"}, f"{MOD}:fogata_ritual"))

    # --- v1.6.0: Plataforma de Despegue, Nave Espacial y el planeta Gleba
    save_png(make_platform_top(), "textures", "block", "plataforma_despegue_top.png")
    save_png(make_platform_side(), "textures", "block", "plataforma_despegue_side.png")
    save_png(make_platform_side(), "textures", "block", "plataforma_despegue_bottom.png")
    write_json(os.path.join(ASSETS, "models", "block", "plataforma_despegue.json"), {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": f"{MOD}:block/plataforma_despegue_top", "side": f"{MOD}:block/plataforma_despegue_side",
                     "bottom": f"{MOD}:block/plataforma_despegue_bottom", "particle": f"{MOD}:block/plataforma_despegue_side"}})
    write_json(os.path.join(ASSETS, "blockstates", "plataforma_despegue.json"),
               {"variants": {"": {"model": f"{MOD}:block/plataforma_despegue"}}})
    write_json(os.path.join(ASSETS, "models", "item", "plataforma_despegue.json"), {"parent": f"{MOD}:block/plataforma_despegue"})
    write_json(os.path.join(DATA, "loot_table", "blocks", "plataforma_despegue.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                   "entries": [{"type": "minecraft:item", "name": f"{MOD}:plataforma_despegue"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
    })
    write_json(os.path.join(DATA, "recipe", "plataforma_despegue.json"), recipe_shaped(
        ["IRI", "IBI", "III"],
        {"I": "minecraft:iron_block", "R": "minecraft:redstone_block", "B": "minecraft:blast_furnace"},
        f"{MOD}:plataforma_despegue"))
    write_json(os.path.join(DATA, "recipe", "nave_espacial.json"), recipe_shaped(
        ["IGI", "IRI", "DFD"],
        {"I": "minecraft:iron_block", "G": "minecraft:glass", "R": "minecraft:redstone_block",
         "D": "minecraft:diamond", "F": "minecraft:firework_rocket"},
        f"{MOD}:nave_espacial"))
    save_png(make_ship_item(), "textures", "item", "nave_espacial.png")
    save_png(make_flyer((205, 210, 220), (200, 50, 45), 181, (70, 150, 230)), "textures", "entity", "nave_espacial.png")

    save_png(make_flyer((60, 140, 170), (170, 220, 240), 200, (255, 255, 255)), "textures", "entity", "creeper_fluido.png")
    save_png(make_flyer((225, 220, 200), (150, 140, 120), 210, (40, 40, 40)), "textures", "entity", "nautilus_oseo.png")
    save_png(make_flyer((150, 60, 110), (230, 150, 190), 220, (255, 230, 120)), "textures", "entity", "garrapata_cerebral.png")
    save_png(make_flyer((190, 90, 80), (240, 170, 150), 230, (30, 20, 20)), "textures", "entity", "gusano_carne.png")
    save_png(make_flyer((70, 110, 50), (150, 200, 90), 240, (240, 240, 100)), "textures", "entity", "parasito.png")
    save_png(make_flyer((230, 200, 60), (180, 140, 30), 250, (30, 30, 30)), "textures", "entity", "cerebro_amarillo.png")
    save_png(make_flyer((110, 60, 150), (200, 140, 240), 260, (255, 255, 255)), "textures", "entity", "parasito_volador.png")
    save_png(make_flyer((240, 240, 245), (60, 60, 70), 270, (255, 60, 60)), "textures", "entity", "flashbang.png")
    save_png(make_biped((30, 24, 46), 8, 61, (50, 40, 80), None, (230, 60, 255)), "textures", "entity", "reaper_pequeno.png")
    save_png(make_biped((226, 222, 204), 10, 62, (180, 176, 160), (110, 27, 27), (200, 30, 30)), "textures", "entity", "medusa_osea.png")
    save_png(make_flyer((190, 40, 50), (240, 120, 130), 223, (255, 230, 120)), "textures", "entity", "cerebro_rojo.png")
    save_png(make_bag((160, 110, 60), (230, 210, 150)), "textures", "item", "bolsa_primitiva.png")
    save_png(make_bag((90, 160, 70), (240, 200, 60)), "textures", "item", "bolsa_glebanoide.png")
    write_json(os.path.join(DATA, "recipe", "bolsa_primitiva.json"), recipe_shaped(
        ["LSL", "L L", "LLL"], {"L": "minecraft:leather", "S": "minecraft:string"}, f"{MOD}:bolsa_primitiva"))
    write_json(os.path.join(DATA, "recipe", "bolsa_glebanoide.json"), recipe_shaped(
        ["MMM", "MPM", "MMM"], {"M": "minecraft:moss_block", "P": f"{MOD}:bolsa_primitiva"}, f"{MOD}:bolsa_glebanoide"))
    # Limbo: dimensión plana oscura; el laberinto lo construye el código al desterrar a alguien.
    write_json(os.path.join(DATA, "dimension_type", "limbo.json"), {
        "ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": False, "has_ceiling": True,
        "ambient_light": 0.0, "fixed_time": 18000, "piglin_safe": False, "bed_works": False, "respawn_anchor_works": False,
        "has_raids": False, "logical_height": 64, "min_y": 0, "height": 64,
        "infiniburn": "#minecraft:infiniburn_overworld", "effects": "minecraft:overworld",
        "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 15})
    write_json(os.path.join(DATA, "dimension", "limbo.json"), {
        "type": f"{MOD}:limbo",
        "generator": {"type": "minecraft:flat", "settings": {
            "biome": "minecraft:the_void", "features": False, "lakes": False,
            "layers": [{"block": "minecraft:bedrock", "height": 1}, {"block": "minecraft:polished_blackstone", "height": 2}],
            "structure_overrides": []}}})
    save_png(make_quiu_fruit(), "textures", "item", "fruta_quiu.png")
    save_png(make_keep_icon(), "textures", "mob_effect", "keep_inventory.png")
    write_json(os.path.join(DATA, "recipe", "fruta_quiu.json"), recipe_shapeless(
        ["minecraft:golden_apple", "minecraft:diamond", "minecraft:sweet_berries"], f"{MOD}:fruta_quiu"))

    # Gleba: dimensión por datapack. Terreno del Overworld con un único bioma orgánico (hongos, hierba amarillenta).
    write_json(os.path.join(DATA, "dimension_type", "gleba.json"), {
        "ultrawarm": False, "natural": True, "coordinate_scale": 1.0, "has_skylight": True, "has_ceiling": False,
        "ambient_light": 0.1, "piglin_safe": False, "bed_works": True, "respawn_anchor_works": False,
        "has_raids": False, "logical_height": 384, "min_y": -64, "height": 384,
        "infiniburn": "#minecraft:infiniburn_overworld", "effects": "minecraft:overworld",
        "monster_spawn_light_level": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 7},
        "monster_spawn_block_light_limit": 0})
    write_json(os.path.join(DATA, "dimension", "gleba.json"), {
        "type": f"{MOD}:gleba",
        "generator": {"type": "minecraft:noise", "settings": "minecraft:overworld",
                      "biome_source": {"type": "minecraft:fixed", "biome": f"{MOD}:gleba"}}})
    features = [[] for _ in range(11)]
    features[6] = ["minecraft:ore_dirt", "minecraft:ore_gravel", "minecraft:ore_coal_upper", "minecraft:ore_coal_lower",
                   "minecraft:ore_iron_upper", "minecraft:ore_iron_middle", "minecraft:ore_gold", "minecraft:ore_redstone",
                   "minecraft:ore_diamond", "minecraft:ore_lapis", "minecraft:ore_copper"]
    features[8] = ["minecraft:spring_water", "minecraft:spring_lava"]
    features[9] = ["minecraft:mushroom_island_vegetation", "minecraft:brown_mushroom_normal",
                   "minecraft:red_mushroom_normal", "minecraft:patch_grass_plain"]
    write_json(os.path.join(DATA, "worldgen", "biome", "gleba.json"), {
        "has_precipitation": True, "temperature": 0.9, "downfall": 0.9,
        "effects": {"sky_color": 0xC49AA8, "fog_color": 0xE6B8A2, "water_color": 0x6FAE4F, "water_fog_color": 0x4B7A32,
                    "grass_color": 0xB5C23A, "foliage_color": 0x8FB035},
        "spawners": {}, "spawn_costs": {},
        "carvers": {"air": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"]},
        "features": features})

    # --- modelos de ítems
    for name in ITEMS:
        parent = "minecraft:item/handheld" if name in ("espada_caotica", "cuchara_resurreccion", "tenedor_resurreccion") else "minecraft:item/generated"
        write_json(os.path.join(ASSETS, "models", "item", f"{name}.json"), item_model(name, parent))

    # --- animación de muerte (12 fotogramas) e icono
    for f in range(12):
        save_png(make_death_frame(f), "textures", "gui", "death", f"frame_{f:02d}.png")
    save_png(make_icon(), "icon.png")

    # --- tags
    write_json(os.path.join(ROOT, "data", "minecraft", "tags", "block", "mineable", "pickaxe.json"), {
        "replace": False,
        "values": [f"{MOD}:roulette_{c}" for c in list(COLORS) + ["random"]] + [f"{MOD}:plataforma_despegue"],
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
    save_png(make_biped((38, 84, 170), 16, 46, (20, 50, 110), (150, 210, 255), (255, 240, 80)), "textures", "entity", "dromoraptor_azul.png")
    save_png(make_magma_creeper(), "textures", "entity", "creeper_magma.png")
    save_png(make_dactilo(), "textures", "entity", "dactilo_bebe.png")
    save_png(make_narval(), "textures", "entity", "narval.png")
    # Herobrine: humanoide gris oscuro propio (no es la skin de Steve) con ojos blancos.
    save_png(make_biped((54, 58, 64), 8, 47, (40, 44, 50), None, (255, 255, 255)), "textures", "entity", "herobrine.png")
    save_png(make_flyer((12, 46, 54), (42, 212, 196), 111, (200, 255, 250)), "textures", "entity", "bomba_warden.png")
    save_png(make_flyer((58, 63, 74), (224, 64, 64), 121, (255, 120, 120)), "textures", "entity", "pod_phora.png")
    save_png(make_flyer((74, 58, 42), (255, 144, 32), 131, (255, 220, 120)), "textures", "entity", "skiff_phora.png")
    save_png(make_flyer((52, 150, 44), (24, 90, 24), 141, (15, 30, 10)), "textures", "entity", "creeper_pasto.png")
    save_png(make_spoon(), "textures", "item", "cuchara_resurreccion.png")
    save_png(make_fork(), "textures", "item", "tenedor_resurreccion.png")
    save_png(make_space_fruit(), "textures", "item", "fruta_espacio.png")
    save_png(make_insomnia_indicator(), "textures", "item", "indicador_insomnia.png")
    save_png(make_flyer((24, 24, 28), (110, 20, 24), 151, (230, 40, 40)), "textures", "entity", "creeper_wither_rose.png")
    save_png(skin((64, 32), (122, 84, 46), 14, 161), "textures", "entity", "dactilo_adulto.png")
    save_png(make_biped((46, 140, 62), 16, 48, (24, 90, 40), (170, 240, 120), (255, 240, 80)), "textures", "entity", "dromoraptor_verde.png")
    save_png(make_soul_icon(), "textures", "gui", "soul.png")
    save_png(make_poison_icon(), "textures", "mob_effect", "veneno_primitivo.png")
    for eid in ENTITY_NAMES:
        write_json(os.path.join(ASSETS, "models", "item", f"spawn_{eid}.json"), {"parent": "minecraft:item/template_spawn_egg"})

    # --- idiomas
    for code, entries in build_lang().items():
        write_json(os.path.join(ASSETS, "lang", f"{code}.json"), entries)

    print("Recursos generados en", os.path.normpath(ROOT))


if __name__ == "__main__":
    main()
