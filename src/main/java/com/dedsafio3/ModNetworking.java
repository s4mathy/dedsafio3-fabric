package com.dedsafio3;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void register() {
		// Servidor -> cliente
		PayloadTypeRegistry.playS2C().register(RoulettePayload.ID, RoulettePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(DeathAnimPayload.ID, DeathAnimPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ConfigPayload.ID, ConfigPayload.CODEC);
		// Cliente -> servidor
		PayloadTypeRegistry.playC2S().register(MenuRequestPayload.ID, MenuRequestPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SettingPayload.ID, SettingPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ActionPayload.ID, ActionPayload.CODEC);
	}
}
