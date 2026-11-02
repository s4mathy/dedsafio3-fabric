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
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Dáctylo Adulto: amo de los cielos. Atrapa a su presa, la eleva por los aires y la deja caer pasados unos segundos.
 * Forma parte de las pesadillas: aparece cuando un jugador tiene Insomnia de nivel alto.
 */
public class DactiloAdultEntity extends HostileEntity {
	private static final int CARRY_TICKS = 100;
	private static final double MAX_RISE = 28.0;

	private LivingEntity grabbed;
	private int carried;
	private int cooldown;
	private double startY;

	public DactiloAdultEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		this.moveControl = new FlightMoveControl(this, 20, true);
		setNoGravity(true);
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.8)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		return new BirdNavigation(this, world);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(2, new MeleeAttackGoal(this, 1.3, true));
		goalSelector.add(5, new FlyGoal(this, 1.0));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
		goalSelector.add(6, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit && grabbed == null && cooldown <= 0 && target instanceof LivingEntity living
				&& !(living instanceof PlayerEntity player && (player.isCreative() || player.isSpectator()))) {
			grabbed = living;
			carried = 0;
			startY = getY();
			playSound(SoundEvents.ENTITY_PHANTOM_SWOOP, 1.5f, 0.6f);
		}
		return hit;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			return;
		}
		if (cooldown > 0) {
			cooldown--;
		}
		if (grabbed == null) {
			return;
		}
		if (!grabbed.isAlive() || carried++ >= CARRY_TICKS) {
			grabbed = null; // lo deja caer
			cooldown = 200;
			return;
		}
		// Lo lleva colgado debajo y sube; durante el transporte no acumula caída.
		grabbed.requestTeleport(getX(), getY() - grabbed.getHeight() + 0.3, getZ());
		grabbed.setVelocity(Vec3d.ZERO);
		grabbed.velocityModified = true;
		grabbed.fallDistance = 0f;
		Vec3d v = getVelocity();
		setVelocity(v.x * 0.5, getY() - startY < MAX_RISE ? 0.22 : 0.0, v.z * 0.5);
		getNavigation().stop();
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_PHANTOM_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_PHANTOM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_PHANTOM_DEATH;
	}
}
