package com.dedsafio3.client;

import com.dedsafio3.DeathAnimPayload;
import com.dedsafio3.Dedsafio3;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/**
 * Animación de muerte.
 * - Quien muere: parpadeo rojo, pantalla oscura, calavera animada (12 fotogramas) y "HAS MUERTO".
 * - El resto de jugadores: un aviso compacto arriba con la calavera y el nombre.
 *
 * Los fotogramas son texturas 64x64 en assets/dedsafio3/textures/gui/death/frame_00..11.png;
 * puedes sustituirlos por los tuyos sin tocar el código.
 */
public final class DeathOverlay {
	public static final int FRAME_COUNT = 12;
	private static final long FRAME_MS = 110;
	private static final long SELF_TOTAL_MS = 5200;
	private static final long OTHER_TOTAL_MS = 3600;
	private static final long FADE_MS = 800;

	private static final Identifier[] FRAMES = new Identifier[FRAME_COUNT];

	static {
		for (int i = 0; i < FRAME_COUNT; i++) {
			FRAMES[i] = Dedsafio3.id(String.format("textures/gui/death/frame_%02d.png", i));
		}
	}

	private static boolean active;
	private static boolean self;
	private static String name = "";
	private static long startMs;

	private DeathOverlay() {
	}

	public static void start(DeathAnimPayload payload) {
		self = payload.self();
		name = payload.name();
		startMs = Util.getMeasuringTimeMs();
		active = true;
		if (self) {
			MinecraftClient.getInstance().getSoundManager().play(
					PositionedSoundInstance.master(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 0.9f));
		}
	}

	public static void render(DrawContext ctx) {
		if (!active) {
			return;
		}
		long elapsed = Util.getMeasuringTimeMs() - startMs;
		long total = self ? SELF_TOTAL_MS : OTHER_TOTAL_MS;
		if (elapsed > total) {
			active = false;
			return;
		}
		float fade = elapsed > total - FADE_MS ? (total - elapsed) / (float) FADE_MS : 1f;
		if (self) {
			renderSelf(ctx, elapsed, fade);
		} else {
			renderOther(ctx, elapsed, fade);
		}
	}

	private static void renderSelf(DrawContext ctx, long elapsed, float fade) {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		int sw = ctx.getScaledWindowWidth();
		int sh = ctx.getScaledWindowHeight();

		// Oscurecimiento progresivo + destello rojo inicial + viñeta.
		float dark = Math.min(1f, elapsed / 700f);
		ctx.fill(0, 0, sw, sh, argb((int) (dark * 175 * fade), 0x000000));
		float flash = Math.max(0f, 1f - elapsed / 350f);
		if (flash > 0f) {
			ctx.fill(0, 0, sw, sh, argb((int) (flash * 170), 0xB00000));
		}
		ctx.fillGradient(0, 0, sw, sh / 3, argb((int) (200 * fade), 0x000000), 0x00000000);
		ctx.fillGradient(0, sh * 2 / 3, sw, sh, 0x00000000, argb((int) (200 * fade), 0x000000));

		// Calavera animada: se reproduce una vez y luego late sobre el último fotograma.
		int frame = (int) Math.min(FRAME_COUNT - 1, elapsed / FRAME_MS);
		long afterFrames = elapsed - FRAME_COUNT * FRAME_MS;
		float pulse = afterFrames > 0 ? 1f + 0.04f * (float) Math.sin(afterFrames / 140.0) : 1f;
		int size = (int) (128 * pulse);
		int x = (sw - size) / 2;
		int y = sh / 2 - size / 2 - 24;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		ctx.setShaderColor(1f, 1f, 1f, fade);
		ctx.drawTexture(FRAMES[frame], x, y, size, size, 0f, 0f, 64, 64, 64, 64);
		ctx.setShaderColor(1f, 1f, 1f, 1f);
		RenderSystem.disableBlend();

		// Texto "HAS MUERTO" (aparece a los 0.7 s).
		float textAlpha = Math.min(1f, Math.max(0f, (elapsed - 700f) / 500f)) * fade;
		if (textAlpha > 0.02f) {
			int a = Math.max(4, (int) (textAlpha * 255));
			ctx.getMatrices().push();
			ctx.getMatrices().translate(sw / 2f, y + size + 10f, 0f);
			ctx.getMatrices().scale(3f, 3f, 1f);
			ctx.drawCenteredTextWithShadow(font, Text.translatable("death.dedsafio3.you"), 0, 0, argb(a, 0xE53935));
			ctx.getMatrices().pop();
		}
	}

	private static void renderOther(DrawContext ctx, long elapsed, float fade) {
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		int sw = ctx.getScaledWindowWidth();

		Text message = Text.translatable("death.dedsafio3.other", name);
		int textW = font.getWidth(message);
		int boxW = textW + 56;
		int boxH = 30;

		// Entra deslizando desde arriba durante los primeros 300 ms.
		float slide = Math.min(1f, elapsed / 300f);
		int y = (int) (-boxH + (boxH + 12) * slide);
		int x = (sw - boxW) / 2;

		ctx.fill(x, y, x + boxW, y + boxH, argb((int) (170 * fade), 0x000000));
		ctx.fill(x, y + boxH - 2, x + boxW, y + boxH, argb((int) (255 * fade), 0xB00000));

		int frame = (int) ((elapsed / FRAME_MS) % FRAME_COUNT);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		ctx.setShaderColor(1f, 1f, 1f, fade);
		ctx.drawTexture(FRAMES[frame], x + 6, y + 3, 24, 24, 0f, 0f, 64, 64, 64, 64);
		ctx.setShaderColor(1f, 1f, 1f, 1f);
		RenderSystem.disableBlend();

		ctx.drawTextWithShadow(font, message, x + 40, y + (boxH - 8) / 2 - 1, argb(Math.max(4, (int) (255 * fade)), 0xFFFFFF));
	}

	private static int argb(int alpha, int rgb) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
	}
}
