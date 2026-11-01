package com.dedsafio3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Aparición natural de los mobs nuevos. Se hace con un spawner propio (en vez de modificar los biomas) para que cada
 * mob se pueda activar y desactivar en caliente desde el menú.
 */
public final class ModSpawner {
	private enum Mode {
		/** Sobre el terreno, a cielo abierto. */
		SURFACE,
		/** Bajo tierra, sin cielo encima. */
		CAVE,
		/** Suelo libre cerca del jugador (Nether). */
		FLOOR,
		/** Agua del océano, a media profundidad. */
		WATER
	}

	private record Rule(DifficultyChange change, RegistryKey<World> dimension, Mode mode, int cap, int min, int max,
						double chance, boolean hostile, BiPredicate<ServerWorld, BlockPos> spot,
						List<EntityType<? extends MobEntity>> types) {
	}

	private static final List<Rule> RULES = new ArrayList<>();

	private static final List<RegistryKey<Biome>> SNOWY = List.of(BiomeKeys.SNOWY_PLAINS, BiomeKeys.ICE_SPIKES,
			BiomeKeys.SNOWY_TAIGA, BiomeKeys.SNOWY_SLOPES, BiomeKeys.GROVE, BiomeKeys.FROZEN_PEAKS,
			BiomeKeys.JAGGED_PEAKS, BiomeKeys.SNOWY_BEACH);

	private ModSpawner() {
	}

	private static boolean snowy(ServerWorld w, BlockPos p) {
		RegistryEntry<Biome> biome = w.getBiome(p);
		for (RegistryKey<Biome> key : SNOWY) {
			if (biome.matchesKey(key)) {
				return true;
			}
		}
		return false;
	}

	private static boolean jungle(ServerWorld w, BlockPos p) {
		return w.getBiome(p).isIn(BiomeTags.IS_JUNGLE);
	}

	private static boolean savannaOrPlains(ServerWorld w, BlockPos p) {
		RegistryEntry<Biome> biome = w.getBiome(p);
		return biome.isIn(BiomeTags.IS_SAVANNA) || biome.matchesKey(BiomeKeys.PLAINS) || biome.matchesKey(BiomeKeys.SUNFLOWER_PLAINS);
	}

	private static boolean dryLands(ServerWorld w, BlockPos p) {
		RegistryEntry<Biome> biome = w.getBiome(p);
		return biome.isIn(BiomeTags.IS_SAVANNA) || biome.isIn(BiomeTags.IS_BADLANDS);
	}

	private static void rule(DifficultyChange change, RegistryKey<World> dimension, Mode mode, int cap, int min, int max,
							 double chance, boolean hostile, BiPredicate<ServerWorld, BlockPos> spot,
							 List<EntityType<? extends MobEntity>> types) {
		RULES.add(new Rule(change, dimension, mode, cap, min, max, chance, hostile, spot, types));
	}

