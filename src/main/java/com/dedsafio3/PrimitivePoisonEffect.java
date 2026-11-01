package com.dedsafio3;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/** Veneno Primitivo: daño mágico continuo (a diferencia del veneno normal, sí puede matar). */
public class PrimitivePoisonEffect extends StatusEffect {
	public PrimitivePoisonEffect() {
		super(StatusEffectCategory.HARMFUL, 0x4E9331);
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		int interval = 20 >> amplifier;
		return interval == 0 || duration % interval == 0;
	}

	@Override
	public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
		entity.damage(entity.getDamageSources().magic(), 1.0f);
		return true;
	}
}
