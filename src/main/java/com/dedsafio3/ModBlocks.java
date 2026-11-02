package com.dedsafio3;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
	/** Ruletas de color fijo. */
	public static final Map<RouletteColor, RouletteBlock> ROULETTES = new EnumMap<>(RouletteColor.class);
	/** Ruleta de colores: la rueda decide el color. */
	public static RouletteBlock ROULETTE_RANDOM;
	/** Fogata del Ritual (más grande que la vanilla), necesaria para usar la Cuchara de la Resurrección. */
	public static RitualCampfireBlock RITUAL_CAMPFIRE;

	/** Plataforma de Despegue: la nave espacial solo se coloca sobre ella. */
	public static net.minecraft.block.Block PLATFORM;

	private ModBlocks() {
	}

	private static AbstractBlock.Settings settings(MapColor mapColor) {
		return AbstractBlock.Settings.create()
				.mapColor(mapColor)
				.strength(2.5f, 6.0f)
				.sounds(BlockSoundGroup.METAL)
				.luminance(state -> 8)
				.pistonBehavior(PistonBehavior.NORMAL);
	}

	private static RouletteBlock registerBlock(String name, RouletteColor color, MapColor mapColor) {
		RouletteBlock block = Registry.register(Registries.BLOCK, Dedsafio3.id(name), new RouletteBlock(color, settings(mapColor)));
		Registry.register(Registries.ITEM, Dedsafio3.id(name), new BlockItem(block, new Item.Settings()));
		return block;
	}

	public static void register() {
		RITUAL_CAMPFIRE = Registry.register(Registries.BLOCK, Dedsafio3.id("fogata_ritual"), new RitualCampfireBlock(
				AbstractBlock.Settings.create().mapColor(MapColor.BROWN).strength(2.0f).sounds(BlockSoundGroup.WOOD)
						.luminance(state -> 15).nonOpaque()));
		Registry.register(Registries.ITEM, Dedsafio3.id("fogata_ritual"), new BlockItem(RITUAL_CAMPFIRE, new Item.Settings()));
		PLATFORM = Registry.register(Registries.BLOCK, Dedsafio3.id("plataforma_despegue"), new net.minecraft.block.Block(
				AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).strength(5.0f, 8.0f).sounds(BlockSoundGroup.METAL)
						.requiresTool()));
		Registry.register(Registries.ITEM, Dedsafio3.id("plataforma_despegue"), new BlockItem(PLATFORM, new Item.Settings()));
		ROULETTE_RANDOM = registerBlock("roulette_random", null, MapColor.GOLD);
		for (RouletteColor color : RouletteColor.values()) {
			ROULETTES.put(color, registerBlock("roulette_" + color.id(), color, color.mapColor()));
		}
	}
}
