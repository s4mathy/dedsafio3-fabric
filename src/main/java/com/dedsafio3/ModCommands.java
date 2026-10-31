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
import java.util.List;

/**
 * Comandos de administración (nivel 2):
 * <pre>
 *   /dedsafio ruleta &lt;color&gt; [jugadores]   gira una ruleta
 *   /dedsafio muerte [jugadores]           reproduce la animación de muerte sin matar
 * </pre>
 */
public final class ModCommands {
	private ModCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("dedsafio")
						.requires(source -> source.hasPermissionLevel(2))
						.then(CommandManager.literal("ruleta")
								.then(CommandManager.argument("color", StringArgumentType.word())
										.suggests((context, builder) -> CommandSource.suggestMatching(RouletteColor.ids(), builder))
										.executes(context -> spin(context, List.of(context.getSource().getPlayerOrThrow())))
										.then(CommandManager.argument("jugadores", EntityArgumentType.players())
												.executes(context -> spin(context, EntityArgumentType.getPlayers(context, "jugadores"))))))
						.then(CommandManager.literal("muerte")
								.executes(context -> deathPreview(context, List.of(context.getSource().getPlayerOrThrow())))
								.then(CommandManager.argument("jugadores", EntityArgumentType.players())
										.executes(context -> deathPreview(context, EntityArgumentType.getPlayers(context, "jugadores")))))));
	}

	private static int spin(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players) throws CommandSyntaxException {
		String colorId = StringArgumentType.getString(context, "color");
		RouletteColor color = RouletteColor.byId(colorId);
		if (color == null) {
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
		context.getSource().sendFeedback(() -> Text.translatable("command.dedsafio3.spin", count, Text.translatable(color.blockTranslationKey())), true);
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
