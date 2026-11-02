package com.dedsafio3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Cuchara de la Resurrección. Con el cambio activo, quien muere se convierte en un alma (queda de espectador).
 * En una Fogata del Ritual, clic derecho con la Cuchara abre el Ritual: una lista con los jugadores muertos; al elegir
 * uno, la Cuchara y el alma de quien hace el ritual (cada jugador tiene una sola) se gastan y el muerto vuelve a la
 * vida junto a la fogata. La Cuchara debe usarse el mismo día
 * (del juego) en que fue reclamada.
 */
public final class Resurrection {
	private static final String CLAIM_KEY = "dedsafio3_claim_day";

	private record Pending(RegistryKey<World> world, BlockPos pos) {
	}

	/** Fogata donde cada jugador abrió el Ritual. */
	private static final Map<UUID, Pending> RITUALS = new HashMap<>();

	private Resurrection() {
	}

	/** Morir siempre deja al jugador como alma (espectador); no depende de ningún cambio. */
	private static boolean active() {
		return true;
	}

	/** El cambio de la Cuchara solo decide si el ritual debe hacerse el mismo día en que se reclamó. */
	private static boolean sameDayRule() {
		return DifficultyChange.RESURRECTION_SPOON.isActive();
	}

	private static long today(MinecraftServer server) {
		return server.getOverworld().getTimeOfDay() / 24000L;
	}

	static long claimDay(ItemStack stack) {
		NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (data == null) {
			return -1;
		}
		NbtCompound nbt = data.copyNbt();
		return nbt.contains(CLAIM_KEY) ? nbt.getLong(CLAIM_KEY) : -1;
	}

	static void stamp(ItemStack stack, long day) {
		NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
		NbtCompound nbt = data == null ? new NbtCompound() : data.copyNbt();
		nbt.putLong(CLAIM_KEY, day);
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
	}

