package com.dedsafio3.client;

import com.dedsafio3.DeathAnimPayload;
import com.dedsafio3.RoulettePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;

public class Dedsafio3Client implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(RoulettePayload.ID,
				(payload, context) -> RouletteOverlay.start(payload));
		ClientPlayNetworking.registerGlobalReceiver(DeathAnimPayload.ID,
				(payload, context) -> DeathOverlay.start(payload));

		// HUD normal (sin pantallas abiertas).
		HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
			if (MinecraftClient.getInstance().currentScreen == null) {
				DeathOverlay.render(drawContext);
				RouletteOverlay.render(drawContext);
			}
		});

		// Sobre cualquier pantalla (inventario, pantalla de muerte, etc.).
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
				ScreenEvents.afterRender(screen).register((s, drawContext, mouseX, mouseY, tickDelta) -> {
					DeathOverlay.render(drawContext);
					RouletteOverlay.render(drawContext);
				}));
	}
}
