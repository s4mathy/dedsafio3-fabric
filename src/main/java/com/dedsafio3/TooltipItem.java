package com.dedsafio3;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Ítem simple que muestra la línea "<clave del ítem>.tooltip" del archivo de idioma. */
public class TooltipItem extends Item {
	public TooltipItem(Settings settings) {
		super(settings);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.GRAY));
	}
}
