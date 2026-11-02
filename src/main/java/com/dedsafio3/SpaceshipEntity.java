package com.dedsafio3;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/**
 * Nave espacial: se coloca sobre una Plataforma de Despegue. Al subirte (clic derecho) hay una cuenta atrás,
 * despega y te lleva de la Tierra (Overworld) a Gleba, o de Gleba de vuelta. Al aterrizar levanta su propia plataforma.
 */
public class SpaceshipEntity extends MobEntity {
	public static final RegistryKey<World> GLEBA = RegistryKey.of(RegistryKeys.WORLD, Dedsafio3.id("gleba"));
	private static final int COUNTDOWN = 60;
	private static final int FLIGHT_END = 110;

	private int launch;

	public SpaceshipEntity(EntityType<? extends MobEntity> type, World world) {
		super(type, world);
		setNoGravity(true);
		setPersistent();
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0);
	}

	/** Mundo al que viaja esta nave desde el que está, o null si aquí no puede despegar. */
	static RegistryKey<World> destinationFrom(RegistryKey<World> here) {
		if (here == World.OVERWORLD) {
			return GLEBA;
		}
		return here == GLEBA ? World.OVERWORLD : null;
	}

	@Override
	public boolean isAiDisabled() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		// Solo un jugador en creativo (o el vacío) puede destruirla.
		if (source.isOf(DamageTypes.OUT_OF_WORLD) || source.isOf(DamageTypes.GENERIC_KILL)
				|| (source.getAttacker() instanceof PlayerEntity player && player.isCreative())) {
			return super.damage(source, amount);
		}
		return false;
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return false;
	}

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (getWorld().isClient) {
			return ActionResult.SUCCESS;
		}
		if (hasPassengers()) {
			return ActionResult.PASS;
		}
		ServerWorld world = (ServerWorld) getWorld();
		RegistryKey<World> destKey = destinationFrom(world.getRegistryKey());
		if (destKey == null || world.getServer().getWorld(destKey) == null) {
			player.sendMessage(Text.translatable("message.dedsafio3.ship_unavailable"), true);
			return ActionResult.FAIL;
		}
		player.startRiding(this);
		launch = 1;
		return ActionResult.SUCCESS;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			return;
		}
		if (!(getFirstPassenger() instanceof ServerPlayerEntity player)) {
			launch = 0;
			return;
		}
		ServerWorld world = (ServerWorld) getWorld();
		launch++;
		if (launch < COUNTDOWN && launch % 20 == 1) {
			player.sendMessage(Text.translatable("message.dedsafio3.ship_countdown", 3 - launch / 20), true);
			world.playSound(null, getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.NEUTRAL, 1.0f, 1.2f);
		}
		world.spawnParticles(ParticleTypes.FLAME, getX(), getY() + 0.2, getZ(), 6, 0.25, 0.05, 0.25, 0.03);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.1, getZ(), 3, 0.35, 0.05, 0.35, 0.02);
		if (launch == COUNTDOWN) {
			world.playSound(null, getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.NEUTRAL, 3.0f, 0.6f);
		}
		if (launch > COUNTDOWN) {
			setPosition(getX(), getY() + 0.6, getZ());
		}
		if (launch >= FLIGHT_END) {
			travel(player, world);
		}
	}

	private void travel(ServerPlayerEntity player, ServerWorld from) {
		MinecraftServer server = from.getServer();
		RegistryKey<World> destKey = destinationFrom(from.getRegistryKey());
		ServerWorld dest = destKey == null ? null : server.getWorld(destKey);
		if (dest == null) {
			player.stopRiding();
			launch = 0;
			return;
		}
		int x = MathHelper.floor(getX());
		int z = MathHelper.floor(getZ());
		dest.getChunk(x >> 4, z >> 4); // fuerza la carga/generación del terreno de destino
		BlockPos pad = new BlockPos(x, dest.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z), z);
		buildPad(dest, pad);

		SpaceshipEntity ship = ModEntities.SPACESHIP.create(dest);
		if (ship != null) {
			ship.refreshPositionAndAngles(x + 0.5, pad.getY() + 1, z + 0.5, getYaw(), 0f);
			dest.spawnEntity(ship);
		}
		player.stopRiding();
		player.teleport(dest, x + 0.5, pad.getY() + 1, z + 2.5, player.getYaw(), player.getPitch());
		player.fallDistance = 0f;
		player.sendMessage(Text.translatable(destKey == GLEBA ? "message.dedsafio3.ship_arrived_gleba"
				: "message.dedsafio3.ship_arrived_home"), true);
		discard();
	}

	/** Plataforma de aterrizaje: base de ladrillos, espacio libre encima y el bloque Plataforma de Despegue. */
	private static void buildPad(ServerWorld world, BlockPos pad) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos below = pad.add(dx, -1, dz);
				if (!world.getBlockState(below).isSolidBlock(world, below)) {
					world.setBlockState(below, Blocks.STONE_BRICKS.getDefaultState());
				}
				for (int dy = 0; dy <= 3; dy++) {
					BlockPos at = pad.add(dx, dy, dz);
					if (!world.getBlockState(at).isAir()) {
						world.breakBlock(at, false);
					}
				}
			}
		}
		world.setBlockState(pad, ModBlocks.PLATFORM.getDefaultState());
	}

	public static void register() {
		// Reservado: la nave no necesita eventos propios.
	}

	/** Útil para que otras clases comprueben si hay una nave en una caja. */
	static boolean isShip(Entity entity) {
		return entity instanceof SpaceshipEntity;
	}
}
