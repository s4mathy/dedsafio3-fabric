package com.dedsafio3.client;

import com.dedsafio3.RouletteColor;
import com.dedsafio3.RoulettePayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Animación de la ruleta: una tira de casillas que se desplaza y frena hasta dejar
 * la casilla ganadora bajo el marcador central.
 */
public final class RouletteOverlay {
	private static final int CELL_W = 78;
	private static final int CELL_H = 40;
	private static final long HOLD_MS = 2200;
	private static final long FADE_MS = 400;

	private static boolean active;
	private static long startMs;
	private static long spinMs;
	private static int rgb;
	private static Text title = Text.empty();
	private static final List<Text> LABELS = new ArrayList<>();
	private static int winner;
	private static float jitter;
	private static int lastCell;
	private static boolean finishedSoundPlayed;

	private RouletteOverlay() {
	}

	public static void start(RoulettePayload payload) {
		RouletteColor color = RouletteColor.byOrdinal(payload.color());
		rgb = color.rgb();
		title = Text.translatable(color.blockTranslationKey());
		LABELS.clear();
		for (String key : payload.keys()) {
			LABELS.add(Text.translatable(key));
		}
		winner = Math.max(0, Math.min(payload.winner(), LABELS.size() - 1));
		spinMs = payload.spinTicks() * 50L;
		// La casilla ganadora no siempre queda exactamente centrada, como en una ruleta real.
		jitter = (MinecraftClient.getInstance().world != null
				? MinecraftClient.getInstance().world.random.nextFloat() - 0.5f : 0f) * CELL_W * 0.6f;
		startMs = Util.getMeasuringTimeMs();
		lastCell = -1;
		finishedSoundPlayed = false;
		active = !LABELS.isEmpty();
	}

	public static void render(DrawContext ctx) {
		if (!active) {
			return;
		}
		long elapsed = Util.getMeasuringTimeMs() - startMs;
		long total = spinMs + HOLD_MS;
		if (elapsed > total) {
			active = false;
			return;
		}

		MinecraftClient mc = MinecraftClient.getInstance();
		TextRenderer font = mc.textRenderer;
		int sw = ctx.getScaledWindowWidth();
		int sh = ctx.getScaledWindowHeight();

		float progress = Math.min(1f, elapsed / (float) spinMs);
		float eased = 1f - (float) Math.pow(1f - progress, 3);
		float target = winner * CELL_W + CELL_W / 2f + jitter;
		float offset = eased * target;

		float alpha = elapsed > total - FADE_MS ? (total - elapsed) / (float) FADE_MS : 1f;
		int a = Math.max(4, (int) (alpha * 255));

		int panelW = Math.min(sw - 20, 380);
		int x0 = (sw - panelW) / 2;
		int x1 = x0 + panelW;
		int yTitle = Math.max(8, sh / 8);
		int y0 = yTitle + 16;
		int y1 = y0 + CELL_H;
		int center = sw / 2;

		// Título y marco.
		ctx.drawCenteredTextWithShadow(font, title, center, yTitle, argb(a, rgb));
		ctx.fill(x0 - 3, y0 - 3, x1 + 3, y1 + 3, argb((int) (a * 0.85f), 0x101010));
		ctx.fill(x0 - 3, y0 - 3, x1 + 3, y0 - 1, argb(a, rgb));
		ctx.fill(x0 - 3, y1 + 1, x1 + 3, y1 + 3, argb(a, rgb));

		// Tira de casillas (recortada al marco).
		int current = (int) Math.floor(offset / CELL_W);
		ctx.enableScissor(x0, y0, x1, y1);
		for (int i = 0; i < LABELS.size(); i++) {
			int left = Math.round(center - offset + i * CELL_W);
			if (left + CELL_W < x0 || left > x1) {
				continue;
			}
			int shade = (i % 2 == 0) ? 0x000000 : 0x202020;
			int base = darken(rgb, 0.55f);
			ctx.fill(left + 1, y0, left + CELL_W - 1, y1, argb((int) (a * 0.9f), blend(base, shade)));

			List<OrderedText> lines = font.wrapLines(LABELS.get(i), CELL_W - 8);
			int lineCount = Math.min(3, lines.size());
			int textY = y0 + (CELL_H - lineCount * 9) / 2;
			for (int l = 0; l < lineCount; l++) {
				ctx.drawCenteredTextWithShadow(font, lines.get(l), left + CELL_W / 2, textY + l * 9, argb(a, 0xFFFFFF));
			}
			if (progress >= 1f && i == winner) {
				int gold = argb(a, 0xFFD54F);
				ctx.fill(left + 1, y0, left + CELL_W - 1, y0 + 2, gold);
				ctx.fill(left + 1, y1 - 2, left + CELL_W - 1, y1, gold);
				ctx.fill(left + 1, y0, left + 3, y1, gold);
				ctx.fill(left + CELL_W - 3, y0, left + CELL_W - 1, y1, gold);
			}
		}
		ctx.disableScissor();

		// Marcador central.
		ctx.fill(center - 1, y0 - 8, center + 1, y1 + 8, argb(a, 0xFFFFFF));
		ctx.fill(center - 4, y0 - 8, center + 4, y0 - 6, argb(a, 0xFFFFFF));
		ctx.fill(center - 4, y1 + 6, center + 4, y1 + 8, argb(a, 0xFFFFFF));

		// Resultado.
		if (progress >= 1f) {
			ctx.drawCenteredTextWithShadow(font, LABELS.get(winner), center, y1 + 14, argb(a, 0xFFD54F));
		}

		// Sonidos: "tic" al cambiar de casilla y fanfarria al final.
		if (progress < 1f && current != lastCell) {
			lastCell = current;
			float pitch = 0.8f + 0.7f * eased;
			mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ITEM_PICKUP, pitch, 0.5f));
		}
		if (progress >= 1f && !finishedSoundPlayed) {
			finishedSoundPlayed = true;
			mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 0.7f));
		}
	}

	private static int argb(int alpha, int rgb) {
		return (alpha << 24) | (rgb & 0xFFFFFF);
	}

	private static int darken(int color, float factor) {
		int r = (int) (((color >> 16) & 0xFF) * factor);
		int g = (int) (((color >> 8) & 0xFF) * factor);
		int b = (int) ((color & 0xFF) * factor);
		return (r << 16) | (g << 8) | b;
	}

	private static int blend(int base, int add) {
		int r = Math.min(255, ((base >> 16) & 0xFF) + ((add >> 16) & 0xFF));
		int g = Math.min(255, ((base >> 8) & 0xFF) + ((add >> 8) & 0xFF));
		int b = Math.min(255, (base & 0xFF) + (add & 0xFF));
		return (r << 16) | (g << 8) | b;
	}
}
