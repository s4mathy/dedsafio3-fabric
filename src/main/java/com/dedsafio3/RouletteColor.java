package com.dedsafio3;

import net.minecraft.block.MapColor;

import java.util.Arrays;
import java.util.List;

/** Las seis ruletas de colores. El orden (ordinal) se usa en los paquetes de red. */
public enum RouletteColor {
	RED("red", 0xE53935, MapColor.RED),
	ORANGE("orange", 0xFB8C00, MapColor.ORANGE),
	YELLOW("yellow", 0xFDD835, MapColor.YELLOW),
	GREEN("green", 0x43A047, MapColor.GREEN),
	BLUE("blue", 0x1E88E5, MapColor.BLUE),
	PURPLE("purple", 0x8E24AA, MapColor.PURPLE);

	private final String id;
	private final int rgb;
	private final MapColor mapColor;

	RouletteColor(String id, int rgb, MapColor mapColor) {
		this.id = id;
		this.rgb = rgb;
		this.mapColor = mapColor;
	}

	public String id() {
		return id;
	}

	public int rgb() {
		return rgb;
	}

	public MapColor mapColor() {
		return mapColor;
	}

	/** Clave de traducción del nombre del bloque (se usa como título de la ruleta). */
	public String blockTranslationKey() {
		return "block." + Dedsafio3.MOD_ID + ".roulette_" + id;
	}

	public static RouletteColor byId(String id) {
		for (RouletteColor c : values()) {
			if (c.id.equalsIgnoreCase(id)) {
				return c;
			}
		}
		return null;
	}

	public static RouletteColor byOrdinal(int ordinal) {
		RouletteColor[] all = values();
		return all[Math.floorMod(ordinal, all.length)];
	}

	public static List<String> ids() {
		return Arrays.stream(values()).map(RouletteColor::id).toList();
	}
}
