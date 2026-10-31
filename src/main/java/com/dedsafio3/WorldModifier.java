package com.dedsafio3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;

import java.util.ArrayList;
import java.util.List;

/**
 * Modificaciones persistentes del mundo (la ruleta "anuncia modificaciones que hacen el mundo más difícil").
 * Quedan guardadas en la configuración y se pueden activar o quitar desde el menú.
 */
public enum WorldModifier {
	MOBS_STRONGER("mobs_stronger"),
	NO_REGEN("no_regen"),
	ETERNAL_NIGHT("eternal_night");

	private final String id;

	WorldModifier(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public String translationKey() {
		return "modifier." + Dedsafio3.MOD_ID + "." + id;
	}

	public boolean isActive() {
		return DedsafioConfig.get().modifiers.contains(id);
	}

	public void set(MinecraftServer server, boolean on) {
		List<String> list = DedsafioConfig.get().modifiers;
		list.remove(id);
		if (on) {
			list.add(id);
		}
		DedsafioConfig.save();
		applyRule(server, on);
	}

	private void applyRule(MinecraftServer server, boolean on) {
		switch (this) {
			case NO_REGEN -> server.getGameRules().get(GameRules.NATURAL_REGENERATION).set(!on, server);
			case ETERNAL_NIGHT -> {
				server.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(!on, server);
				if (on) {
					for (ServerWorld world : server.getWorlds()) {
						world.setTimeOfDay(18000L);
					}
				}
			}
			case MOBS_STRONGER -> {
				// Se aplica a los mobs hostiles cuando cargan (ver register()).
			}
		}
	}

	public static WorldModifier byId(String id) {
		for (WorldModifier modifier : values()) {
			if (modifier.id.equals(id)) {
				return modifier;
			}
		}
		return null;
	}

	public static void clearAll(MinecraftServer server) {
		for (WorldModifier modifier : new ArrayList<>(List.of(values()))) {
			modifier.set(server, false);
		}
	}

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (MOBS_STRONGER.isActive() && entity instanceof HostileEntity mob) {
				if (!mob.hasStatusEffect(StatusEffects.STRENGTH)) {
					mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, StatusEffectInstance.INFINITE, 0, false, false));
				}
				if (!mob.hasStatusEffect(StatusEffects.SPEED)) {
					mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, StatusEffectInstance.INFINITE, 0, false, false));
				}
			}
		});
	}
}
