package com.dedsafio3.client;

import com.dedsafio3.ConfigPayload;
import com.dedsafio3.DeathAnimPayload;
import com.dedsafio3.Dedsafio3;
import com.dedsafio3.MenuRequestPayload;
import com.dedsafio3.RoulettePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Dedsafio3Client implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(RoulettePayload.ID,
				(payload, context) -> RouletteOverlay.start(payload));
		ClientPlayNetworking.registerGlobalReceiver(DeathAnimPayload.ID,
				(payload, context) -> DeathOverlay.start(payload));

		// Estado del menú: abre la pantalla o refresca la que ya está abierta.
		ClientPlayNetworking.registerGlobalReceiver(ConfigPayload.ID, (payload, context) -> {
			MinecraftClient client = context.client();
			try {
				MenuState state = MenuState.parse(payload.json());
				if (client.currentScreen instanceof DedsafioMenuScreen screen) {
					screen.update(state);
				} else if (payload.open()) {
					client.setScreen(new DedsafioMenuScreen(state));
				}
			} catch (Exception e) {
				Dedsafio3.LOGGER.error("Estado de menú inválido", e);
			}
		});

		// Tecla K: pide el menú al servidor (el servidor comprueba los permisos).
		KeyBinding menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.dedsafio3.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.dedsafio3"));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (menuKey.wasPressed()) {
				if (client.player != null && client.currentScreen == null) {
					ClientPlayNetworking.send(new MenuRequestPayload());
				}
			}
		});

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
