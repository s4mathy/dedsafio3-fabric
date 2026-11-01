package com.dedsafio3;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.FlyGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/**
 * Naves Phora, que vuelan: el Pod Phora dispara proyectiles láser desde lejos y el Skiff Phora ataca cuerpo a cuerpo.
 */
public class PhoraEntity extends HostileEntity implements RangedAttackMob {
	private final boolean pod;

	public PhoraEntity(EntityType<? extends HostileEntity> type, World world, boolean pod) {
		super(type, world);
		this.pod = pod;
		this.moveControl = new FlightMoveControl(this, 20, true);
		setNoGravity(true);
	}

	public static DefaultAttributeContainer.Builder attributes(boolean pod) {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, pod ? 20.0 : 30.0)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, pod ? 2.0 : 7.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, pod ? 0.5 : 0.7)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0);
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		return new BirdNavigation(this, world);
	}

	@Override
	protected void initGoals() {
		if (pod) {
			goalSelector.add(2, new ProjectileAttackGoal(this, 1.0, 30, 22.0f));
		} else {
			goalSelector.add(2, new MeleeAttackGoal(this, 1.2, true));
		}
		goalSelector.add(5, new FlyGoal(this, 1.0));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		goalSelector.add(6, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	/** Proyectil láser: un disparo rápido y recto, sin gravedad. */
	@Override
	public void shootAt(LivingEntity target, float pullProgress) {
		PersistentProjectileEntity laser = ProjectileUtil.createArrowProjectile(this, new ItemStack(Items.ARROW), 1.0f, null);
		double dx = target.getX() - getX();
		double dy = target.getBodyY(0.5) - laser.getY();
		double dz = target.getZ() - getZ();
		laser.setNoGravity(true);
		laser.setDamage(2.5);
		laser.setVelocity(dx, dy, dz, 2.6f, 1.0f);
		playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 1.0f, 1.6f);
		getWorld().spawnEntity(laser);
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return false;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_IRON_GOLEM_DEATH;
	}
}
