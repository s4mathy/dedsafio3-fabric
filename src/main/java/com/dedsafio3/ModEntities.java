package com.dedsafio3;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModEntities {
	public static EntityType<NuclearCreeperEntity> NUCLEAR_CREEPER;
	public static EntityType<PlumosaurioEntity> PLUMOSAURIO;
	public static final Map<ReptilianEntity.Kind, EntityType<ReptilianEntity>> REPTILIANS = new EnumMap<>(ReptilianEntity.Kind.class);
	/** Huevos de aparición (para el modo creativo). */
	public static final List<Item> EGGS = new ArrayList<>();

	private ModEntities() {
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		Identifier id = Dedsafio3.id(name);
		return Registry.register(Registries.ENTITY_TYPE, id, builder.build(id.toString()));
	}

	private static void egg(String name, EntityType<? extends MobEntity> type, int primary, int secondary) {
		EGGS.add(Registry.register(Registries.ITEM, Dedsafio3.id("spawn_" + name),
				new SpawnEggItem(type, primary, secondary, new Item.Settings())));
	}

	public static void register() {
		NUCLEAR_CREEPER = register("creeper_nuclear", EntityType.Builder.create(NuclearCreeperEntity::new, SpawnGroup.MONSTER)
				.dimensions(0.7f, 1.9f).maxTrackingRange(8));
		FabricDefaultAttributeRegistry.register(NUCLEAR_CREEPER, NuclearCreeperEntity.attributes());
		egg("creeper_nuclear", NUCLEAR_CREEPER, 0xE8D21A, 0x1A1A1A);

		PLUMOSAURIO = register("plumosaurio", EntityType.Builder.create(PlumosaurioEntity::new, SpawnGroup.CREATURE)
				.dimensions(1.4f, 2.7f).maxTrackingRange(10));
		FabricDefaultAttributeRegistry.register(PLUMOSAURIO, PlumosaurioEntity.attributes());
		egg("plumosaurio", PLUMOSAURIO, 0x7B3FA0, 0xC9A0DC);

		for (ReptilianEntity.Kind kind : ReptilianEntity.Kind.values()) {
			EntityType<ReptilianEntity> type = register(kind.id, EntityType.Builder
					.<ReptilianEntity>create((t, world) -> new ReptilianEntity(t, world, kind), SpawnGroup.MONSTER)
					.dimensions(0.6f, 1.95f).maxTrackingRange(8));
			FabricDefaultAttributeRegistry.register(type, ReptilianEntity.attributes(kind));
			REPTILIANS.put(kind, type);
			egg(kind.id, type, kind == ReptilianEntity.Kind.DROMORAPTOR ? 0xB03020 : 0x3E6B2A, 0x1A2A12);
		}
	}
}
