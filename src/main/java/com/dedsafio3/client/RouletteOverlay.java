package com.dedsafio3.client;

import com.dedsafio3.RouletteColor;
import com.dedsafio3.RoulettePayload;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Rueda circular de colores. Los sectores tienen un ángulo proporcional a su peso, la rueda gira y frena
 * hasta dejar el color ganador bajo el puntero (arriba). Al terminar muestra el color y su premio.
 */
public final class RouletteOverlay {
	private static final long HOLD_MS = 3200;
	private static final long FADE_MS = 400;
	private static final float SECTOR_GAP_DEG = 1.6f;
	private static final int FULL_TURNS = 4;

	private static boolean active;
	private static long startMs;
	private static long spinMs;
	private static final List<Integer> COLORS = new ArrayList<>();
	private static final List<Float> STARTS = new ArrayList<>(); // grados, inicio de cada sector
	private static final List<Float> ENDS = new ArrayList<>();
	private static int winner;
	private static float finalRotation;
	private static Text prizeText = Text.empty();
	private static Text colorText = Text.empty();
	private static int lastSector;
	private static boolean finishedSoundPlayed;

	private RouletteOverlay() {
	}

	public static void start(RoulettePayload payload) {
		COLORS.clear();
		STARTS.clear();
		ENDS.clear();
		COLORS.addAll(payload.colors());
		float total = 0f;
		for (int weight : payload.weights()) {
			total += Math.max(1, weight);
		}
		float acc = 0f;
		for (int weight : payload.weights()) {
			float width = Math.max(1, weight) / total * 360f;
			STARTS.add(acc);
			ENDS.add(acc + width);
			acc += width;
		}
		winner = Math.max(0, Math.min(payload.winner(), COLORS.size() - 1));
		float target = STARTS.get(winner) + payload.frac() * (ENDS.get(winner) - STARTS.get(winner));
		// El punto 'target' de la rueda debe quedar bajo el puntero (ángulo 0, arriba).
		finalRotation = 360f * FULL_TURNS - target;
		spinMs = payload.spinTicks() * 50L;
		RouletteColor color = RouletteColor.byOrdinal(COLORS.get(winner));
		colorText = Text.translatable(color.nameKey());
		prizeText = Text.translatable(payload.prizeKey());
		startMs = Util.getMeasuringTimeMs();
		lastSector = -1;
		finishedSoundPlayed = false;
		active = !COLORS.isEmpty() && COLORS.size() == STARTS.size();
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
		float eased = 1f - (float) Math.pow(1f - progress, 4);
		float rotation = finalRotation * eased;

		float alpha = elapsed > total - FADE_MS ? (total - elapsed) / (float) FADE_MS : 1f;
		int a = Math.max(4, Math.min(255, (int) (alpha * 255)));

		float radius = Math.min(72f, Math.min(sw, sh) * 0.28f);
		float cx = sw / 2f;
		float cy = sh / 2f - 6f;

		// Sector bajo el puntero (para el "tic").
		float under = (((-rotation) % 360f) + 360f) % 360f;
		int current = 0;
		for (int i = 0; i < STARTS.size(); i++) {
			if (under >= STARTS.get(i) && under < ENDS.get(i)) {
				current = i;
				break;
			}
		}

		// Fondo y rueda.
		fan(ctx, cx, cy, radius + 9f, 0f, 360f, argb((int) (a * 0.75f), 0x000000));
		for (int i = 0; i < COLORS.size(); i++) {
			float a0 = rotation + STARTS.get(i);
			float a1 = rotation + ENDS.get(i);
			float gap = Math.min(SECTOR_GAP_DEG, (a1 - a0) * 0.25f);
			int rgb = RouletteColor.byOrdinal(COLORS.get(i)).rgb();
			boolean dim = progress >= 1f && i != winner;
			fan(ctx, cx, cy, radius, a0 + gap, a1 - gap, argb(a, dim ? shade(rgb, 0.45f) : rgb));
		}
		// Aro dorado, resaltado del ganador, cubo central.
		ring(ctx, cx, cy, radius, radius + 4f, 0f, 360f, argb(a, 0xD4AF37));
		if (progress >= 1f) {
			ring(ctx, cx, cy, radius - 6f, radius, rotation + STARTS.get(winner), rotation + ENDS.get(winner), argb(a, 0xFFFFFF));
		}
		fan(ctx, cx, cy, radius * 0.16f, 0f, 360f, argb(a, 0xD4AF37));
		fan(ctx, cx, cy, radius * 0.08f, 0f, 360f, argb(a, 0x202020));

		// Puntero (arriba).
		triangle(ctx, cx, cy - radius + 8f, cx - 9f, cy - radius - 16f, cx + 9f, cy - radius - 16f, argb(a, 0x000000));
		triangle(ctx, cx, cy - radius + 5f, cx - 7f, cy - radius - 14f, cx + 7f, cy - radius - 14f, argb(a, 0xFFFFFF));

		// Textos.
		ctx.drawCenteredTextWithShadow(font, Text.translatable("gui.dedsafio3.wheel_title"),
				(int) cx, (int) (cy - radius - 30f), argb(a, 0xFFFFFF));
		if (progress >= 1f) {
			int rgb = RouletteColor.byOrdinal(COLORS.get(winner)).rgb();
			ctx.drawCenteredTextWithShadow(font, colorText, (int) cx, (int) (cy + radius + 14f), argb(a, rgb));
			ctx.drawCenteredTextWithShadow(font, prizeText, (int) cx, (int) (cy + radius + 26f), argb(a, 0xFFD54F));
		}

		// Sonidos: "tic" al cruzar de sector y fanfarria al final.
		if (progress < 1f && current != lastSector) {
			lastSector = current;
			float pitch = 0.8f + 0.7f * eased;
			mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ITEM_PICKUP, pitch, 0.5f));
		}
		if (progress >= 1f && !finishedSoundPlayed) {
			finishedSoundPlayed = true;
			mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 0.7f));
		}
	}

	// ------------------------------------------------------------------ dibujo de formas

	/** Sector circular relleno. Los ángulos están en grados, en sentido horario desde arriba. */
	private static void fan(DrawContext ctx, float cx, float cy, float r, float a0, float a1, int argb) {
		if (a1 <= a0) {
			return;
		}
		ctx.draw();
		Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
		int steps = Math.max(1, (int) Math.ceil((a1 - a0) / 4f));
		BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
		put(buf, m, cx, cy, argb);
		for (int i = 0; i <= steps; i++) {
			float angle = a0 + (a1 - a0) * i / steps;
			put(buf, m, px(cx, r, angle), py(cy, r, angle), argb);
		}
		flush(buf);
	}

	/** Anillo (o arco de anillo) entre dos radios. */
	private static void ring(DrawContext ctx, float cx, float cy, float rIn, float rOut, float a0, float a1, int argb) {
		if (a1 <= a0) {
			return;
		}
		ctx.draw();
		Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
		int steps = Math.max(1, (int) Math.ceil((a1 - a0) / 4f));
		BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
		for (int i = 0; i <= steps; i++) {
			float angle = a0 + (a1 - a0) * i / steps;
			put(buf, m, px(cx, rOut, angle), py(cy, rOut, angle), argb);
			put(buf, m, px(cx, rIn, angle), py(cy, rIn, angle), argb);
		}
		flush(buf);
	}

	private static void triangle(DrawContext ctx, float x1, float y1, float x2, float y2, float x3, float y3, int argb) {
		ctx.draw();
		Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
		BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
		put(buf, m, x1, y1, argb);
		put(buf, m, x2, y2, argb);
		put(buf, m, x3, y3, argb);
		flush(buf);
	}

	private static float px(float cx, float r, float angleDeg) {
		return cx + (float) (Math.sin(Math.toRadians(angleDeg)) * r);
	}

	private static float py(float cy, float r, float angleDeg) {
		return cy - (float) (Math.cos(Math.toRadians(angleDeg)) * r);
	}

	private static void put(BufferBuilder buf, Matrix4f m, float x, float y, int argb) {
		buf.vertex(m, x, y, 0f).color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
	}

	private static void flush(BufferBuilder buf) {
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		BufferRenderer.drawWithGlobalProgram(buf.end());
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static int argb(int alpha, int rgb) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
	}

	private static int shade(int rgb, float f) {
		int r = (int) (((rgb >> 16) & 0xFF) * f);
		int g = (int) (((rgb >> 8) & 0xFF) * f);
		int b = (int) ((rgb & 0xFF) * f);
		return (r << 16) | (g << 8) | b;
	}
}
