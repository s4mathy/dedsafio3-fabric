package com.dedsafio3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

/** Efectos de los cambios de dificultad: magma letal, sin miel / panales explosivos, zombies escaladores, veneno primitivo. */
public final class ChangeEffects {
	/** Daño del magma: el número de la serie no cabe en un float, esto ya mata a cualquiera. */
	private static final float MAGMA_DAMAGE = 3.0E9f;
	private static final String TAG_DECIDED = "d3_decided";
	private static final String TAG_CLIMBER = "d3_climber";

	private static final Set<ZombieEntity> CLIMBERS = Collections.newSetFromMap(new WeakHashMap<>());

	private ChangeEffects() {
	}

	public static void reset() {
		CLIMBERS.clear();
	}

	public static void register() {
		// Sin miel: no se puede embotellar ni esquilar un panal.
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!ChangeType.NO_HONEY.isActive()) {
				return ActionResult.PASS;
			}
			BlockState state = world.getBlockState(hit.getBlockPos());
			if (state.isOf(Blocks.BEEHIVE) || state.isOf(Blocks.BEE_NEST)) {
				ItemStack stack = player.getStackInHand(hand);
				if (stack.isOf(Items.GLASS_BOTTLE) || stack.isOf(Items.SHEARS)) {
					return ActionResult.FAIL;
				}
			}
			return ActionResult.PASS;
		});

		// Los panales explotan al romperse.
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (ChangeType.NO_HONEY.isActive() && world instanceof ServerWorld serverWorld
					&& (state.isOf(Blocks.BEEHIVE) || state.isOf(Blocks.BEE_NEST))) {
				serverWorld.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
						3.0f, World.ExplosionSourceType.BLOCK);
			}
		});

		// Zombies escaladores: parte de los zombies que cargan reciben la etiqueta de "escalador".
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof ZombieEntity zombie && ChangeType.ZOMBIE_CLIMB.isActive()) {
				if (!zombie.getCommandTags().contains(TAG_DECIDED)) {
					zombie.addCommandTag(TAG_DECIDED);
					if (zombie.getRandom().nextFloat() < 0.35f) {
						zombie.addCommandTag(TAG_CLIMBER);
					}
				}
				if (zombie.getCommandTags().contains(TAG_CLIMBER)) {
					CLIMBERS.add(zombie);
				}
			}
		});

		// Veneno Primitivo de los reptisaurios (cuerpo a cuerpo y flechas).
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (source.getAttacker() instanceof PrimitivePoison && entity != source.getAttacker()) {
				entity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 8 * 20, 0), source.getAttacker());
			}
			return true;
		});
	}

	public static void tick(MinecraftServer server) {
		if (ChangeType.MAGMA_DEADLY.isActive() && server.getTicks() % 2 == 0) {
			tickMagma(server);
		}
		if (!CLIMBERS.isEmpty() && ChangeType.ZOMBIE_CLIMB.isActive()) {
			Iterator<ZombieEntity> it = CLIMBERS.iterator();
			while (it.hasNext()) {
				ZombieEntity zombie = it.next();
				if (zombie.isRemoved() || !zombie.isAlive()) {
					it.remove();
					continue;
				}
				if (zombie.horizontalCollision && zombie.getTarget() != null) {
					Vec3d velocity = zombie.getVelocity();
					zombie.setVelocity(velocity.x, 0.2, velocity.z);
					zombie.fallDistance = 0f;
				}
			}
		}
	}

	/** Tocar un bloque de magma por cualquier cara mata (el totem aún puede salvar, como en la serie). */
	private static void tickMagma(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
				continue;
			}
			ServerWorld world = player.getServerWorld();
			Box box = player.getBoundingBox().expand(0.05);
			boolean touching = false;
			for (BlockPos pos : BlockPos.iterate(MathHelper.floor(box.minX), MathHelper.floor(box.minY), MathHelper.floor(box.minZ),
					MathHelper.floor(box.maxX), MathHelper.floor(box.maxY), MathHelper.floor(box.maxZ))) {
				if (world.getBlockState(pos).isOf(Blocks.MAGMA_BLOCK)) {
					touching = true;
					break;
				}
			}
			if (touching) {
				player.damage(world.getDamageSources().hotFloor(), MAGMA_DAMAGE);
			}
		}
	}
}
