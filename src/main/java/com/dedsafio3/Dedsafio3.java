package com.dedsafio3;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dedsafio3 implements ModInitializer {
	public static final String MOD_ID = "dedsafio3";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// El orden importa: los bloques antes que los ítems (el grupo creativo los usa).
		DedsafioConfig.load();
		ServerLifecycleEvents.SERVER_STARTING.register(DedsafioConfig::loadWorld);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> DedsafioConfig.unloadWorld());
		ModEffects.register();
		ModEntities.register();
		ModBlocks.register();
		ModItems.register();
		ModNetworking.register();
		TotemHandler.register();
		DeathAnimation.register();
		Roulette.register();
		WorldModifier.register();
		ChangeHooks.register();
		ModSpawner.register();
		ReviilEvent.register();
		Resurrection.register();
		MenuService.register();
		ModCommands.register();
		LOGGER.info("Dedsafio 3 (fan mod) cargado.");
	}
}
