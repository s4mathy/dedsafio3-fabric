package com.dedsafio3.client;

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

/** Introducción del Momento Reviil: pantalla oscura, destello rojo, calavera animada y el título "MOMENTO REVIIL". */
public final class ReviilIntro {
	private static final int FRAME_COUNT = DeathOverlay.FRAME_COUNT;
	private static final long FRAME_MS = 110;
	private static final long FADE_MS = 700;
	private static final Identifier[] FRAMES = new Identifier[FRAME_COUNT];

	static {
		for (int i = 0; i < FRAME_COUNT; i++) {
			FRAMES[i] = Dedsafio3.id(String.format("textures/gui/death/frame_%02d.png", i));
		}
	}

	private static long startMs;
	private static long totalMs;
	private static boolean active;

	private ReviilIntro() {
	}

	public static void start(int ticks) {
		totalMs = Math.max(1500, ticks * 50L);
		startMs = Util.getMeasuringTimeMs();
		active = true;
		MinecraftClient.getInstance().getSoundManager().play(
				PositionedSoundInstance.master(SoundEvents.ENTITY_WITHER_SPAWN, 0.8f, 0.7f));
	}

	public static void render(DrawContext ctx) {
		if (!active) {
			return;
		}
		long elapsed = Util.getMeasuringTimeMs() - startMs;
		if (elapsed > totalMs) {
			active = false;
			return;
		}
		float fade = elapsed > totalMs - FADE_MS ? (totalMs - elapsed) / (float) FADE_MS : 1f;
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		int sw = ctx.getScaledWindowWidth();
		int sh = ctx.getScaledWindowHeight();

		float dark = Math.min(1f, elapsed / 500f);
		ctx.fill(0, 0, sw, sh, argb((int) (dark * 200 * fade), 0x000000));
		float flash = Math.max(0f, 1f - elapsed / 300f);
		if (flash > 0f) {
			ctx.fill(0, 0, sw, sh, argb((int) (flash * 170), 0xB00000));
		}

		int frame = (int) Math.min(FRAME_COUNT - 1, elapsed / FRAME_MS);
		long after = elapsed - FRAME_COUNT * FRAME_MS;
		float pulse = after > 0 ? 1f + 0.05f * (float) Math.sin(after / 130.0) : 1f;
		int size = (int) (128 * pulse);
		int x = (sw - size) / 2;
		int y = sh / 2 - size / 2 - 24;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		ctx.setShaderColor(1f, 1f, 1f, fade);
		ctx.drawTexture(FRAMES[frame], x, y, size, size, 0f, 0f, 64, 64, 64, 64);
		ctx.setShaderColor(1f, 1f, 1f, 1f);
		RenderSystem.disableBlend();

		float textAlpha = Math.min(1f, Math.max(0f, (elapsed - 500f) / 400f)) * fade;
		if (textAlpha > 0.02f) {
			int a = Math.max(4, (int) (textAlpha * 255));
			ctx.getMatrices().push();
			ctx.getMatrices().translate(sw / 2f, y + size + 10f, 0f);
			ctx.getMatrices().scale(3f, 3f, 1f);
			ctx.drawCenteredTextWithShadow(font, Text.translatable("gui.dedsafio3.reviil_intro"), 0, 0, argb(a, 0xE53935));
			ctx.getMatrices().pop();
		}
	}

	private static int argb(int alpha, int rgb) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
	}
}
