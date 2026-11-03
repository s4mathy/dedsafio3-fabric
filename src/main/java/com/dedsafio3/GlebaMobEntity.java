package com.dedsafio3;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.FlyGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Criaturas de Gleba (y el Flashbang de Reviil). Una sola clase; {@link Kind} decide estadísticas y comportamiento:
 * explotar (Creeper de Fluido, Flashbang), escalar, volar (Nautilus Óseo, Parásito Volador) o agarrarse a la cabeza
 * (Cerebro Amarillo).
 */
public class GlebaMobEntity extends HostileEntity {
	public enum Kind {
		CREEPER_FLUIDO("creeper_fluido", 20.0, 0.0, 0.27, false, true, 0.6f, 1.7f, 1.0f),
		NAUTILUS_OSEO("nautilus_oseo", 14.0, 3.0, 0.25, true, false, 0.8f, 0.8f, 0.9f),
		GARRAPATA_CEREBRAL("garrapata_cerebral", 10.0, 4.0, 0.38, false, true, 0.4f, 0.7f, 0.5f),
		GUSANO_CARNE("gusano_carne", 8.0, 3.0, 0.18, false, false, 0.4f, 0.5f, 0.45f),
		PARASITO("parasito", 16.0, 5.0, 0.30, false, false, 0.5f, 1.1f, 0.8f),
		CEREBRO_AMARILLO("cerebro_amarillo", 6.0, 0.0, 0.36, false, false, 0.4f, 0.6f, 0.5f),
		PARASITO_VOLADOR("parasito_volador", 10.0, 4.0, 0.30, true, false, 0.5f, 0.5f, 0.6f),
		FLASHBANG("flashbang", 14.0, 0.0, 0.42, false, false, 0.6f, 1.7f, 1.0f),
		CEREBRO_ROJO("cerebro_rojo", 8.0, 0.0, 0.38, false, false, 0.4f, 0.6f, 0.5f),
		REAPER_PEQUENO("reaper_pequeno", 22.0, 6.0, 0.30, false, false, 0.5f, 1.3f, 0.7f);

		public final String id;
		final double health;
		final double damage;
		final double speed;
		final boolean flying;
		final boolean climbs;
		public final float width;
		public final float height;
		public final float scale;

		Kind(String id, double health, double damage, double speed, boolean flying, boolean climbs, float width, float height, float scale) {
			this.id = id;
			this.health = health;
			this.damage = damage;
			this.speed = speed;
			this.flying = flying;
			this.climbs = climbs;
			this.width = width;
			this.height = height;
			this.scale = scale;
		}

		boolean explodes() {
			return this == CREEPER_FLUIDO || this == FLASHBANG;
		}
	}

	private static final int FUSE_TICKS = 25;

	private final Kind kind;
	private int fuse = -1;
	/** Cerebro Amarillo: jugador al que está agarrado. */
	private LivingEntity host;
	private int drain;

