package com.dedsafio3;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.world.World;

/** Narval: cetáceo de los océanos con un largo colmillo. Se comporta como un delfín, pero más grande y fuerte. */
public class NarvalEntity extends DolphinEntity {
	public NarvalEntity(EntityType<? extends DolphinEntity> type, World world) {
		super(type, world);
	}

	/** Con el cambio "Narval" de los acuíferos activo, embiste a los intrusos a gran velocidad. */
	@Override
	protected void initGoals() {
		super.initGoals();
		goalSelector.add(1, new MeleeAttackGoal(this, 2.4, true) {
			@Override
			public boolean canStart() {
				return DifficultyChange.NARVAL_ACUIFERO.isActive() && super.canStart();
			}
		});
		targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false,
				e -> DifficultyChange.NARVAL_ACUIFERO.isActive()));
	}

	/** A veces empuja al jugador al golpearlo, como si jugara con él. */
	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit && target instanceof LivingEntity living && getRandom().nextFloat() < 0.5f) {
			Vec3d push = target.getPos().subtract(getPos()).normalize();
			living.addVelocity(push.x * 1.2, 0.5, push.z * 1.2);
			living.velocityModified = true;
		}
		return hit;
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return DolphinEntity.createDolphinAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0);
	}
}
