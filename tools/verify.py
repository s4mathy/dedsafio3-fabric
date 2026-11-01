#!/usr/bin/env python3
"""Comprobaciones estáticas (no sustituyen a compilar): idiomas vs código, JSON válidos,
referencias de modelos/texturas, ítems registrados vs recursos."""
import glob
import json
import os
import re
import sys

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
JAVA = os.path.join(BASE, "src", "main", "java", "com", "dedsafio3")
RES = os.path.join(BASE, "src", "main", "resources")
MOD = "dedsafio3"
errors = []


def err(msg):
    errors.append(msg)


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


# 1) todos los JSON parsean
jsons = glob.glob(os.path.join(RES, "**", "*.json"), recursive=True)
for p in jsons:
    try:
        json.loads(read(p))
    except Exception as e:
        err(f"JSON inválido {os.path.relpath(p, BASE)}: {e}")

# 2) premios de Roulette.java <-> idiomas
roulette = read(os.path.join(JAVA, "Roulette.java"))
code_prizes = set(re.findall(r'add\(RouletteColor\.(\w+),\s*"(\w+)"', roulette))
code_keys = {f"roulette.{MOD}.{c.lower()}.{pid}" for c, pid in code_prizes}
for lang in ("es_es", "en_us"):
    data = json.loads(read(os.path.join(RES, "assets", MOD, "lang", f"{lang}.json")))
    lang_prize_keys = {k for k in data if k.startswith(f"roulette.{MOD}.")}
    for k in sorted(code_keys - lang_prize_keys):
        err(f"[{lang}] falta traducción del premio: {k}")
    for k in sorted(lang_prize_keys - code_keys):
        err(f"[{lang}] traducción sin premio en el código: {k}")

# 3) ítems registrados <-> modelo/textura/lang
items_java = read(os.path.join(JAVA, "ModItems.java"))
item_ids = re.findall(r'register\("(\w+)"', items_java)
colors = re.findall(r'^\s+\w+\("(\w+)",\s*0x', read(os.path.join(JAVA, "RouletteColor.java")), re.M)
enum_colors = list(colors)
colors = colors + ["random"]
lang_es = json.loads(read(os.path.join(RES, "assets", MOD, "lang", "es_es.json")))
lang_en = json.loads(read(os.path.join(RES, "assets", MOD, "lang", "en_us.json")))
for name in item_ids:
    for rel in (f"models/item/{name}.json", f"textures/item/{name}.png"):
        if not os.path.exists(os.path.join(RES, "assets", MOD, rel)):
            err(f"ítem '{name}': falta {rel}")
    for lang_name, lang in (("es_es", lang_es), ("en_us", lang_en)):
        if f"item.{MOD}.{name}" not in lang:
            err(f"[{lang_name}] falta nombre de ítem {name}")
        if f"item.{MOD}.{name}.tooltip" not in lang:
            err(f"[{lang_name}] falta tooltip de ítem {name}")
for c in colors:
    for rel in (f"blockstates/roulette_{c}.json", f"models/block/roulette_{c}.json", f"models/item/roulette_{c}.json",
                f"textures/block/roulette_{c}_top.png", f"textures/block/roulette_{c}_side.png"):
        if not os.path.exists(os.path.join(RES, "assets", MOD, rel)):
            err(f"ruleta '{c}': falta {rel}")
    if not os.path.exists(os.path.join(RES, "data", MOD, "loot_table", "blocks", f"roulette_{c}.json")):
        err(f"ruleta '{c}': falta loot table")
    for lang_name, lang in (("es_es", lang_es), ("en_us", lang_en)):
        if f"block.{MOD}.roulette_{c}" not in lang:
            err(f"[{lang_name}] falta nombre de bloque roulette_{c}")

# 4) claves de traducción usadas en Java existen en ambos idiomas
used = set()
for jf in glob.glob(os.path.join(JAVA, "**", "*.java"), recursive=True):
    found = set(re.findall(r'"((?:message|death|command|itemGroup|gui|key|modifier|color)\.dedsafio3[\w.]*)"', read(jf)))
    used |= {k for k in found if not k.endswith(".")}  # las que acaban en "." son prefijos dinámicos, se revisan abajo
used.add(f"itemGroup.{MOD}")
for k in sorted(used):
    for lang_name, lang in (("es_es", lang_es), ("en_us", lang_en)):
        if k not in lang:
            err(f"[{lang_name}] falta clave usada en Java: {k}")

# 4b) claves dinámicas: colores, modificadores, dificultades, pestañas
mods = re.findall(r'^\s+[A-Z_]+\("(\w+)"\)', read(os.path.join(JAVA, "WorldModifier.java")), re.M)
tabs = re.findall(r'\b[A-Z]+\("([a-z]+)"\)', read(os.path.join(JAVA, "client", "DedsafioMenuScreen.java")))
dyn = [f"color.{MOD}.{c}" for c in enum_colors] + [f"modifier.{MOD}.{m}" for m in mods] \
    + [f"gui.{MOD}.difficulty.{i}" for i in range(4)] + [f"gui.{MOD}.tab.{t}" for t in tabs]
if len(mods) != 3:
    err(f"se esperaban 3 modificadores en WorldModifier.java, hay {len(mods)}")
if len(tabs) != 4:
    err(f"se esperaban 4 pestañas en DedsafioMenuScreen.java, hay {len(tabs)}")
