package com.dedsafio3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Cambios de dificultad que no son mobs: panales explosivos sin miel, magma letal y zombis escaladores. */
public final class ChangeHooks {
	private ChangeHooks() {
	}

	private static boolean isHive(BlockState state) {
		return state.isOf(Blocks.BEEHIVE) || state.isOf(Blocks.BEE_NEST);
	}

	public static void register() {
		// Los panales explotan al romperse.
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (DifficultyChange.NO_HONEY.isActive() && isHive(state) && world instanceof ServerWorld serverWorld) {
				serverWorld.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.5f,
						World.ExplosionSourceType.TNT);
			}
		});

		// Ya no se puede conseguir miel ni panales de miel de las colmenas.
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (DifficultyChange.NO_HONEY.isActive() && isHive(world.getBlockState(hit.getBlockPos()))) {
				ItemStack held = player.getStackInHand(hand);
				if (held.isOf(Items.GLASS_BOTTLE) || held.isOf(Items.SHEARS)) {
					return ActionResult.FAIL;
				}
			}
			return ActionResult.PASS;
		});

		ServerTickEvents.END_WORLD_TICK.register(ChangeHooks::tickWorld);
	}

	private static void tickWorld(ServerWorld world) {
		boolean magma = DifficultyChange.MAGMA_DEADLY.isActive();
		boolean climb = DifficultyChange.ZOMBIE_CLIMB.isActive();
		if (!magma && !climb) {
			return;
		}
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (magma && !player.isCreative() && !player.isSpectator() && touchesMagma(world, player)) {
				player.damage(world.getDamageSources().hotFloor(), 1.0E9f);
			}
			if (climb && world.getTime() % 2 == 0) {
				for (ZombieEntity zombie : world.getEntitiesByClass(ZombieEntity.class, player.getBoundingBox().expand(24.0), z -> true)) {
					// Un 40 % de los zombis (fijo por zombi) sabe escalar.
					if (Math.floorMod(zombie.getUuid().hashCode(), 10) < 4
							&& zombie.horizontalCollision && zombie.getTarget() != null) {
						Vec3d v = zombie.getVelocity();
						zombie.setVelocity(v.x, 0.24, v.z);
						zombie.velocityModified = true;
						zombie.fallDistance = 0f;
					}
				}
			}
		}
	}

	/** Toca un bloque de magma por cualquiera de sus caras (no solo por encima). */
	private static boolean touchesMagma(ServerWorld world, ServerPlayerEntity player) {
		Box box = player.getBoundingBox().expand(0.06);
		BlockPos min = BlockPos.ofFloored(box.minX, box.minY, box.minZ);
		BlockPos max = BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ);
		for (BlockPos pos : BlockPos.iterate(min, max)) {
			if (world.getBlockState(pos).isOf(Blocks.MAGMA_BLOCK)) {
				return true;
			}
		}
		return false;
	}
}
