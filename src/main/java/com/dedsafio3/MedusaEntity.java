package com.dedsafio3;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.List;

/**
 * Médusa Ósea: si mata a un jugador se convierte en él (toma su nombre) y adopta su inventario: se equipa su armadura,
 * su mejor arma y su escudo, y usa su comida para curarse. Al morir suelta todo.
 */
public class MedusaEntity extends HostileEntity {
	private final DefaultedList<ItemStack> loot = DefaultedList.ofSize(54, ItemStack.EMPTY);
	private int healCooldown;

	public MedusaEntity(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 70.0)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.33)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.3);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(2, new MeleeAttackGoal(this, 1.25, false));
		goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
		goalSelector.add(6, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	/** Se convierte en el jugador muerto y se queda con sus cosas. */
	public void adopt(ServerPlayerEntity dead, List<ItemStack> stacks) {
		setCustomName(dead.getName());
		setCustomNameVisible(true);
		setPersistent();
		setHealth(getMaxHealth());
		for (ItemStack stack : stacks) {
			if (stack.isEmpty()) {
				continue;
			}
			ItemStack copy = stack.copy();
			if (copy.getItem() instanceof ArmorItem armor) {
				EquipmentSlot slot = armor.getSlotType();
				if (getEquippedStack(slot).isEmpty()) {
					equipStack(slot, copy);
					continue;
				}
			} else if (copy.getItem() instanceof SwordItem || copy.getItem() instanceof AxeItem) {
				ItemStack current = getEquippedStack(EquipmentSlot.MAINHAND);
				if (current.isEmpty()) {
					equipStack(EquipmentSlot.MAINHAND, copy);
					continue;
				} else if (copy.getMaxDamage() > current.getMaxDamage()) {
					equipStack(EquipmentSlot.MAINHAND, copy);
					copy = current;
				}
			} else if (copy.isOf(Items.SHIELD) && getEquippedStack(EquipmentSlot.OFFHAND).isEmpty()) {
				equipStack(EquipmentSlot.OFFHAND, copy);
				continue;
			}
			addLoot(copy);
		}
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			setEquipmentDropChance(slot, 1.0f);
		}
	}

	private void addLoot(ItemStack stack) {
		for (int i = 0; i < loot.size(); i++) {
			if (loot.get(i).isEmpty()) {
				loot.set(i, stack);
				return;
			}
		}
		dropStack(stack); // sin sitio: cae al suelo
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			return;
		}
		if (healCooldown > 0) {
			healCooldown--;
		} else if (getHealth() < getMaxHealth() * 0.4f) {
			// "Usa todos sus objetos": come lo que haya robado para curarse.
			for (int i = 0; i < loot.size(); i++) {
				ItemStack stack = loot.get(i);
				if (!stack.isEmpty() && stack.get(DataComponentTypes.FOOD) != null) {
					stack.decrement(1);
					heal(8.0f);
					playSound(SoundEvents.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
					healCooldown = 200;
					break;
				}
			}
		}
	}

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (!getWorld().isClient) {
			for (ItemStack stack : loot) {
				if (!stack.isEmpty()) {
					dropStack(stack.copy());
				}
			}
			loot.clear();
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		Inventories.writeNbt(nbt, loot, getRegistryManager());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		Inventories.readNbt(nbt, loot, getRegistryManager());
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_SKELETON_DEATH;
	}
}
