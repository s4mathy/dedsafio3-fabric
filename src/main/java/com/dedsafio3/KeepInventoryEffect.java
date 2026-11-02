package com.dedsafio3;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/** Keep Inventory (Fruta de Quiu): al morir conservas el inventario. El efecto se pierde al morir/revivir. */
public class KeepInventoryEffect extends StatusEffect {
	public KeepInventoryEffect() {
		super(StatusEffectCategory.BENEFICIAL, 0xE8B830);
	}
}
