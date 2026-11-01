package com.dedsafio3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Ajustes del mod, guardados en config/dedsafio3.json y editables desde el menú (tecla K). */
public final class DedsafioConfig {
	public boolean deathAnimation = true;
	public boolean totemsEnabled = true;
	/** Espera entre giros del mismo jugador, en segundos. */
	public int cooldownSeconds = 2;
	/** Duración del giro de la rueda, en segundos. */
	public int spinSeconds = 6;
	/** Peso de cada color en la rueda (orden de {@link RouletteColor}). 0 = color desactivado. */
	public int[] weights = {10, 10, 10, 10, 10, 10};
	/** Modificaciones del mundo activas (ids de {@link WorldModifier}). */
	public List<String> modifiers = new ArrayList<>();
	/** Cambios de dificultad activos (ids de {@link DifficultyChange}; el Momento Reviil no se guarda). */
	public List<String> changes = new ArrayList<>();
	/** Segundos que dura el contador del Momento Reviil. */
	public int reviilSeconds = 60;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static DedsafioConfig instance = new DedsafioConfig();

	public static DedsafioConfig get() {
		return instance;
	}

	public int weight(RouletteColor color) {
		return weights[color.ordinal()];
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("dedsafio3.json");
	}

	public static void load() {
		Path path = path();
		try {
			if (Files.exists(path)) {
				DedsafioConfig loaded = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), DedsafioConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			}
		} catch (Exception e) {
			Dedsafio3.LOGGER.error("No se pudo leer {}, se usan los valores por defecto", path, e);
		}
		instance.sanitize();
		save();
	}

	public static void save() {
		try {
			Files.writeString(path(), GSON.toJson(instance), StandardCharsets.UTF_8);
		} catch (IOException e) {
			Dedsafio3.LOGGER.error("No se pudo guardar la configuración", e);
		}
	}

	private void sanitize() {
		int count = RouletteColor.values().length;
		if (weights == null || weights.length != count) {
			int[] fixed = new int[count];
			for (int i = 0; i < count; i++) {
				fixed[i] = (weights != null && i < weights.length) ? weights[i] : 10;
			}
			weights = fixed;
		}
		for (int i = 0; i < weights.length; i++) {
			weights[i] = Math.max(0, Math.min(20, weights[i]));
		}
		cooldownSeconds = Math.max(0, Math.min(60, cooldownSeconds));
		spinSeconds = Math.max(3, Math.min(15, spinSeconds));
		if (modifiers == null) {
			modifiers = new ArrayList<>();
		}
		if (changes == null) {
			changes = new ArrayList<>();
		}
		changes.removeIf(id -> DifficultyChange.byId(id) == null || id.equals("reviil"));
		reviilSeconds = Math.max(20, Math.min(180, reviilSeconds));
	}
}
