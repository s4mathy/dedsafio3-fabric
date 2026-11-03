package com.dedsafio3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.registry.RegistryKeys;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.SculkShriekerBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
	/** Último lanzamiento de Ender Pearl de cada jugador (tick del mundo). */
	private static final Map<UUID, Long> PEARLS = new HashMap<>();
	/** Evita duplicar dos veces el daño de caída (al re-aplicarlo o con las perlas). */
	private static boolean fallGuard;

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
			// Al romper hierbas puede salir un Creeper de Pasto.
			if (DifficultyChange.CREEPER_PASTO.isActive() && world instanceof ServerWorld serverWorld
					&& (state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.SHORT_GRASS) || state.isOf(Blocks.TALL_GRASS)
					|| state.isOf(Blocks.FERN) || state.isOf(Blocks.LARGE_FERN) || state.isOf(Blocks.DEAD_BUSH))
					&& world.getRandom().nextFloat() < 0.12f) {
				ModEntities.GRASS_CREEPER.spawn(serverWorld, pos, SpawnReason.TRIGGERED);
			}
			// Gusano de Carne: puede salir al romper bloques de "grasa" (aquí: barro). Parásito Volador: sale de las verrugas.
			if (DifficultyChange.GUSANO_CARNE.isActive() && world instanceof ServerWorld serverWorld
					&& (state.isOf(Blocks.MUD) || state.isOf(Blocks.PACKED_MUD)) && world.getRandom().nextFloat() < 0.3f) {
				ModEntities.GLEBA_MOBS.get(GlebaMobEntity.Kind.GUSANO_CARNE).spawn(serverWorld, pos, SpawnReason.TRIGGERED);
			}
			if (DifficultyChange.PARASITO_VOLADOR.isActive() && world instanceof ServerWorld serverWorld
					&& (state.isOf(Blocks.NETHER_WART_BLOCK) || state.isOf(Blocks.WARPED_WART_BLOCK))
					&& world.getRandom().nextFloat() < 0.4f) {
				ModEntities.GLEBA_MOBS.get(GlebaMobEntity.Kind.PARASITO_VOLADOR).spawn(serverWorld, pos, SpawnReason.TRIGGERED);
			}
			// Al romper una telaraña puede salir una Araña de Cueva.
			if (DifficultyChange.COBWEB_SPIDER.isActive() && state.isOf(Blocks.COBWEB) && world instanceof ServerWorld serverWorld
					&& world.getRandom().nextFloat() < 0.3f) {
				EntityType.CAVE_SPIDER.spawn(serverWorld, pos, SpawnReason.TRIGGERED);
			}
		});

		// No caen brotes de árboles (los que suelta el follaje; los que tira un jugador no cuentan).
		// Creepers eléctricos en lugar de los comunes. Se anota quién lanzó una Ender Pearl.
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof ItemEntity item && DifficultyChange.NO_SAPLINGS.isActive() && item.age == 0
					&& item.getOwner() == null && item.getStack().isIn(ItemTags.SAPLINGS)) {
				item.discard();
			} else if (entity instanceof CreeperEntity creeper && DifficultyChange.CREEPER_ELECTRIC.isActive()
					&& creeper.age == 0) {
				NbtCompound nbt = new NbtCompound();
				creeper.writeNbt(nbt);
				nbt.putBoolean("powered", true);
				creeper.readNbt(nbt);
			} else if (entity instanceof EnderPearlEntity pearl && pearl.getOwner() instanceof ServerPlayerEntity owner) {
				PEARLS.put(owner.getUuid(), world.getTime());
			}
		});

		// No se puede ordeñar a las vacas con el cubo.
		UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (DifficultyChange.NO_MILK.isActive() && entity instanceof CowEntity cow && !cow.isBaby()
					&& player.getStackInHand(hand).isOf(Items.BUCKET)) {
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});

		// Al morir, el Warden deja un Sculk Shrieker.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (DifficultyChange.WARDEN_SHRIEKER.isActive() && entity instanceof WardenEntity && entity.getWorld() instanceof ServerWorld w) {
				BlockPos pos = entity.getBlockPos();
				for (int i = 0; i < 4; i++) {
					BlockPos at = pos.up(i);
					if (w.getBlockState(at).isAir() || w.getBlockState(at).isReplaceable()) {
						w.setBlockState(at, Blocks.SCULK_SHRIEKER.getDefaultState().with(SculkShriekerBlock.CAN_SUMMON, true));
						break;
					}
				}
			}
		});

		// Ender Pearl: con "perforantes" el daño (5) pasa a ser mágico, que ignora la armadura;
		// con "10 de daño" la perla hace 10 (5 corazones).
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			boolean pierce = DifficultyChange.PEARL_PIERCE.isActive();
			boolean ten = DifficultyChange.PEARL_10.isActive();
			if (entity instanceof ServerPlayerEntity p && (pierce || ten)
					&& source.isOf(DamageTypes.FALL) && amount > 4.9f && amount < 5.1f) {
				Long thrown = PEARLS.get(p.getUuid());
				if (thrown != null && p.getServerWorld().getTime() - thrown < 400) {
					PEARLS.remove(p.getUuid());
					fallGuard = true;
					try {
						p.damage(pierce ? p.getServerWorld().getDamageSources().magic() : p.getServerWorld().getDamageSources().fall(),
								ten ? 10.0f : 5.0f);
					} finally {
						fallGuard = false;
					}
					return false;
				}
			}
			return true;
		});

		// Daño de caída x2.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!fallGuard && DifficultyChange.FALL_DAMAGE_X2.isActive() && source.isOf(DamageTypes.FALL)) {
				fallGuard = true;
				try {
					entity.damage(source, amount * 2.0f);
				} finally {
					fallGuard = false;
				}
				return false;
			}
			return true;
		});

		// Arañas: Fuerza III y Velocidad I infinita (según los cambios activos al aparecer).
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof net.minecraft.entity.mob.SpiderEntity spider && spider.age == 0) {
				if (DifficultyChange.SPIDER_STRENGTH.isActive()) {
					spider.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
							net.minecraft.entity.effect.StatusEffects.STRENGTH, net.minecraft.entity.effect.StatusEffectInstance.INFINITE, 2, false, false));
				}
				if (DifficultyChange.SPIDER_NIGHT_SPEED.isActive()) {
					spider.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
							net.minecraft.entity.effect.StatusEffects.SPEED, net.minecraft.entity.effect.StatusEffectInstance.INFINITE, 0, false, false));
				}
			}
		});

		// Médusa Ósea: si mata a un jugador se convierte en él y adopta su inventario.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (DifficultyChange.MEDUSA_OSEA.isActive() && entity instanceof ServerPlayerEntity dead
					&& source.getAttacker() instanceof MedusaEntity medusa) {
				java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
				for (ItemEntity drop : dead.getServerWorld().getEntitiesByClass(ItemEntity.class, dead.getBoundingBox().expand(4.0), d -> true)) {
					stacks.add(drop.getStack().copy());
					drop.discard();
				}
				medusa.adopt(dead, stacks);
			}
		});

		// Todos los zombis nacen con Espada de Hierro con Filo V.
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (DifficultyChange.ZOMBIE_ELITE.isActive() && entity instanceof ZombieEntity zombie
					&& zombie.age == 0 && zombie.getMainHandStack().isEmpty()) {
				ItemStack sword = new ItemStack(Items.IRON_SWORD);
				sword.addEnchantment(world.getRegistryManager().getWrapperOrThrow(RegistryKeys.ENCHANTMENT)
						.getOrThrow(Enchantments.SHARPNESS), 5);
				zombie.equipStack(EquipmentSlot.MAINHAND, sword);
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

		// Keep Inventory (Fruta de Quiu): quien muere con el efecto conserva inventario y experiencia.
		net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			if (!alive && oldPlayer.hasStatusEffect(ModEffects.KEEP_INVENTORY)) {
				newPlayer.getInventory().clone(oldPlayer.getInventory());
				newPlayer.experienceLevel = oldPlayer.experienceLevel;
				newPlayer.experienceProgress = oldPlayer.experienceProgress;
				newPlayer.totalExperience = oldPlayer.totalExperience;
			}
		});

		ServerTickEvents.END_WORLD_TICK.register(ChangeHooks::tickWorld);

		// Animales extintos: un gas acaba con los animales cercanos a los jugadores (las mascotas con dueño se salvan).
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world.getTime() % 40 != 0 || !DifficultyChange.ANIMALS_EXTINCT.isActive()) {
				return;
			}
			for (ServerPlayerEntity player : world.getPlayers()) {
				for (net.minecraft.entity.passive.AnimalEntity animal : world.getEntitiesByClass(
						net.minecraft.entity.passive.AnimalEntity.class, player.getBoundingBox().expand(96.0), a -> true)) {
					if (animal instanceof net.minecraft.entity.passive.TameableEntity tame && tame.isTamed()) {
						continue;
					}
					world.spawnParticles(net.minecraft.particle.ParticleTypes.SNEEZE, animal.getX(), animal.getBodyY(0.5),
							animal.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
					animal.damage(world.getDamageSources().magic(), 1.0E6f);
				}
			}
		});
	}

	private static void tickWorld(ServerWorld world) {
		boolean magma = DifficultyChange.MAGMA_DEADLY.isActive();
		boolean elite = DifficultyChange.ZOMBIE_ELITE.isActive();
		boolean climb = DifficultyChange.ZOMBIE_CLIMB.isActive() || elite;
		boolean doors = DifficultyChange.DOORS_NO_AIR.isActive();
		if (!magma && !climb && !doors) {
			return;
		}
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (magma && !player.isCreative() && !player.isSpectator() && touchesMagma(world, player)) {
				player.damage(world.getDamageSources().hotFloor(), 1.0E9f);
			}
			if (doors && !player.isCreative() && !player.isSpectator()) {
				// Las puertas y trampillas ya no dan aire bajo el agua: el aire se gasta como al ahogarse.
				if (inAirPocket(world, player)) {
					int air = player.getAir() - 5; // cancela la recarga de +4 por tick y gasta 1 por tick
					if (air <= -20) {
						player.setAir(0);
						player.damage(world.getDamageSources().drown(), 2.0f);
					} else {
						player.setAir(air);
					}
				}
			}
			if (climb && world.getTime() % 2 == 0) {
				for (ZombieEntity zombie : world.getEntitiesByClass(ZombieEntity.class, player.getBoundingBox().expand(24.0), z -> true)) {
					// Un 40 % de los zombis (fijo por zombi) sabe escalar; con los zombis élite, todos.
					if ((elite || Math.floorMod(zombie.getUuid().hashCode(), 10) < 4)
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

	private static boolean doorLike(BlockState state) {
		return state.isIn(BlockTags.DOORS) || state.isIn(BlockTags.TRAPDOORS);
	}

	private static boolean water(ServerWorld world, BlockPos pos) {
		return world.getFluidState(pos).isIn(FluidTags.WATER);
	}

	/** Cabeza fuera del agua pero bajo ella: bolsa de aire (puerta o trampilla dentro del agua, o techo bajo un lago). */
	private static boolean inAirPocket(ServerWorld world, ServerPlayerEntity player) {
		BlockPos eye = BlockPos.ofFloored(player.getEyePos());
		if (!world.getFluidState(eye).isEmpty()) {
			return false; // con los ojos en el agua ya se ahoga normalmente
		}
		if (water(world, eye.up())) {
			return true; // agua justo encima de la cabeza
		}
		boolean door = doorLike(world.getBlockState(eye)) || doorLike(world.getBlockState(eye.up()))
				|| doorLike(world.getBlockState(eye.down()));
		if (!door) {
			return false;
		}
		for (net.minecraft.util.math.Direction d : net.minecraft.util.math.Direction.values()) {
			if (water(world, eye.offset(d))) {
				return true;
			}
		}
		return water(world, eye.up(2));
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
