package com.dedsafio3;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;

/** Lado servidor del menú interactivo: valida permisos, aplica los cambios y devuelve el estado. */
public final class MenuService {
	private MenuService() {
	}

	public static boolean canEdit(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		return player.hasPermissionLevel(2) || (server != null && server.isHost(player.getGameProfile()));
	}

	public static String stateJson(MinecraftServer server) {
		DedsafioConfig config = DedsafioConfig.get();
		JsonObject o = new JsonObject();
		o.addProperty("difficulty", server.getOverworld().getDifficulty().getId());
		o.addProperty("pvp", server.isPvpEnabled());
		o.addProperty("keepInventory", server.getGameRules().getBoolean(GameRules.KEEP_INVENTORY));
		o.addProperty("deathAnimation", config.deathAnimation);
		o.addProperty("totems", config.totemsEnabled);
		o.addProperty("cooldown", config.cooldownSeconds);
		o.addProperty("spin", config.spinSeconds);
		JsonArray weights = new JsonArray();
		for (int weight : config.weights) {
			weights.add(weight);
		}
		o.add("weights", weights);
		JsonArray modifiers = new JsonArray();
		for (String modifier : config.modifiers) {
			modifiers.add(modifier);
		}
		o.add("modifiers", modifiers);
		JsonArray changes = new JsonArray();
		for (DifficultyChange change : DifficultyChange.values()) {
			if (change.isActive()) {
				changes.add(change.id());
			}
		}
		o.add("changes", changes);
		o.addProperty("reviilSeconds", config.reviilSeconds);
		return o.toString();
	}

	/** Envía el estado al jugador. Con {@code open} abre la pantalla; si no, solo refresca la que esté abierta. */
	public static void send(ServerPlayerEntity player, boolean open) {
		MinecraftServer server = player.getServer();
		if (server != null) {
			ServerPlayNetworking.send(player, new ConfigPayload(open, stateJson(server)));
		}
	}

	/** Refresca el menú de todos los jugadores que lo tengan abierto (el cliente ignora a los demás). */
	public static void refreshAll(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (canEdit(player)) {
				send(player, false);
			}
		}
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(MenuRequestPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (!canEdit(player)) {
				player.sendMessage(Text.translatable("message.dedsafio3.no_permission"), true);
				return;
			}
			send(player, true);
		});

		ServerPlayNetworking.registerGlobalReceiver(SettingPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (!canEdit(player)) {
				player.sendMessage(Text.translatable("message.dedsafio3.no_permission"), true);
				return;
			}
			applySetting(player, payload.key(), payload.value());
			send(player, false);
		});

		ServerPlayNetworking.registerGlobalReceiver(ActionPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (!canEdit(player)) {
				player.sendMessage(Text.translatable("message.dedsafio3.no_permission"), true);
				return;
			}
			runAction(player, payload.action(), payload.arg());
			send(player, false);
		});
	}

	private static void applySetting(ServerPlayerEntity player, String key, int value) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return;
		}
		DedsafioConfig config = DedsafioConfig.get();
		boolean flag = value != 0;
		switch (key) {
			case "difficulty" -> server.setDifficulty(Difficulty.byId(value), true);
			case "pvp" -> server.setPvpEnabled(flag);
			case "keep_inventory" -> server.getGameRules().get(GameRules.KEEP_INVENTORY).set(flag, server);
			case "death_animation" -> config.deathAnimation = flag;
			case "totems" -> config.totemsEnabled = flag;
			case "cooldown" -> config.cooldownSeconds = Math.max(0, Math.min(60, value));
			case "spin" -> config.spinSeconds = Math.max(3, Math.min(15, value));
			case "reviil_seconds" -> config.reviilSeconds = Math.max(20, Math.min(600, value));
			default -> {
				if (key.startsWith("weight.")) {
					RouletteColor color = RouletteColor.byId(key.substring("weight.".length()));
					if (color != null) {
						config.weights[color.ordinal()] = Math.max(0, Math.min(20, value));
					}
				} else if (key.startsWith("change.")) {
					DifficultyChange change = DifficultyChange.byId(key.substring("change.".length()));
					if (change != null) {
						if (flag) {
							change.activateWithRoulette(server, player);
						} else {
							change.deactivate(server);
						}
					}
				} else if (key.startsWith("modifier.")) {
					WorldModifier modifier = WorldModifier.byId(key.substring("modifier.".length()));
					if (modifier != null) {
						modifier.set(server, flag);
					}
				}
			}
		}
		DedsafioConfig.save();
	}

	private static void runAction(ServerPlayerEntity player, String action, String arg) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return;
		}
		switch (action) {
			case "spin" -> Roulette.spin(player, RouletteColor.byId(arg)); // "random" -> null -> la rueda decide
			case "death_preview" -> {
				DeathAnimation.worldEffects(player.getServerWorld(), player);
				DeathAnimation.broadcast(player);
			}
			case "clear_modifiers" -> WorldModifier.clearAll(server);
			case "notify" -> {
				DifficultyChange change = DifficultyChange.byId(arg);
				if (change != null) {
					// "Avisar": activa el cambio sin ruleta y lo anuncia en el chat.
					if (!change.isActive()) {
						change.setActive(server, true, player);
					}
					change.broadcastNotice(server);
					refreshAll(server);
				}
			}
			default -> {
			}
		}
	}
}
