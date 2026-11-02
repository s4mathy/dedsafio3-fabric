package com.dedsafio3.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;

/** Pantalla en blanco del Flashbang: opaca al principio y se desvanece al final. */
public final class FlashOverlay {
	private static long endMs;
	private static long totalMs = 1;

	private FlashOverlay() {
	}

	public static void start(int ticks) {
		totalMs = Math.max(1, ticks * 50L);
		endMs = Util.getMeasuringTimeMs() + totalMs;
	}

	public static void render(DrawContext ctx) {
		long now = Util.getMeasuringTimeMs();
		if (now >= endMs) {
			return;
		}
		float left = (endMs - now) / (float) totalMs;
		float alpha = left > 0.4f ? 1.0f : left / 0.4f;
		int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
		ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), (a << 24) | 0xFFFFFF);
	}
}
