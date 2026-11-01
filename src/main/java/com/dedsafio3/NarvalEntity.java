package com.dedsafio3;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.world.World;

/** Narval: cetáceo de los océanos con un largo colmillo. Se comporta como un delfín, pero más grande y fuerte. */
public class NarvalEntity extends DolphinEntity {
	public NarvalEntity(EntityType<? extends DolphinEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder attributes() {
		return DolphinEntity.createDolphinAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0);
	}
}
