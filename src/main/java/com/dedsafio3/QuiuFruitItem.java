package com.dedsafio3;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Fruta de Quiu: al comerla das Keep Inventory (efecto infinito que se pierde al morir). */
public class QuiuFruitItem extends TooltipItem {
	public QuiuFruitItem(Settings settings) {
		super(settings);
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (!world.isClient) {
			user.addStatusEffect(new StatusEffectInstance(ModEffects.KEEP_INVENTORY, StatusEffectInstance.INFINITE, 0, false, true, true));
		}
		return super.finishUsing(stack, world, user);
	}
}
