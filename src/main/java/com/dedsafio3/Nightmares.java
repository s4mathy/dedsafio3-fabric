package com.dedsafio3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Pesadillas e Insomnia. El nivel de Insomnia sale del tiempo sin dormir (el mismo contador que usan los Phantoms):
 * Bajo (1 día), Medio (2 días) y Alto (3 días). Con el cambio "Pesadillas" activo cada jugador recibe un Indicador de
 * Insomnia y las pesadillas aparecen según el nivel. Con Insomnia Alta y el cambio del Dáctylo Adulto activo,
 * aparecen Dáctylos Adultos en el cielo. Dormir lo resetea todo.
 */
public final class Nightmares {
	public enum Level {
		NONE(Formatting.GRAY), LOW(Formatting.YELLOW), MEDIUM(Formatting.GOLD), HIGH(Formatting.RED);

		final Formatting color;

		Level(Formatting color) {
			this.color = color;
		}
	}

	private Nightmares() {
	}

	public static Level levelOf(ServerPlayerEntity player) {
		int ticks = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST));
		if (ticks >= 72000) {
			return Level.HIGH;
		} else if (ticks >= 48000) {
			return Level.MEDIUM;
		} else if (ticks >= 24000) {
			return Level.LOW;
		}
		return Level.NONE;
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 100 == 0) {
				tick(server);
			}
			if (server.getTicks() % 10 == 0) {
				// Quien duerme lo suficiente hace desaparecer a todos los Dáctylos Adultos.
				for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
					if (player.isSleepingLongEnough()) {
						for (ServerWorld w : server.getWorlds()) {
							for (DactiloAdultEntity adult : w.getEntitiesByType(ModEntities.DACTILO_ADULT, e -> true)) {
								adult.discard();
							}
						}
						break;
					}
				}
			}
		});
	}

	private static void tick(MinecraftServer server) {
		boolean nightmares = DifficultyChange.PESADILLAS.isActive();
		boolean adults = DifficultyChange.DACTILO_ADULTO.isActive();
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (!player.isAlive() || player.isSpectator()) {
				continue;
			}
			Level level = levelOf(player);
			updateIndicator(player, level, nightmares);
			ServerWorld world = player.getServerWorld();
			Random random = world.getRandom();
			if (nightmares && level != Level.NONE) {
				if (random.nextFloat() < 0.3f) {
					world.playSound(null, player.getBlockPos(), SoundEvents.AMBIENT_CAVE.value(), SoundCategory.AMBIENT, 1.0f, 0.5f);
				}
				if (level.ordinal() >= Level.MEDIUM.ordinal() && random.nextFloat() < 0.35f
						&& world.getEntitiesByClass(PhantomEntity.class, player.getBoundingBox().expand(48.0), e -> true).size() < 3) {
					BlockPos at = player.getBlockPos().add(random.nextBetween(-8, 8), random.nextBetween(8, 14), random.nextBetween(-8, 8));
					EntityType.PHANTOM.spawn(world, at, SpawnReason.EVENT);
				}
				if (level == Level.HIGH && random.nextFloat() < 0.3f) {
					player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 120, 0));
				}
			}
			if (adults && level == Level.HIGH && world.isSkyVisible(player.getBlockPos()) && random.nextFloat() < 0.3f
					&& world.getEntitiesByClass(DactiloAdultEntity.class, player.getBoundingBox().expand(96.0), e -> true).size() < 2) {
				BlockPos at = player.getBlockPos().add(random.nextBetween(-14, 14), 18, random.nextBetween(-14, 14));
				if (world.getBlockState(at).isAir()) {
					ModEntities.DACTILO_ADULT.spawn(world, at, SpawnReason.EVENT);
				}
			}
		}
	}

	/** Mantiene un único Indicador de Insomnia en el inventario con el nivel actual; sin el cambio, lo retira. */
	private static void updateIndicator(ServerPlayerEntity player, Level level, boolean enabled) {
		ItemStack found = ItemStack.EMPTY;
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (stack.isOf(ModItems.INDICADOR_INSOMNIA)) {
				if (!enabled || !found.isEmpty()) {
					stack.setCount(0);
				} else {
					found = stack;
				}
			}
		}
		if (!enabled) {
			return;
		}
		if (found.isEmpty()) {
			found = new ItemStack(ModItems.INDICADOR_INSOMNIA);
			player.getInventory().insertStack(found);
		}
		found.set(DataComponentTypes.CUSTOM_NAME, Text.translatable("item.dedsafio3.indicador_insomnia.level."
				+ level.name().toLowerCase()).formatted(level.color));
	}
}
