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
	public static EntityType<MagmaCreeperEntity> MAGMA_CREEPER;
	public static EntityType<DactiloEntity> DACTILO;
	public static EntityType<NarvalEntity> NARVAL;
	public static EntityType<HerobrineEntity> HEROBRINE;
	public static EntityType<WardenBombEntity> WARDEN_BOMB;
	public static EntityType<GrassCreeperEntity> GRASS_CREEPER;
	public static EntityType<PhoraEntity> POD_PHORA;
	public static EntityType<PhoraEntity> SKIFF_PHORA;
	public static EntityType<WitherRoseCreeperEntity> WITHER_ROSE_CREEPER;
	public static EntityType<DactiloAdultEntity> DACTILO_ADULT;
	public static EntityType<SpaceshipEntity> SPACESHIP;
	public static final Map<GlebaMobEntity.Kind, EntityType<GlebaMobEntity>> GLEBA_MOBS = new EnumMap<>(GlebaMobEntity.Kind.class);
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
			boolean small = kind == ReptilianEntity.Kind.SALVAJE;
			EntityType<ReptilianEntity> type = register(kind.id, EntityType.Builder
					.<ReptilianEntity>create((t, world) -> new ReptilianEntity(t, world, kind), SpawnGroup.MONSTER)
					.dimensions(small ? 0.5f : 0.6f, small ? 1.1f : 1.95f).maxTrackingRange(8));
			FabricDefaultAttributeRegistry.register(type, ReptilianEntity.attributes(kind));
			REPTILIANS.put(kind, type);
			int primary = kind == ReptilianEntity.Kind.DROMORAPTOR ? 0xB03020
					: kind == ReptilianEntity.Kind.DROMORAPTOR_AZUL ? 0x2E6FD0
					: kind == ReptilianEntity.Kind.DROMORAPTOR_VERDE ? 0x4CAF50 : 0x3E6B2A;
			egg(kind.id, type, primary, 0x1A2A12);
		}

		MAGMA_CREEPER = register("creeper_magma", EntityType.Builder.create(MagmaCreeperEntity::new, SpawnGroup.MONSTER)
				.dimensions(0.6f, 1.7f).makeFireImmune().maxTrackingRange(8));
		FabricDefaultAttributeRegistry.register(MAGMA_CREEPER, MagmaCreeperEntity.attributes());
		egg("creeper_magma", MAGMA_CREEPER, 0xD2501A, 0x3A0F0F);

		DACTILO = register("dactilo_bebe", EntityType.Builder.create(DactiloEntity::new, SpawnGroup.MONSTER)
				.dimensions(0.8f, 0.7f).maxTrackingRange(10));
		FabricDefaultAttributeRegistry.register(DACTILO, DactiloEntity.attributes());
		egg("dactilo_bebe", DACTILO, 0xC98A3C, 0x5A3B1A);

		NARVAL = register("narval", EntityType.Builder.create(NarvalEntity::new, SpawnGroup.WATER_CREATURE)
				.dimensions(0.9f, 0.7f).maxTrackingRange(10));
		FabricDefaultAttributeRegistry.register(NARVAL, NarvalEntity.attributes());
		egg("narval", NARVAL, 0x7C8FA8, 0xE9E4D0);

		HEROBRINE = register("herobrine", EntityType.Builder.create(HerobrineEntity::new, SpawnGroup.MONSTER)
				.dimensions(0.6f, 1.8f).makeFireImmune().maxTrackingRange(10));
		FabricDefaultAttributeRegistry.register(HEROBRINE, HerobrineEntity.attributes());
		egg("herobrine", HEROBRINE, 0x2B2B2B, 0xFFFFFF);

		WARDEN_BOMB = register("bomba_warden", EntityType.Builder.create(WardenBombEntity::new, SpawnGroup.MONSTER)
				.dimensions(0.7f, 0.7f).maxTrackingRange(10));
		FabricDefaultAttributeRegistry.register(WARDEN_BOMB, WardenBombEntity.attributes());
		egg("bomba_warden", WARDEN_BOMB, 0x0C2E36, 0x2AD4C4);

		GRASS_CREEPER = register("creeper_pasto", EntityType.Builder.create(GrassCreeperEntity::new, SpawnGroup.MONSTER)
				.dimensions(0.4f, 1.0f).maxTrackingRange(8));
		FabricDefaultAttributeRegistry.register(GRASS_CREEPER, GrassCreeperEntity.attributes());
		egg("creeper_pasto", GRASS_CREEPER, 0x3E9B2E, 0x1B4A14);

		POD_PHORA = register("pod_phora", EntityType.Builder.<PhoraEntity>create((t, w) -> new PhoraEntity(t, w, true), SpawnGroup.MONSTER)
				.dimensions(0.9f, 0.7f).maxTrackingRange(10));
		FabricDefaultAttributeRegistry.register(POD_PHORA, PhoraEntity.attributes(true));
		egg("pod_phora", POD_PHORA, 0x3A3F4A, 0xE04040);

		SKIFF_PHORA = register("skiff_phora", EntityType.Builder.<PhoraEntity>create((t, w) -> new PhoraEntity(t, w, false), SpawnGroup.MONSTER)
				.dimensions(1.0f, 0.8f).maxTrackingRange(10));
		FabricDefaultAttributeRegistry.register(SKIFF_PHORA, PhoraEntity.attributes(false));
		egg("skiff_phora", SKIFF_PHORA, 0x4A3A2A, 0xFF9020);

		WITHER_ROSE_CREEPER = register("creeper_wither_rose", EntityType.Builder.create(WitherRoseCreeperEntity::new, SpawnGroup.MONSTER)
				.dimensions(0.6f, 1.7f).makeFireImmune().maxTrackingRange(8));
		FabricDefaultAttributeRegistry.register(WITHER_ROSE_CREEPER, WitherRoseCreeperEntity.attributes());
		egg("creeper_wither_rose", WITHER_ROSE_CREEPER, 0x1C1C1C, 0x6B1B1B);

		DACTILO_ADULT = register("dactilo_adulto", EntityType.Builder.create(DactiloAdultEntity::new, SpawnGroup.MONSTER)
				.dimensions(1.6f, 1.0f).maxTrackingRange(12));
		FabricDefaultAttributeRegistry.register(DACTILO_ADULT, DactiloAdultEntity.attributes());
		egg("dactilo_adulto", DACTILO_ADULT, 0x8B5A2B, 0x2E1A0A);

		for (GlebaMobEntity.Kind kind : GlebaMobEntity.Kind.values()) {
			EntityType<GlebaMobEntity> type = register(kind.id, EntityType.Builder
					.<GlebaMobEntity>create((t, world) -> new GlebaMobEntity(t, world, kind), SpawnGroup.MONSTER)
					.dimensions(kind.width, kind.height).maxTrackingRange(8));
			FabricDefaultAttributeRegistry.register(type, GlebaMobEntity.attributes(kind));
			GLEBA_MOBS.put(kind, type);
			egg(kind.id, type, 0x6B8E3A, 0xD9C45A);
		}

		SPACESHIP = register("nave_espacial", EntityType.Builder.create(SpaceshipEntity::new, SpawnGroup.MISC)
				.dimensions(1.6f, 3.6f).makeFireImmune().maxTrackingRange(10));
		FabricDefaultAttributeRegistry.register(SPACESHIP, SpaceshipEntity.attributes());
	}
}
