package com.dedsafio3;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.world.World;

/**
 * Plumosaurio: coloso pacífico, pero si lo golpeas te hará mucho daño.
 * Reutiliza el comportamiento del golem de hierro (neutral, solo se defiende) en versión gigante.
 */
public class PlumosaurioEntity extends IronGolemEntity {
	public PlumosaurioEntity(EntityType<? extends IronGolemEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return IronGolemEntity.createIronGolemAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 250.0)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 28.0)
				.add(EntityAttributes.GENERIC_SCALE, 1.7);
	}
}
