package com.dedsafio3;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.BowAttackGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/**
 * Los reptisaurios y el Dromoraptor Rojo comparten clase; {@link Kind} decide estadísticas, arma y veneno.
 * El Salvaje y el Dromoraptor atacan con garras; el Guerrero y el de Lanza cuerpo a cuerpo con veneno;
 * el Arquero dispara flechas envenenadas ("Veneno Primitivo").
 */
public class ReptilianEntity extends HostileEntity implements RangedAttackMob {
	public enum Kind {
		SALVAJE("reptisaurio_salvaje", 30.0, 7.0, 0.30, false, null),
		GUERRERO("reptisaurio_guerrero", 40.0, 8.0, 0.27, true, Items.STONE_AXE),
		ARQUERO("reptisaurio_arquero", 24.0, 3.0, 0.25, true, Items.BOW),
		LANZA("reptisaurio_lanza", 36.0, 9.0, 0.27, true, Items.TRIDENT),
		DROMORAPTOR("dromoraptor_rojo", 34.0, 10.0, 0.38, false, null);

		public final String id;
		final double health;
		final double damage;
		final double speed;
		final boolean poison;
		final Item weapon;

		Kind(String id, double health, double damage, double speed, boolean poison, Item weapon) {
			this.id = id;
			this.health = health;
			this.damage = damage;
			this.speed = speed;
			this.poison = poison;
			this.weapon = weapon;
		}
	}

	private final Kind kind;

	public ReptilianEntity(EntityType<? extends HostileEntity> type, World world, Kind kind) {
		super(type, world);
		this.kind = kind;
		if (kind.weapon != null) {
			equipStack(EquipmentSlot.MAINHAND, new ItemStack(kind.weapon));
		}
	}

	public Kind kind() {
		return kind;
	}

	public static DefaultAttributeContainer.Builder attributes(Kind kind) {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, kind.health)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, kind.damage)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, kind.speed)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		if (kind == Kind.ARQUERO) {
			goalSelector.add(2, new BowAttackGoal<>(this, 1.0, 20, 15.0f));
		} else {
			goalSelector.add(2, new MeleeAttackGoal(this, 1.15, false));
		}
		goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		goalSelector.add(6, new LookAroundGoal(this));
		// Se desplazan en manada: si atacan a uno, avisan a los demás.
		targetSelector.add(1, new RevengeGoal(this, ReptilianEntity.class).setGroupRevenge());
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit && kind.poison && target instanceof LivingEntity living) {
			living.addStatusEffect(new StatusEffectInstance(ModEffects.PRIMITIVE_POISON, 200, 0), this);
		}
		return hit;
	}

	@Override
	public void shootAt(LivingEntity target, float pullProgress) {
		PersistentProjectileEntity arrow = ProjectileUtil.createArrowProjectile(this, new ItemStack(Items.ARROW), pullProgress, null);
		if (arrow instanceof ArrowEntity tipped) {
			tipped.addEffect(new StatusEffectInstance(ModEffects.PRIMITIVE_POISON, 200, 0));
		}
		double dx = target.getX() - getX();
		double dy = target.getBodyY(1.0 / 3.0) - arrow.getY();
		double dz = target.getZ() - getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		arrow.setVelocity(dx, dy + horizontal * 0.2, dz, 1.6f, 14 - getWorld().getDifficulty().getId() * 4);
		playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0f, 1.0f / (getRandom().nextFloat() * 0.4f + 0.8f));
		getWorld().spawnEntity(arrow);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_HUSK_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_HUSK_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_HUSK_DEATH;
	}
}
