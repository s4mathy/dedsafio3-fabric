package com.dedsafio3;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

import java.util.List;

public final class ModItems {
	// --- Economía: deditas (cada una es más rara que la anterior) ---
	public static final Item DEDITA = register("dedita", new TooltipItem(new Item.Settings().rarity(Rarity.COMMON)));
	public static final Item DEDITA_PLATA = register("dedita_plata", new TooltipItem(new Item.Settings().rarity(Rarity.UNCOMMON)));
	public static final Item DEDITA_ORO = register("dedita_oro", new TooltipItem(new Item.Settings().rarity(Rarity.RARE)));

	// --- Ruleta ---
	public static final Item FICHA_RULETA = register("ficha_ruleta",
			new RouletteTokenItem(new Item.Settings().rarity(Rarity.UNCOMMON)));

	// --- Tótems ---
	public static final Item TOTEM_ROJO = register("totem_rojo", new TotemItem(
			new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON), 6.0f, List.of(
			new TotemItem.Buff(StatusEffects.REGENERATION, 20, 1),
			new TotemItem.Buff(StatusEffects.FIRE_RESISTANCE, 40, 0),
			new TotemItem.Buff(StatusEffects.STRENGTH, 20, 0))));

	public static final Item TOTEM_AZUL = register("totem_azul", new TotemItem(
			new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON), 8.0f, List.of(
			new TotemItem.Buff(StatusEffects.WATER_BREATHING, 60, 0),
			new TotemItem.Buff(StatusEffects.DOLPHINS_GRACE, 60, 0),
			new TotemItem.Buff(StatusEffects.RESISTANCE, 20, 1))));

	public static final Item TOTEM_DORADO = register("totem_dorado", new TotemItem(
			new Item.Settings().maxCount(1).rarity(Rarity.EPIC), 20.0f, List.of(
			new TotemItem.Buff(StatusEffects.REGENERATION, 30, 1),
			new TotemItem.Buff(StatusEffects.ABSORPTION, 60, 2),
			new TotemItem.Buff(StatusEffects.RESISTANCE, 30, 1))));

	public static final List<Item> TOTEMS = List.of(TOTEM_ROJO, TOTEM_AZUL, TOTEM_DORADO);

	// --- Cuchara de la Resurrección: se usa en una fogata para devolver a la vida a un jugador muerto ---
	public static final Item CUCHARA_RESURRECCION = register("cuchara_resurreccion",
			new TooltipItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));

	// --- Tenedor de la Resurrección: como la Cuchara, pero no gasta el alma de quien hace el ritual ---
	public static final Item TENEDOR_RESURRECCION = register("tenedor_resurreccion",
			new TooltipItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));

	// --- Pesadillas: la Fruta quita el Insomnia y los Dáctylos Adultos; el Indicador muestra el nivel de Insomnia ---
	public static final Item FRUTA_ESPACIO = register("fruta_espacio", new SpaceFruitItem(new Item.Settings()
			.food(new FoodComponent.Builder().nutrition(6).saturationModifier(0.6f).alwaysEdible().build())
			.rarity(Rarity.EPIC)));
	public static final Item INDICADOR_INSOMNIA = register("indicador_insomnia",
			new TooltipItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));

	// --- Arma ---
	public static final Item ESPADA_CAOTICA = register("espada_caotica", new ChaosSwordItem(
			ToolMaterials.NETHERITE,
			new Item.Settings()
					.attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 3, -2.4f))
					.rarity(Rarity.EPIC)
					.fireproof()));

	public static final RegistryKey<ItemGroup> GROUP_KEY =
			RegistryKey.of(RegistryKeys.ITEM_GROUP, Dedsafio3.id("main"));

	private ModItems() {
	}

	private static Item register(String name, Item item) {
		return Registry.register(Registries.ITEM, Dedsafio3.id(name), item);
	}

	public static void register() {
		Registry.register(Registries.ITEM_GROUP, GROUP_KEY, FabricItemGroup.builder()
				.icon(() -> new ItemStack(DEDITA_ORO))
				.displayName(Text.translatable("itemGroup." + Dedsafio3.MOD_ID))
				.entries((context, entries) -> {
					entries.add(DEDITA);
					entries.add(DEDITA_PLATA);
					entries.add(DEDITA_ORO);
					entries.add(FICHA_RULETA);
					for (Item totem : TOTEMS) {
						entries.add(totem);
					}
					entries.add(CUCHARA_RESURRECCION);
					entries.add(TENEDOR_RESURRECCION);
					entries.add(FRUTA_ESPACIO);
					entries.add(INDICADOR_INSOMNIA);
					entries.add(ModBlocks.RITUAL_CAMPFIRE);
					entries.add(ESPADA_CAOTICA);
					for (Item egg : ModEntities.EGGS) {
						entries.add(egg);
					}
					entries.add(ModBlocks.ROULETTE_RANDOM);
					for (RouletteBlock block : ModBlocks.ROULETTES.values()) {
						entries.add(block);
					}
				})
				.build());
	}
}
