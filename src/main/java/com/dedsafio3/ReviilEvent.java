package com.dedsafio3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Momento Reviil: Reviil quiere lanzar un rayo mortal y Eón construyó un búnker. Antes de que termine el contador
 * todos deben estar dentro o morirán. Además, al empezar se intercambian al azar las posiciones de todos los
 * jugadores y se les oculta la vida mientras dura. Solo afecta a los jugadores de la dimensión del búnker.
 */
public final class ReviilEvent {
	private static boolean running;
	private static long endTick;
	private static int totalTicks;
	private static ServerBossBar bar;
	private static ServerWorld bunkerWorld;
	private static Box bunkerBox;

	private ReviilEvent() {
	}

	public static boolean isRunning() {
		return running;
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!running) {
				return;
			}
			long left = endTick - server.getTicks();
			if (left <= 0) {
				strike(server);
				return;
			}
			bar.setPercent(Math.max(0f, Math.min(1f, left / (float) totalTicks)));
			if (left % 20 == 0) {
				bar.setName(Text.translatable("message.dedsafio3.reviil_bar", left / 20));
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			running = false;
			bar = null;
			bunkerWorld = null;
			bunkerBox = null;
			DifficultyChange.resetRuntime();
		});
	}

	public static void start(MinecraftServer server, ServerPlayerEntity source) {
		if (running) {
			return;
		}
		if (source == null || source.isRemoved()) {
			List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
			if (players.isEmpty()) {
				return;
			}
			source = players.get(0);
		}
		ServerWorld world = source.getServerWorld();
		Direction facing = source.getHorizontalFacing();
		BlockPos center = source.getBlockPos().offset(facing, 14);
		int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ());
		BlockPos origin = new BlockPos(center.getX() - 4, top - 1, center.getZ() - 4);
		buildBunker(world, origin);

		bunkerWorld = world;
		bunkerBox = new Box(origin.getX() + 1, origin.getY() + 1, origin.getZ() + 1,
				origin.getX() + 8, origin.getY() + 4, origin.getZ() + 8);
		int seconds = DedsafioConfig.get().reviilSeconds;
		totalTicks = seconds * 20;
		endTick = server.getTicks() + totalTicks;
		running = true;

		bar = new ServerBossBar(Text.translatable("message.dedsafio3.reviil_bar", seconds), BossBar.Color.RED, BossBar.Style.PROGRESS);
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			bar.addPlayer(player);
			ServerPlayNetworking.send(player, new ReviilPayload(seconds));
		}
		world.playSound(null, center, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 4.0f, 0.7f);
		server.getPlayerManager().broadcast(Text.translatable("message.dedsafio3.reviil_bunker",
				center.getX(), center.getY(), center.getZ()), false);
		swapPositions(world);
	}

	/** Cancela el evento sin rayo. */
	public static void stop(MinecraftServer server) {
		if (!running) {
			return;
		}
		finish(server);
		server.getPlayerManager().broadcast(Text.translatable("message.dedsafio3.reviil_cancel"), false);
	}

	private static void finish(MinecraftServer server) {
		running = false;
		if (bar != null) {
			bar.clearPlayers();
			bar = null;
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			ServerPlayNetworking.send(player, new ReviilPayload(0));
		}
		MenuService.refreshAll(server);
	}

	/** El conjuro: intercambia al azar las posiciones de todos los jugadores de la dimensión. */
	private static void swapPositions(ServerWorld world) {
		List<ServerPlayerEntity> players = new ArrayList<>();
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (!player.isSpectator() && player.isAlive()) {
				players.add(player);
			}
		}
		int n = players.size();
		if (n < 2) {
			return;
		}
		List<Vec3d> spots = new ArrayList<>();
		for (ServerPlayerEntity player : players) {
			spots.add(player.getPos());
		}
		Collections.shuffle(spots, new java.util.Random());
		int shift = 1 + world.getRandom().nextInt(n - 1);
		for (int i = 0; i < n; i++) {
			Vec3d spot = spots.get((i + shift) % n);
			players.get(i).requestTeleport(spot.x, spot.y, spot.z);
		}
	}

	/** El rayo: mata a todo el que no esté dentro del búnker. */
	private static void strike(MinecraftServer server) {
		ServerWorld world = bunkerWorld;
		Box box = bunkerBox;
		finish(server);
		if (world == null || box == null) {
			return;
		}
		server.getPlayerManager().broadcast(Text.translatable("message.dedsafio3.reviil_strike"), false);
		for (ServerPlayerEntity player : new ArrayList<>(world.getPlayers())) {
			if (!player.isAlive() || player.isCreative() || player.isSpectator() || box.contains(player.getPos())) {
				continue;
			}
			LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
			if (bolt != null) {
				bolt.refreshPositionAfterTeleport(player.getX(), player.getY(), player.getZ());
				world.spawnEntity(bolt);
			}
			player.damage(world.getDamageSources().lightningBolt(), 1000.0f);
		}
	}

	/** Búnker de 9x5x9 con puertas en los cuatro lados y sea lanterns en las esquinas. */
	private static void buildBunker(ServerWorld world, BlockPos origin) {
		BlockState wall = Blocks.DEEPSLATE_BRICKS.getDefaultState();
		BlockState lamp = Blocks.SEA_LANTERN.getDefaultState();
		BlockState air = Blocks.AIR.getDefaultState();
		for (int x = 0; x <= 8; x++) {
			for (int y = 0; y <= 4; y++) {
				for (int z = 0; z <= 8; z++) {
					boolean shell = x == 0 || x == 8 || z == 0 || z == 8 || y == 0 || y == 4;
					boolean door = y >= 1 && y <= 2 && ((x == 4 && (z == 0 || z == 8)) || (z == 4 && (x == 0 || x == 8)));
					boolean light = y == 3 && (x == 1 || x == 7) && (z == 1 || z == 7);
					BlockState state = light ? lamp : (shell && !door ? wall : air);
					world.setBlockState(origin.add(x, y, z), state, Block.NOTIFY_LISTENERS);
				}
			}
		}
	}
}
