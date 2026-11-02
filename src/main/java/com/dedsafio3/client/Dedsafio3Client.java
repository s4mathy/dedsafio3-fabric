package com.dedsafio3.client;

import com.dedsafio3.ConfigPayload;
import com.dedsafio3.DeathAnimPayload;
import com.dedsafio3.Dedsafio3;
import com.dedsafio3.MenuRequestPayload;
import com.dedsafio3.ReviilPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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

		ClientPlayNetworking.registerGlobalReceiver(com.dedsafio3.FlashPayload.ID,
				(payload, context) -> FlashOverlay.start(payload.ticks()));
		ModRenderers.register();
		ClientPlayNetworking.registerGlobalReceiver(com.dedsafio3.SoulsPayload.ID, (payload, context) -> {
			java.util.Set<java.util.UUID> set = new java.util.HashSet<>();
			for (String id : payload.holders().split(",")) {
				try {
					set.add(java.util.UUID.fromString(id.trim()));
				} catch (IllegalArgumentException ignored) {
					// vacío u otro formato: se ignora
				}
			}
			com.dedsafio3.SoulIcons.set(set);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> com.dedsafio3.SoulIcons.set(java.util.Set.of()));
		ClientPlayNetworking.registerGlobalReceiver(com.dedsafio3.RitualPayload.ID,
				(payload, context) -> context.client().setScreen(new RitualScreen(payload.souls())));
		ClientPlayNetworking.registerGlobalReceiver(ReviilPayload.ID,
				(payload, context) -> ReviilClientState.hideHealth = payload.seconds() > 0);
		// Al salir del mundo se restablece, por si el servidor se cerró en pleno Momento Reviil.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ReviilClientState.hideHealth = false);

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
			FlashOverlay.render(drawContext);
		});

		// Sobre cualquier pantalla (inventario, pantalla de muerte, etc.).
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
				ScreenEvents.afterRender(screen).register((s, drawContext, mouseX, mouseY, tickDelta) -> {
					DeathOverlay.render(drawContext);
					RouletteOverlay.render(drawContext);
					FlashOverlay.render(drawContext);
				}));
	}
}
