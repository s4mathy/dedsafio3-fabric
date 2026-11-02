package com.dedsafio3;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;

public final class ModEffects {
	public static RegistryEntry<StatusEffect> PRIMITIVE_POISON;
	public static RegistryEntry<StatusEffect> KEEP_INVENTORY;

	private ModEffects() {
	}

	public static void register() {
		PRIMITIVE_POISON = Registry.registerReference(Registries.STATUS_EFFECT, Dedsafio3.id("veneno_primitivo"),
				new PrimitivePoisonEffect());
		KEEP_INVENTORY = Registry.registerReference(Registries.STATUS_EFFECT, Dedsafio3.id("keep_inventory"),
				new KeepInventoryEffect());
	}
}
