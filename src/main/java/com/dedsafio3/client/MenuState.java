package com.dedsafio3.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/** Copia en el cliente del estado que muestra el menú (llega del servidor como JSON). */
public record MenuState(int difficulty, boolean pvp, boolean keepInventory, boolean deathAnimation,
						boolean totems, int cooldown, int spin, List<Integer> weights, List<String> modifiers) {

	public static MenuState parse(String json) {
		JsonObject o = JsonParser.parseString(json).getAsJsonObject();
		List<Integer> weights = new ArrayList<>();
		for (JsonElement e : o.getAsJsonArray("weights")) {
			weights.add(e.getAsInt());
		}
		List<String> modifiers = new ArrayList<>();
		JsonArray mods = o.getAsJsonArray("modifiers");
		for (JsonElement e : mods) {
			modifiers.add(e.getAsString());
		}
		return new MenuState(
				o.get("difficulty").getAsInt(),
				o.get("pvp").getAsBoolean(),
				o.get("keepInventory").getAsBoolean(),
				o.get("deathAnimation").getAsBoolean(),
				o.get("totems").getAsBoolean(),
				o.get("cooldown").getAsInt(),
				o.get("spin").getAsInt(),
				weights,
				modifiers
		);
	}
}