	public static void register() {
		rule(DifficultyChange.BLAZES, World.NETHER, Mode.FLOOR, 6, 1, 2, 0.5, true, (w, p) -> true,
				List.of(EntityType.BLAZE));
		rule(DifficultyChange.CREEPER_NUCLEAR, World.OVERWORLD, Mode.SURFACE, 3, 1, 1, 0.35, true, ModSpawner::snowy,
				List.of(ModEntities.NUCLEAR_CREEPER));
		rule(DifficultyChange.REPTISAURIO_SALVAJE, World.OVERWORLD, Mode.CAVE, 5, 1, 2, 0.5, true, (w, p) -> true,
				List.of(ModEntities.REPTILIANS.get(ReptilianEntity.Kind.SALVAJE)));
		rule(DifficultyChange.REPTISAURIO_GUERRERO, World.OVERWORLD, Mode.SURFACE, 4, 1, 2, 0.3, true, ModSpawner::jungle,
				List.of(ModEntities.REPTILIANS.get(ReptilianEntity.Kind.GUERRERO)));
		rule(DifficultyChange.REPTISAURIO_ARQUERO, World.OVERWORLD, Mode.SURFACE, 4, 1, 2, 0.3, true, ModSpawner::jungle,
				List.of(ModEntities.REPTILIANS.get(ReptilianEntity.Kind.ARQUERO)));
		rule(DifficultyChange.REPTISAURIO_LANZA, World.OVERWORLD, Mode.SURFACE, 4, 1, 2, 0.3, true, ModSpawner::jungle,
				List.of(ModEntities.REPTILIANS.get(ReptilianEntity.Kind.LANZA)));
		rule(DifficultyChange.PLUMOSAURIO, World.OVERWORLD, Mode.SURFACE, 1, 1, 1, 0.15, false,
				(w, p) -> w.isDay() && savannaOrPlains(w, p), List.of(ModEntities.PLUMOSAURIO));
		rule(DifficultyChange.DROMORAPTOR_ROJO, World.OVERWORLD, Mode.SURFACE, 8, 3, 5, 0.25, true, ModSpawner::dryLands,
				List.of(ModEntities.REPTILIANS.get(ReptilianEntity.Kind.DROMORAPTOR)));

		rule(DifficultyChange.DROMORAPTOR_AZUL, World.OVERWORLD, Mode.SURFACE, 8, 3, 5, 0.25, true,
				(w, p) -> w.getBiome(p).isIn(BiomeTags.IS_SAVANNA) || w.getBiome(p).matchesKey(BiomeKeys.PLAINS),
				List.of(ModEntities.REPTILIANS.get(ReptilianEntity.Kind.DROMORAPTOR_AZUL)));
		rule(DifficultyChange.DACTILO_BEBE, World.OVERWORLD, Mode.SURFACE, 4, 1, 2, 0.3, true,
				(w, p) -> w.isDay() && (w.getBiome(p).isIn(BiomeTags.IS_SAVANNA) || w.getBiome(p).isIn(BiomeTags.IS_JUNGLE)
						|| w.getBiome(p).matchesKey(BiomeKeys.PLAINS)),
				List.of(ModEntities.DACTILO));
		rule(DifficultyChange.CREEPER_MAGMA, World.OVERWORLD, Mode.SURFACE, 4, 1, 2, 0.35, true,
				(w, p) -> !w.isDay() || w.getBiome(p).isIn(BiomeTags.IS_BADLANDS),
				List.of(ModEntities.MAGMA_CREEPER));
		rule(DifficultyChange.NARVAL, World.OVERWORLD, Mode.WATER, 3, 1, 2, 0.3, false,
				(w, p) -> w.getBiome(p).isIn(BiomeTags.IS_OCEAN), List.of(ModEntities.NARVAL));
		rule(DifficultyChange.HEROBRINE, World.OVERWORLD, Mode.SURFACE, 1, 1, 1, 0.04, true,
				(w, p) -> !w.isDay(), List.of(ModEntities.HEROBRINE));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 40 != 0) {
				return;
			}
			for (ServerWorld world : server.getWorlds()) {
				for (Rule rule : RULES) {
					if (rule.dimension() != world.getRegistryKey() || !rule.change().isActive()) {
						continue;
					}
					if (rule.hostile() && world.getDifficulty() == Difficulty.PEACEFUL) {
						continue;
					}
					for (ServerPlayerEntity player : new ArrayList<>(world.getPlayers())) {
						if (!player.isSpectator()) {
							trySpawn(world, player, rule);
						}
					}
				}
			}
		});
	}

	private static void trySpawn(ServerWorld world, ServerPlayerEntity player, Rule rule) {
		Random random = world.getRandom();
		if (random.nextDouble() > rule.chance()) {
			return;
		}
		Box area = player.getBoundingBox().expand(96.0);
		int existing = 0;
		for (EntityType<? extends MobEntity> type : rule.types()) {
			existing += world.getEntitiesByType(type, area, e -> true).size();
		}
		if (existing >= rule.cap()) {
			return;
		}
		for (int attempt = 0; attempt < 3; attempt++) {
			BlockPos pos = findSpot(world, player, rule, random);
			if (pos == null) {
				continue;
			}
			int count = rule.min() + random.nextInt(rule.max() - rule.min() + 1);
			for (int i = 0; i < count; i++) {
				EntityType<? extends MobEntity> type = rule.types().get(random.nextInt(rule.types().size()));
				type.spawn(world, pos.add(random.nextBetween(-2, 2), 0, random.nextBetween(-2, 2)), SpawnReason.NATURAL);
			}
			return;
		}
	}

	private static BlockPos findSpot(ServerWorld w, ServerPlayerEntity player, Rule rule, Random random) {
		double angle = random.nextDouble() * Math.PI * 2.0;
		double dist = 24.0 + random.nextDouble() * 24.0;
		int x = MathHelper.floor(player.getX() + Math.cos(angle) * dist);
		int z = MathHelper.floor(player.getZ() + Math.sin(angle) * dist);
		if (!w.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
			return null;
		}

		if (rule.mode() == Mode.WATER) {
			BlockPos wp = new BlockPos(x, Math.max(w.getBottomY() + 4, w.getSeaLevel() - 4), z);
			if (!w.getFluidState(wp).isIn(net.minecraft.registry.tag.FluidTags.WATER)
					|| !w.getFluidState(wp.up()).isIn(net.minecraft.registry.tag.FluidTags.WATER)
					|| w.getClosestPlayer(wp.getX() + 0.5, wp.getY(), wp.getZ() + 0.5, 20.0, false) != null) {
				return null;
			}
			return rule.spot().test(w, wp) ? wp : null;
		}

		BlockPos pos = null;
		if (rule.mode() == Mode.SURFACE) {
			pos = new BlockPos(x, w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z);
		} else {
			int bottom = Math.max(w.getBottomY() + 1, player.getBlockY() - 24);
			for (int y = player.getBlockY() + 8; y >= bottom; y--) {
				BlockPos c = new BlockPos(x, y, z);
				if (w.getBlockState(c).isAir() && w.getBlockState(c.up()).isAir()
						&& w.getBlockState(c.down()).isSolidBlock(w, c.down())) {
					pos = c;
					break;
				}
			}
		}
		if (pos == null) {
			return null;
		}
		if (!w.getBlockState(pos.down()).isSolidBlock(w, pos.down())
				|| !w.getBlockState(pos).isAir() || !w.getBlockState(pos.up()).isAir()) {
			return null;
		}
		if (!w.getFluidState(pos).isEmpty() || !w.getFluidState(pos.down()).isEmpty()) {
			return null;
		}
		if (rule.mode() == Mode.CAVE && (w.isSkyVisible(pos) || pos.getY() > 50)) {
			return null;
		}
		if (rule.mode() == Mode.SURFACE && !w.isSkyVisible(pos)) {
			return null;
		}
		if (w.getClosestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 20.0, false) != null) {
			return null;
		}
		return rule.spot().test(w, pos) ? pos : null;
	}
}
