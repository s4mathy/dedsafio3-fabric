package com.dedsafio3;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Creeper de Magma: al explotar deja Lava en su posición. Es inmune al fuego. */
public class MagmaCreeperEntity extends HostileEntity {
	private static final int FUSE_TICKS = 30;
	private static final float EXPLOSION_POWER = 3.0f;

	private int fuse = -1;

	public MagmaCreeperEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(4, new MeleeAttackGoal(this, 1.0, false));
		goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		goalSelector.add(6, new LookAroundGoal(this));
		targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		targetSelector.add(2, new RevengeGoal(this));
	}

	@Override
	public boolean isFireImmune() {
		return true;
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
		} else if (target != null && squaredDistanceTo(target) < 9.0) {
			fuse = 0;
			playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 1.0f, 0.8f);
		}
	}

	private void explode() {
		World world = getWorld();
		BlockPos here = getBlockPos();
		world.createExplosion(this, getX(), getY(), getZ(), EXPLOSION_POWER, World.ExplosionSourceType.MOB);
		// Lava en la posición donde explotó (y en el hueco que dejó, para que no se la lleve el aire).
		BlockState lava = Blocks.LAVA.getDefaultState();
		for (BlockPos pos : new BlockPos[]{here, here.down()}) {
			if (world.getBlockState(pos).isAir() || world.getBlockState(pos).isReplaceable()) {
				world.setBlockState(pos, lava);
			}
		}
		discard();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_CREEPER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_CREEPER_DEATH;
	}
}
