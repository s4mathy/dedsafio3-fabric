package com.dedsafio3;

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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** Bomba Warden: criatura voladora de la Deep Dark que explota al llegar a su objetivo y deja a los cercanos a oscuras. */
public class WardenBombEntity extends HostileEntity {
	private static final int FUSE_TICKS = 25;
	private int fuse = -1;

	public WardenBombEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
		this.moveControl = new FlightMoveControl(this, 20, true);
		setNoGravity(true);
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 12.0)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.5)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		return new BirdNavigation(this, world);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(2, new MeleeAttackGoal(this, 1.2, false));
		goalSelector.add(5, new FlyGoal(this, 1.0));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		goalSelector.add(6, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			return;
		}
		LivingEntity target = getTarget();
		if (fuse >= 0) {
			getNavigation().stop();
			fuse++;
			if (target == null || squaredDistanceTo(target) > 49.0) {
				fuse = -1;
			} else if (fuse >= FUSE_TICKS) {
				explode();
			}
		} else if (target != null && squaredDistanceTo(target) < 6.25) {
			fuse = 0;
			playSound(SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 1.2f);
		}
	}

	private void explode() {
		World world = getWorld();
		world.createExplosion(this, getX(), getY(), getZ(), 3.5f, World.ExplosionSourceType.MOB);
		for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(8.0), p -> true)) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 200, 0), this);
		}
		discard();
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return false;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_WARDEN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_WARDEN_DEATH;
	}
}
