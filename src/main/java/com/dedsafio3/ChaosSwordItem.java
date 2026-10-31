package com.dedsafio3;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Espada Caótica: cada golpe aplica un efecto aleatorio al objetivo. */
public class ChaosSwordItem extends SwordItem {
	private static final List<RegistryEntry<StatusEffect>> EFFECTS = List.of(
			StatusEffects.SLOWNESS,
			StatusEffects.WEAKNESS,
			StatusEffects.POISON,
			StatusEffects.BLINDNESS,
			StatusEffects.NAUSEA,
			StatusEffects.GLOWING,
			StatusEffects.LEVITATION,
			StatusEffects.WITHER,
			StatusEffects.HUNGER
	);

	public ChaosSwordItem(ToolMaterial material, Settings settings) {
		super(material, settings);
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		RegistryEntry<StatusEffect> effect = EFFECTS.get(attacker.getRandom().nextInt(EFFECTS.size()));
		int seconds = effect == StatusEffects.LEVITATION ? 2 : 4;
		target.addStatusEffect(new StatusEffectInstance(effect, seconds * 20, 0), attacker);
		return super.postHit(stack, target, attacker);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.GRAY));
	}
}
