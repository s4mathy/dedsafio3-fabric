package com.dedsafio3;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playS2C().register(RoulettePayload.ID, RoulettePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(DeathAnimPayload.ID, DeathAnimPayload.CODEC);
	}
}
