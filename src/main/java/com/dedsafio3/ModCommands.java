package com.dedsafio3;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.List;

/**
 * Comandos de administración (nivel 2):
 * <pre>
 *   /dedsafio menu                          abre el menú interactivo (también con la tecla K)
 *   /dedsafio ruleta &lt;color|random&gt; [jugadores]   gira la rueda (random = la rueda decide)
 *   /dedsafio muerte [jugadores]            reproduce la animación de muerte sin matar
 *   /dedsafio quitar_modificaciones          quita todas las modificaciones del mundo
 * </pre>
 */
public final class ModCommands {
	private ModCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("dedsafio")
						.requires(source -> source.hasPermissionLevel(2))
						.then(CommandManager.literal("menu")
								.executes(context -> {
									MenuService.send(context.getSource().getPlayerOrThrow(), true);
									return 1;
								}))
						.then(CommandManager.literal("ruleta")
								.then(CommandManager.argument("color", StringArgumentType.word())
										.suggests((context, builder) -> CommandSource.suggestMatching(RouletteColor.idsWithRandom(), builder))
										.executes(context -> spin(context, List.of(context.getSource().getPlayerOrThrow())))
										.then(CommandManager.argument("jugadores", EntityArgumentType.players())
												.executes(context -> spin(context, EntityArgumentType.getPlayers(context, "jugadores"))))))
						.then(CommandManager.literal("muerte")
								.executes(context -> deathPreview(context, List.of(context.getSource().getPlayerOrThrow())))
								.then(CommandManager.argument("jugadores", EntityArgumentType.players())
										.executes(context -> deathPreview(context, EntityArgumentType.getPlayers(context, "jugadores")))))
						.then(CommandManager.literal("alma")
								.then(CommandManager.argument("jugador", EntityArgumentType.player())
										.executes(context -> {
											ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "jugador");
											boolean given = Resurrection.giveSoul(context.getSource().getServer(), target.getUuid());
											context.getSource().sendFeedback(() -> Text.translatable(given
													? "command.dedsafio3.soul_given" : "command.dedsafio3.soul_already", target.getDisplayName()), true);
											return given ? 1 : 0;
										})))
						.then(CommandManager.literal("revivir")
								.then(CommandManager.argument("jugador", StringArgumentType.word())
										.suggests((context, builder) -> CommandSource.suggestMatching(
												DedsafioConfig.get().souls.values(), builder))
										.executes(ModCommands::revive)))
						.then(CommandManager.literal("quitar_modificaciones")
								.executes(context -> {
									WorldModifier.clearAll(context.getSource().getServer());
									context.getSource().sendFeedback(() -> Text.translatable("message.dedsafio3.truce"), true);
									return 1;
								}))));
	}

	private static int revive(CommandContext<ServerCommandSource> context) {
		String name = StringArgumentType.getString(context, "jugador");
		String uuid = null;
		for (Map.Entry<String, String> soul : DedsafioConfig.get().souls.entrySet()) {
			if (soul.getValue().equalsIgnoreCase(name)) {
				uuid = soul.getKey();
			}
		}
		if (uuid == null) {
			context.getSource().sendError(Text.translatable("command.dedsafio3.not_dead", name));
			return 0;
		}
		ServerCommandSource source = context.getSource();
		Resurrection.reviveAt(source.getServer(), uuid, source.getWorld(), source.getPosition().x, source.getPosition().y, source.getPosition().z);
		source.sendFeedback(() -> Text.translatable("command.dedsafio3.revived", name), true);
		return 1;
	}

	private static int spin(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players) throws CommandSyntaxException {
		String colorId = StringArgumentType.getString(context, "color");
		boolean random = colorId.equalsIgnoreCase("random");
		RouletteColor color = random ? null : RouletteColor.byId(colorId);
		if (!random && color == null) {
			context.getSource().sendError(Text.translatable("command.dedsafio3.unknown_color", colorId));
			return 0;
		}
		int started = 0;
		for (ServerPlayerEntity player : players) {
			if (Roulette.spin(player, color)) {
				started++;
			}
		}
		int count = started;
		Text colorName = random ? Text.translatable("gui.dedsafio3.random") : Text.translatable(color.nameKey());
		context.getSource().sendFeedback(() -> Text.translatable("command.dedsafio3.spin", count, colorName), true);
		return started;
	}

	private static int deathPreview(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players) {
		for (ServerPlayerEntity player : players) {
			DeathAnimation.worldEffects(player.getServerWorld(), player);
			DeathAnimation.broadcast(player);
		}
		int count = players.size();
		context.getSource().sendFeedback(() -> Text.translatable("command.dedsafio3.death", count), true);
		return count;
	}
}