for k in dyn:
    for lang_name, lang in (("es_es", lang_es), ("en_us", lang_en)):
        if k not in lang:
            err(f"[{lang_name}] falta clave dinámica: {k}")
# claves de red: ids de payload únicos
ids = []
for jf in glob.glob(os.path.join(JAVA, "*Payload.java")):
    ids += re.findall(r'Dedsafio3\.id\("(\w+)"\)', read(jf))
if len(ids) != len(set(ids)):
    err(f"ids de payload repetidos: {ids}")
# claves de lang sin uso aparente (aviso, no error)
print(f"payloads: {len(ids)} | modificadores: {len(mods)} | pestañas: {len(tabs)}")

# 4c) v1.1.1: cambios (DifficultyChange), mobs, texturas, mixin
change_src = read(os.path.join(JAVA, "DifficultyChange.java"))
change_ids = re.findall(r'^\s+[A-Z_]+\("(\w+)",\s*Kind\.\w+\)', change_src, re.M)
if len(change_ids) != 12:
    err(f"se esperaban 12 cambios en DifficultyChange.java, hay {len(change_ids)}")
for cid in change_ids:
    for suffix in ("name", "desc", "wheel"):
        for lang_name, lang in (("es_es", lang_es), ("en_us", lang_en)):
            if f"change.{MOD}.{cid}.{suffix}" not in lang:
                err(f"[{lang_name}] falta change.{MOD}.{cid}.{suffix}")
for kind in ("difficulty", "mob", "moment"):
    for lang_name, lang in (("es_es", lang_es), ("en_us", lang_en)):
        if f"change.{MOD}.title.{kind}" not in lang:
            err(f"[{lang_name}] falta change.{MOD}.title.{kind}")
ent_src = read(os.path.join(JAVA, "ModEntities.java")) + read(os.path.join(JAVA, "ReptilianEntity.java"))
entity_ids = set(re.findall(r'register\("(\w+)"', ent_src)) | set(re.findall(r'^\s+[A-Z_]+\("(\w+)",\s*[\d.]+,', ent_src, re.M))
entity_ids = {e for e in entity_ids if e not in ("kind",)}
if len(entity_ids) != 7:
    err(f"se esperaban 7 mobs nuevos, encontrados {sorted(entity_ids)}")
for eid in entity_ids:
    if not os.path.exists(os.path.join(RES, "assets", MOD, "textures", "entity", f"{eid}.png")):
        err(f"mob '{eid}': falta textures/entity/{eid}.png")
    if not os.path.exists(os.path.join(RES, "assets", MOD, "models", "item", f"spawn_{eid}.json")):
        err(f"mob '{eid}': falta models/item/spawn_{eid}.json")
    for lang_name, lang in (("es_es", lang_es), ("en_us", lang_en)):
        for k in (f"entity.{MOD}.{eid}", f"item.{MOD}.spawn_{eid}"):
            if k not in lang:
                err(f"[{lang_name}] falta {k}")
eff = re.search(r'Dedsafio3\.id\("(\w+)"\)', read(os.path.join(JAVA, "ModEffects.java")))
if not eff or not os.path.exists(os.path.join(RES, "assets", MOD, "textures", "mob_effect", f"{eff.group(1)}.png")):
    err("falta el icono del efecto en textures/mob_effect/")
if eff and f"effect.{MOD}.{eff.group(1)}" not in lang_es:
    err("falta el nombre del efecto en es_es")
mixin_cfg = json.loads(read(os.path.join(RES, "dedsafio3.mixins.json")))
for m in mixin_cfg.get("client", []) + mixin_cfg.get("mixins", []):
    if not os.path.exists(os.path.join(JAVA, "mixin", f"{m}.java")):
        err(f"mixin declarado sin clase: {m}")
print(f"cambios: {len(change_ids)} | mobs: {len(entity_ids)}")

# 5) fotogramas de muerte (FRAME_COUNT en DeathOverlay)
m = re.search(r"FRAME_COUNT\s*=\s*(\d+)", read(os.path.join(JAVA, "client", "DeathOverlay.java")))
frames = int(m.group(1)) if m else 0
for i in range(frames):
    if not os.path.exists(os.path.join(RES, "assets", MOD, "textures", "gui", "death", f"frame_{i:02d}.png")):
        err(f"falta fotograma de muerte frame_{i:02d}.png")

# 6) cada .java: llaves y paréntesis balanceados (detecta errores de tipeo gruesos)
for jf in glob.glob(os.path.join(JAVA, "**", "*.java"), recursive=True):
    src = re.sub(r'"(?:\\.|[^"\\])*"', '""', read(jf))
    src = re.sub(r"//.*", "", src)
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    for o, c in (("{", "}"), ("(", ")"), ("[", "]")):
        if src.count(o) != src.count(c):
            err(f"{os.path.relpath(jf, BASE)}: '{o}' y '{c}' no balanceados ({src.count(o)} vs {src.count(c)})")

print(f"JSON revisados: {len(jsons)} | premios: {len(code_prizes)} | ítems: {len(item_ids)} | ruletas: {len(colors)}")
if errors:
    print("\nPROBLEMAS:")
    for e in errors:
        print(" -", e)
    sys.exit(1)
print("OK: sin problemas.")