	public GlebaMobEntity(EntityType<? extends HostileEntity> type, World world, Kind kind) {
		super(type, world);
		this.kind = kind;
		if (kind.flying) {
			this.moveControl = new FlightMoveControl(this, 20, true);
			this.navigation = new BirdNavigation(this, world);
			setNoGravity(true);
		}
		if (kind == Kind.REAPER_PEQUENO) {
			equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_HOE));
			setEquipmentDropChance(net.minecraft.entity.EquipmentSlot.MAINHAND, 0f);
		}
		// initGoals() ya corrió dentro del constructor de MobEntity, antes de conocer el tipo: se rehace.
		goalSelector.clear(goal -> true);
		targetSelector.clear(goal -> true);
		initGoals();
	}

	public Kind kind() {
		return kind;
	}

	public static DefaultAttributeContainer.Builder attributes(Kind kind) {
		DefaultAttributeContainer.Builder builder = HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, kind.health)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, kind.damage)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, kind.speed)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
		if (kind.flying) {
			builder.add(EntityAttributes.GENERIC_FLYING_SPEED, kind == Kind.PARASITO_VOLADOR ? 1.0 : 0.6);
		}
		return builder;
	}

	@Override
	protected void initGoals() {
		if (kind == null) {
			return; // llamada del constructor de la superclase
		}
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(2, new MeleeAttackGoal(this, kind == Kind.FLASHBANG ? 1.5 : 1.15, false));
		goalSelector.add(5, kind.flying ? new FlyGoal(this, 1.0) : new WanderAroundFarGoal(this, 0.8));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		goalSelector.add(6, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	/** Los Nautilus no se pueden empujar. */
	@Override
	public boolean isPushable() {
		return kind != Kind.NAUTILUS_OSEO && super.isPushable();
	}

	@Override
	public void takeKnockback(double strength, double x, double z) {
		if (kind != Kind.NAUTILUS_OSEO) {
			super.takeKnockback(strength, x, z);
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		// Quien lleva un Cerebro Amarillo en la cabeza no puede quitárselo: necesita a otra persona.
		if (host != null && source.getAttacker() == host) {
			return false;
		}
		return super.damage(source, amount);
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit && kind == Kind.NAUTILUS_OSEO && target instanceof LivingEntity living) {
			living.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 80, 1), this);
		}
		// Con la hoz, el Reaper Pequeño destierra a su víctima al laberinto del Limbo.
		if (hit && kind == Kind.REAPER_PEQUENO && target instanceof ServerPlayerEntity player) {
			Limbo.banish(player);
		}
		return hit;
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return !kind.flying && super.handleFallDamage(fallDistance, damageMultiplier, damageSource);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient || !(getWorld() instanceof ServerWorld world)) {
			return;
		}
		LivingEntity target = getTarget();
		if (kind.climbs && target != null && horizontalCollision) {
			Vec3d v = getVelocity();
			setVelocity(v.x, 0.24, v.z);
			velocityModified = true;
			fallDistance = 0f;
		}
		if (kind.explodes()) {
			tickFuse(world, target);
		} else if (kind == Kind.CEREBRO_AMARILLO || kind == Kind.CEREBRO_ROJO) {
			tickLatch(target);
		}
	}

	private void tickFuse(ServerWorld world, LivingEntity target) {
		if (fuse >= 0) {
			getNavigation().stop();
			fuse++;
			if (target == null || squaredDistanceTo(target) > 49.0) {
				fuse = -1;
			} else if (fuse >= FUSE_TICKS) {
				explode(world);
			}
		} else if (target != null && squaredDistanceTo(target) < (kind == Kind.FLASHBANG ? 6.0 : 9.0)) {
			fuse = 0;
			playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 1.0f, kind == Kind.FLASHBANG ? 1.8f : 0.6f);
		}
	}

	private void explode(ServerWorld world) {
		if (kind == Kind.FLASHBANG) {
			world.playSound(null, getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, net.minecraft.sound.SoundCategory.HOSTILE, 3.0f, 1.6f);
			// Con el cambio del Limbo, el rango de explosión del Flashbang se duplica.
			double range = DifficultyChange.FLASHBANG_LIMBO.isActive() ? 28.0 : 14.0;
			for (ServerPlayerEntity player : world.getEntitiesByClass(ServerPlayerEntity.class, getBoundingBox().expand(range), p -> true)) {
				net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new FlashPayload(120));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
			}
		} else {
			world.createExplosion(this, getX(), getY(), getZ(), 1.5f, World.ExplosionSourceType.MOB);
			// "Introduce fluidos en tu computadora": te desconecta de la realidad (visión y movimiento).
			for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(6.0), e -> e != this)) {
				living.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 160, 0), this);
				living.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 240, 0), this);
				living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 160, 1), this);
			}
		}
		discard();
	}

	private void tickLatch(LivingEntity target) {
		if (host != null) {
			if (!host.isAlive() || (host instanceof PlayerEntity p && p.isSpectator())) {
				host = null;
				return;
			}
			getNavigation().stop();
			setVelocity(Vec3d.ZERO);
			refreshPositionAndAngles(host.getX(), host.getEyeY() - 0.15, host.getZ(), host.getYaw(), 0f);
			if (++drain % 20 == 0) {
				host.damage(getDamageSources().magic(), 1.0f);
			}
			host.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0, false, false));
			// Cerebro Rojo: control mental, el portador ataca a sus compañeros cercanos (incluso hasta matarlos).
			if (kind == Kind.CEREBRO_ROJO && drain % 15 == 0 && host instanceof PlayerEntity controlled) {
				PlayerEntity victim = null;
				double best = 16.0;
				for (PlayerEntity other : getWorld().getEntitiesByClass(PlayerEntity.class, host.getBoundingBox().expand(4.0), p -> p != host)) {
					double d = host.squaredDistanceTo(other);
					if (!other.isCreative() && !other.isSpectator() && d < best) {
						best = d;
						victim = other;
					}
				}
				if (victim != null) {
					controlled.swingHand(net.minecraft.util.Hand.MAIN_HAND, true);
					victim.damage(getDamageSources().playerAttack(controlled), 5.0f);
					victim.takeKnockback(0.5, host.getX() - victim.getX(), host.getZ() - victim.getZ());
				}
			}
		} else if (target instanceof PlayerEntity player && !player.isCreative() && !player.isSpectator()
				&& squaredDistanceTo(player) < 2.25) {
			host = player;
			drain = 0;
			playSound(SoundEvents.ENTITY_SLIME_SQUISH, 1.0f, 0.6f);
		}
	}

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		// Cambio "Parásitos Voladores explotan al morir".
		if (kind == Kind.PARASITO_VOLADOR && !getWorld().isClient && DifficultyChange.PARASITO_VOLADOR_EXPLOTA.isActive()) {
			getWorld().createExplosion(this, getX(), getY(), getZ(), 2.0f, World.ExplosionSourceType.MOB);
		}
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_SILVERFISH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_SILVERFISH_DEATH;
	}
}
