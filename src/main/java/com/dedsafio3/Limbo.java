package com.dedsafio3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * El Limbo: dimensión vacía y oscura con un Laberinto por jugador. El Reaper Pequeño destierra aquí a sus víctimas;
 * para volver hay que encontrar la salida (suelo de luz ocre en la esquina opuesta) y llegarás a donde te golpearon.
 */
public final class Limbo {
	public static final RegistryKey<World> KEY = RegistryKey.of(RegistryKeys.WORLD, Dedsafio3.id("limbo"));
	private static final int CELLS = 15;
	private static final int SIZE = CELLS * 2 + 1;
	private static final int FLOOR_Y = 3;
	private static final int WALL_H = 4;
	private static final int SPACING = 64;

	private record Return(RegistryKey<World> world, double x, double y, double z) {
	}

	private static final Map<UUID, Return> RETURNS = new HashMap<>();

	private Limbo() {
	}

	private static BlockPos origin(UUID id) {
		int slot = Math.floorMod(id.hashCode(), 256);
		return new BlockPos((slot % 16) * SPACING, FLOOR_Y, (slot / 16) * SPACING);
	}

	private static BlockPos exit(UUID id) {
		return origin(id).add(SIZE - 2, 0, SIZE - 2);
	}

	public static boolean isIn(ServerPlayerEntity player) {
		return player.getServerWorld().getRegistryKey() == KEY;
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 10 != 0) {
				return;
			}
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (isIn(player) && player.isAlive()) {
					BlockPos e = exit(player.getUuid());
					if (player.squaredDistanceTo(e.getX() + 0.5, e.getY(), e.getZ() + 0.5) < 2.5) {
						leave(server, player);
					}
				}
			}
		});
		// Las paredes del laberinto no se pueden romper.
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
				!(world.getRegistryKey() == KEY && !player.isCreative()));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> RETURNS.clear());
	}

	/** Destierra al jugador a su laberinto del Limbo. Devuelve false si no se pudo. */
	public static boolean banish(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		ServerWorld limbo = server == null ? null : server.getWorld(KEY);
		if (limbo == null || isIn(player)) {
			return false;
		}
		RETURNS.put(player.getUuid(), new Return(player.getServerWorld().getRegistryKey(), player.getX(), player.getY(), player.getZ()));
		BlockPos o = origin(player.getUuid());
		build(limbo, player.getUuid(), o);
		player.teleport(limbo, o.getX() + 1.5, o.getY(), o.getZ() + 1.5, player.getYaw(), 0f);
		player.fallDistance = 0f;
		player.sendMessage(Text.translatable("message.dedsafio3.limbo_banished"), false);
		return true;
	}

	private static void leave(MinecraftServer server, ServerPlayerEntity player) {
		Return back = RETURNS.remove(player.getUuid());
		ServerWorld world = back == null ? null : server.getWorld(back.world());
		if (world == null) {
			world = server.getOverworld();
			BlockPos spawn = world.getSpawnPos();
			player.teleport(world, spawn.getX() + 0.5, world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, spawn.getX(), spawn.getZ()),
					spawn.getZ() + 0.5, player.getYaw(), player.getPitch());
		} else {
			player.teleport(world, back.x(), back.y(), back.z(), player.getYaw(), player.getPitch());
		}
		player.fallDistance = 0f;
		player.sendMessage(Text.translatable("message.dedsafio3.limbo_exit"), false);
	}

	/** Genera un laberinto aleatorio (DFS) con paredes de blackstone, techo con luces y la salida en la esquina opuesta. */
	private static void build(ServerWorld world, UUID id, BlockPos o) {
		Random random = world.getRandom();
		boolean[][] open = new boolean[SIZE][SIZE];
		boolean[][] seen = new boolean[CELLS][CELLS];
		Deque<int[]> stack = new ArrayDeque<>();
		stack.push(new int[]{0, 0});
		seen[0][0] = true;
		open[1][1] = true;
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		while (!stack.isEmpty()) {
			int[] cur = stack.peek();
			int start = random.nextInt(4);
			boolean moved = false;
			for (int k = 0; k < 4 && !moved; k++) {
				int[] d = dirs[(start + k) % 4];
				int nx = cur[0] + d[0];
				int nz = cur[1] + d[1];
				if (nx >= 0 && nz >= 0 && nx < CELLS && nz < CELLS && !seen[nx][nz]) {
					seen[nx][nz] = true;
					open[nx * 2 + 1][nz * 2 + 1] = true;
					open[cur[0] * 2 + 1 + d[0]][cur[1] * 2 + 1 + d[1]] = true;
					stack.push(new int[]{nx, nz});
					moved = true;
				}
			}
			if (!moved) {
				stack.pop();
			}
		}
		BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
		BlockState floor = Blocks.POLISHED_BLACKSTONE.getDefaultState();
		BlockState air = Blocks.AIR.getDefaultState();
		BlockPos exit = exit(id);
		for (int x = 0; x < SIZE; x++) {
			for (int z = 0; z < SIZE; z++) {
				BlockPos base = o.add(x, 0, z);
				world.setBlockState(base.down(), floor, Block.NOTIFY_LISTENERS);
				for (int y = 0; y < WALL_H; y++) {
					world.setBlockState(base.up(y), open[x][z] ? air : wall, Block.NOTIFY_LISTENERS);
				}
				boolean light = open[x][z] && x % 4 == 1 && z % 4 == 1;
				world.setBlockState(base.up(WALL_H), light ? Blocks.SHROOMLIGHT.getDefaultState() : floor, Block.NOTIFY_LISTENERS);
			}
		}
		world.setBlockState(exit.down(), Blocks.OCHRE_FROGLIGHT.getDefaultState(), Block.NOTIFY_LISTENERS);
		world.setBlockState(exit.up(WALL_H), Blocks.SHROOMLIGHT.getDefaultState(), Block.NOTIFY_LISTENERS);
	}
}