	public static void register() {
		// Una Cuchara se "reclama" el día en que un jugador la tiene por primera vez.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 20 != 0) {
				return;
			}
			long day = today(server);
			if (server.getTicks() % 40 == 0) {
				syncAll(server);
			}
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				// Quien es un alma siempre está de espectador (por si el cambio de modo al reaparecer no se aplicó).
				if (active() && player.isAlive() && !player.isSpectator()
						&& DedsafioConfig.get().souls.containsKey(player.getUuidAsString())) {
					player.changeGameMode(GameMode.SPECTATOR);
				}
				for (int i = 0; i < player.getInventory().size(); i++) {
					ItemStack stack = player.getInventory().getStack(i);
					if (stack.isOf(ModItems.CUCHARA_RESURRECCION) && claimDay(stack) < 0) {
						stamp(stack, day);
					}
				}
			}
			releaseSouls(server);
		});

		// Al morir con el cambio activo, el jugador se convierte en un alma.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (active() && entity instanceof ServerPlayerEntity player) {
				DedsafioConfig.get().souls.put(player.getUuid().toString(), player.getGameProfile().getName());
				DedsafioConfig.save();
				player.getServer().getPlayerManager().broadcast(
						Text.translatable("message.dedsafio3.soul_taken", player.getDisplayName()), false);
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> onSpawn(newPlayer));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onSpawn(handler.getPlayer()));

		// Ritual: clic derecho en una fogata encendida con la Cuchara.
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (hand != Hand.MAIN_HAND || !player.getStackInHand(hand).isOf(ModItems.CUCHARA_RESURRECCION)) {
				return ActionResult.PASS;
			}
			BlockState state = world.getBlockState(hit.getBlockPos());
			if (!state.isOf(ModBlocks.RITUAL_CAMPFIRE)) {
				return ActionResult.PASS;
			}
			if (player instanceof ServerPlayerEntity sp) {
				openRitual(sp, hit.getBlockPos());
			}
			return ActionResult.SUCCESS;
		});

		ServerPlayNetworking.registerGlobalReceiver(RevivePayload.ID, (payload, context) ->
				revive(context.player(), payload.uuid()));

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> RITUALS.clear());
	}

	private static void openRitual(ServerPlayerEntity player, BlockPos campfire) {
		Map<String, String> souls = DedsafioConfig.get().souls;
		if (DedsafioConfig.get().spentSouls.contains(player.getUuidAsString())) {
			player.sendMessage(Text.translatable("message.dedsafio3.ritual_soul_spent"), true);
			return;
		}
		if (souls.isEmpty()) {
			player.sendMessage(Text.translatable("message.dedsafio3.ritual_no_souls"), true);
			return;
		}
		StringBuilder list = new StringBuilder();
		for (Map.Entry<String, String> soul : souls.entrySet()) {
			if (list.length() > 0) {
				list.append('\n');
			}
			list.append(soul.getKey()).append('|').append(soul.getValue());
		}
		RITUALS.put(player.getUuid(), new Pending(player.getServerWorld().getRegistryKey(), campfire.toImmutable()));
		ServerPlayNetworking.send(player, new RitualPayload(list.toString()));
	}

	private static void revive(ServerPlayerEntity reviver, String uuidText) {
		MinecraftServer server = reviver.getServer();
		Pending ritual = RITUALS.get(reviver.getUuid());
		DedsafioConfig config = DedsafioConfig.get();
		if (ritual == null || !config.souls.containsKey(uuidText)) {
			return;
		}
		ServerWorld world = server.getWorld(ritual.world());
		if (world == null || !world.getBlockState(ritual.pos()).isOf(ModBlocks.RITUAL_CAMPFIRE)
				|| reviver.getServerWorld() != world || reviver.squaredDistanceTo(ritual.pos().toCenterPos()) > 100.0) {
			reviver.sendMessage(Text.translatable("message.dedsafio3.ritual_far"), true);
			return;
		}
		ItemStack spoon = reviver.getMainHandStack();
		if (!spoon.isOf(ModItems.CUCHARA_RESURRECCION)) {
			return;
		}
		if (config.spentSouls.contains(reviver.getUuidAsString())) {
			reviver.sendMessage(Text.translatable("message.dedsafio3.ritual_soul_spent"), true);
			return;
		}
		RITUALS.remove(reviver.getUuid());
		// La Cuchara se gasta siempre; si ya no es del día en que fue reclamada, el Ritual falla.
		boolean fresh = !sameDayRule() || claimDay(spoon) == today(server);
		spoon.decrement(1);
		if (!fresh) {
			reviver.sendMessage(Text.translatable("message.dedsafio3.ritual_expired"), false);
			return;
		}
		config.spentSouls.add(reviver.getUuidAsString()); // el alma de quien hace el ritual se gasta
		String name = config.souls.remove(uuidText);
		Vec3 spot = new Vec3(ritual.pos().getX() + 0.5, ritual.pos().getY() + 1.0, ritual.pos().getZ() + 0.5);
		ServerPlayerEntity target = server.getPlayerManager().getPlayer(UUID.fromString(uuidText));
		if (target != null && target.isAlive()) {
			applyRevive(target, world, spot);
		} else {
			// Offline o todavía en la pantalla de muerte: vuelve a la vida junto a la fogata cuando aparezca.
			config.pendingRevive.put(uuidText, world.getRegistryKey().getValue() + ";" + spot.x + ";" + spot.y + ";" + spot.z);
		}
		DedsafioConfig.save();
		syncAll(server);
		server.getPlayerManager().broadcast(Text.translatable("message.dedsafio3.ritual_done", name, reviver.getDisplayName()), false);
	}

	private record Vec3(double x, double y, double z) {
	}

	private static void applyRevive(ServerPlayerEntity target, ServerWorld world, Vec3 spot) {
		target.changeGameMode(GameMode.SURVIVAL);
		target.teleport(world, spot.x(), spot.y(), spot.z(), target.getYaw(), target.getPitch());
		target.setHealth(target.getMaxHealth());
		target.getHungerManager().setFoodLevel(20);
		target.fallDistance = 0f;
		world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, spot.x(), spot.y() + 0.5, spot.z(), 40, 0.4, 0.6, 0.4, 0.03);
		world.playSound(null, BlockPos.ofFloored(spot.x(), spot.y(), spot.z()), SoundEvents.BLOCK_BEACON_ACTIVATE,
				SoundCategory.PLAYERS, 1.0f, 1.4f);
	}

	/** Al reaparecer o conectarse: aplica un ritual pendiente, o deja de espectador a quien sigue siendo un alma. */
	private static void onSpawn(ServerPlayerEntity player) {
		DedsafioConfig config = DedsafioConfig.get();
		syncAll(player.getServer());
		String key = player.getUuid().toString();
		String pending = config.pendingRevive.get(key);
		if (pending != null) {
			config.pendingRevive.remove(key);
			DedsafioConfig.save();
			String[] parts = pending.split(";");
			Identifier dim = Identifier.tryParse(parts[0]);
			ServerWorld world = dim == null ? null : player.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, dim));
			if (world != null && parts.length == 4) {
				applyRevive(player, world, new Vec3(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3])));
				return;
			}
		}
		if (config.souls.containsKey(key)) {
			if (active()) {
				player.changeGameMode(GameMode.SPECTATOR);
			} else {
				config.souls.remove(key);
				DedsafioConfig.save();
				player.changeGameMode(GameMode.SURVIVAL);
			}
		}
	}

	/** Envía a todos qué jugadores conservan su alma (icono junto al nombre). */
	public static void syncAll(MinecraftServer server) {
		DedsafioConfig config = DedsafioConfig.get();
		java.util.Set<UUID> holders = new java.util.HashSet<>();
		if (active()) {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				String id = player.getUuidAsString();
				if (!config.spentSouls.contains(id) && !config.souls.containsKey(id)) {
					holders.add(player.getUuid());
				}
			}
		}
		SoulIcons.set(holders);
		String csv = String.join(",", holders.stream().map(UUID::toString).toList());
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			ServerPlayNetworking.send(player, new SoulsPayload(csv));
		}
	}

	/** Si el cambio se desactiva, las almas conectadas vuelven a jugar. */
	private static void releaseSouls(MinecraftServer server) {
		DedsafioConfig config = DedsafioConfig.get();
		if (active() || config.souls.isEmpty()) {
			return;
		}
		Iterator<String> it = config.souls.keySet().iterator();
		boolean changed = false;
		while (it.hasNext()) {
			ServerPlayerEntity online = server.getPlayerManager().getPlayer(UUID.fromString(it.next()));
			if (online != null) {
				online.changeGameMode(GameMode.SURVIVAL);
				it.remove();
				changed = true;
			}
		}
		if (changed) {
			DedsafioConfig.save();
		}
	}
}
