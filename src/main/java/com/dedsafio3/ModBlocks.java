package com.dedsafio3;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
	public static final Map<RouletteColor, RouletteBlock> ROULETTES = new EnumMap<>(RouletteColor.class);

	private ModBlocks() {
	}

	public static void register() {
		for (RouletteColor color : RouletteColor.values()) {
			String name = "roulette_" + color.id();
			RouletteBlock block = Registry.register(
					Registries.BLOCK,
					Dedsafio3.id(name),
					new RouletteBlock(color, AbstractBlock.Settings.create()
							.mapColor(color.mapColor())
							.strength(2.5f, 6.0f)
							.sounds(BlockSoundGroup.METAL)
							.luminance(state -> 8)
							.pistonBehavior(PistonBehavior.NORMAL))
			);
			ROULETTES.put(color, block);
			Registry.register(Registries.ITEM, Dedsafio3.id(name), new BlockItem(block, new Item.Settings()));
		}
	}

	public static Block get(RouletteColor color) {
		return ROULETTES.get(color);
	}
}
