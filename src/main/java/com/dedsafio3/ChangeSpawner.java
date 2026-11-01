package com.dedsafio3;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.util.List;

/**
 * Aparición natural de los mobs de los cambios. Se hace con un "spawner" propio (cada 5 s, cerca de cada jugador)
 * para que el menú pueda activar y desactivar cada mob sin reiniciar nada.
 */
public final class ChangeSpawner {
	private enum Mode {NETHER, SURFACE, CAVE}

	private record Rule(ChangeType change, EntityType<? extends MobEntity> type, int cap, int min, int max,
						Mode mode, List<RegistryKey<Biome>> biomes) {
	}

	private static final List<RegistryKey<Biome>> NONE = List.of();
	private static final List<RegistryKey<Biome>> SNOW = List.of(BiomeKeys.SNOWY_PLAINS, BiomeKeys.SNOWY_TAIGA,
			BiomeKeys.SNOWY_SLOPES, BiomeKeys.GROVE, BiomeKeys.FROZEN_PEAKS, BiomeKeys.ICE_SPIKES, BiomeKeys.JAGGED_PEAKS);
	private static final List<RegistryKey<Biome>> SAVANNA = List.of(BiomeKeys.SAVANNA, BiomeKeys.SAVANNA_PLATEAU,
			BiomeKeys.WINDSWEPT_SAVANNA);
	private static final List<RegistryKey<Biome>> DRY = List.of(BiomeKeys.DESERT, BiomeKeys.BADLANDS,
			BiomeKeys.ERODED_BADLANDS, BiomeKeys.WOODED_BADLANDS, BiomeKeys.SAVANNA, BiomeKeys.SAVANNA_PLATEAU);
	private static final List<RegistryKey<Biome>> JUNGLE = List.of(BiomeKeys.JUNGLE, BiomeKeys.SPARSE_JUNGLE,
			BiomeKeys.BAMBOO_JUNGLE);

	private static List<Rule> rules = List.of();

	private ChangeSpawner() {
	}

	/** Se llama después de registrar las entidades. */
	public static void init() {
		rules = List.of(
				new Rule(ChangeType.BLAZES_NETHER, EntityType.BLAZE, 5, 1, 2, Mode.NETHER, NONE),
				new Rule(ChangeType.NUCLEAR_CREEPER, ModEntities.NUCLEAR_CREEPER, 2, 1, 1, Mode.SURFACE, SNOW),
				new Rule(ChangeType.REPTISAURIO_SALVAJE, ModEntities.REPTISAURIO_SALVAJE, 6, 1, 2, Mode.CAVE, NONE),
				new Rule(ChangeType.PLUMOSAURIO, ModEntities.PLUMOSAURIO, 2, 1, 1, Mode.SURFACE, SAVANNA),
				new Rule(ChangeType.DROMORAPTOR_ROJO, ModEntities.DROMORAPTOR_ROJO, 8, 3, 4, Mode.SURFACE, DRY),
				new Rule(ChangeType.REPTISAURIO_GUERRERO, ModEntities.REPTISAURIO_GUERRERO, 4, 1, 2, Mode.SURFACE, JUNGLE),
				new Rule(ChangeType.REPTISAURIO_ARQUERO, ModEntities.REPTISAURIO_ARQUERO, 4, 1, 2, Mode.SURFACE, JUNGLE),
				new Rule(ChangeType.REPTISAURIO_LANZA, ModEntities.REPTISAURIO_LANZA, 4, 1, 2, Mode.SURFACE, JUNGLE)
		);
	}

	public static void tick(MinecraftServer server) {
		if (server.getTicks() % 100 != 0) {
			return;
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (player.isSpectator()) {
				continue;
			}
			ServerWorld world = player.getServerWorld();
			for (Rule rule : rules) {
				if (!rule.change().isActive() || !dimensionOk(world, rule.mode())) {
					continue;
				}
				if (world.getRandom().nextFloat() > 0.45f) {
					continue;
				}
				trySpawn(world, player, rule);
			}
		}
	}

	private static boolean dimensionOk(ServerWorld world, Mode mode) {
		return mode == Mode.NETHER ? world.getRegistryKey() == World.NETHER : world.getRegistryKey() == World.OVERWORLD;
	}

	private static void trySpawn(ServerWorld world, ServerPlayerEntity player, Rule rule) {
		Random random = world.getRandom();
		Box area = new Box(player.getBlockPos()).expand(80);
		if (world.getEntitiesByType(rule.type(), area, entity -> true).size() >= rule.cap()) {
			return;
		}
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = 24.0 + random.nextDouble() * 24.0;
			int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
			int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			BlockPos pos = switch (rule.mode()) {
				case SURFACE -> surface(world, x, z);
				case CAVE -> standable(world, x, Math.min(player.getBlockY() + 6, 40), z,
						Math.max(world.getBottomY() + 2, player.getBlockY() - 24));
				case NETHER -> standable(world, x, player.getBlockY() + 8, z, player.getBlockY() - 12);
			};
			if (pos == null || !valid(world, pos, rule)) {
				continue;
			}
			int group = rule.min() + random.nextInt(rule.max() - rule.min() + 1);
			for (int i = 0; i < group; i++) {
				BlockPos spot = pos.add(random.nextBetween(-2, 2), 0, random.nextBetween(-2, 2));
				if (!world.getBlockState(spot).isAir() || !world.getBlockState(spot.up()).isAir()
						|| !world.getBlockState(spot.down()).isSolidBlock(world, spot.down())) {
					spot = pos;
				}
				rule.type().spawn(world, spot, SpawnReason.NATURAL);
			}
			return;
		}
	}

	private static BlockPos surface(ServerWorld world, int x, int z) {
		int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos pos = new BlockPos(x, y, z);
		BlockState below = world.getBlockState(pos.down());
		if (!below.isSolidBlock(world, pos.down()) || !world.getFluidState(pos).isEmpty()) {
			return null;
		}
		return pos;
	}

	/** Primer hueco de 2 bloques de alto con suelo sólido, buscando hacia abajo desde yStart hasta yMin. */
	private static BlockPos standable(ServerWorld world, int x, int yStart, int z, int yMin) {
		for (int y = yStart; y >= yMin; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			if (world.getBlockState(pos).isAir() && world.getBlockState(pos.up()).isAir()
					&& world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
				return pos;
			}
		}
		return null;
	}

	private static boolean valid(ServerWorld world, BlockPos pos, Rule rule) {
		if (world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 16.0, false) != null) {
			return false;
		}
		if (!rule.biomes().isEmpty()) {
			RegistryEntry<Biome> biome = world.getBiome(pos);
			boolean match = false;
			for (RegistryKey<Biome> key : rule.biomes()) {
				if (biome.matchesKey(key)) {
					match = true;
					break;
				}
			}
			if (!match) {
				return false;
			}
		}
		if (rule.mode() == Mode.CAVE) {
			return !world.isSkyVisible(pos) && world.getLightLevel(pos) <= 7;
		}
		return true;
	}
}
