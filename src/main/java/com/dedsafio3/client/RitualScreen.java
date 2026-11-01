package com.dedsafio3.client;

import com.dedsafio3.Dedsafio3;
import com.dedsafio3.RevivePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Menú del Ritual de Resurrección: lista de almas (jugadores muertos) con un icono de alma junto a cada nombre. */
public class RitualScreen extends Screen {
	private static final Identifier SOUL = Dedsafio3.id("textures/gui/soul.png");
	private static final int PER_PAGE = 7;

	private final List<String[]> souls = new ArrayList<>(); // {uuid, nombre}
	private int page;

	public RitualScreen(String data) {
		super(Text.translatable("gui.dedsafio3.ritual.title"));
		for (String line : data.split("\n")) {
			int bar = line.indexOf('|');
			if (bar > 0) {
				souls.add(new String[]{line.substring(0, bar), line.substring(bar + 1)});
			}
		}
	}

	@Override
	protected void init() {
		int pages = Math.max(1, (souls.size() + PER_PAGE - 1) / PER_PAGE);
		page = Math.max(0, Math.min(page, pages - 1));
		int x = width / 2 - 80;
		int y = 52;
		for (int i = page * PER_PAGE; i < Math.min(souls.size(), (page + 1) * PER_PAGE); i++) {
			String[] soul = souls.get(i);
			addDrawableChild(ButtonWidget.builder(Text.literal(soul[1]), b -> {
				ClientPlayNetworking.send(new RevivePayload(soul[0]));
				close();
			}).dimensions(x, y, 160, 20).build());
			y += 24;
		}
		if (pages > 1) {
			int by = 52 + PER_PAGE * 24 + 4;
			addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
				page--;
				clearAndInit();
			}).dimensions(x, by, 20, 20).build()).active = page > 0;
			addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
				page++;
				clearAndInit();
			}).dimensions(x + 140, by, 20, 20).build()).active = page < pages - 1;
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
		context.drawCenteredTextWithShadow(textRenderer, Text.translatable("gui.dedsafio3.ritual.hint"), width / 2, 34, 0xAAAAAA);
		// Icono de alma al lado del nombre de cada jugador.
		int y = 52;
		for (int i = page * PER_PAGE; i < Math.min(souls.size(), (page + 1) * PER_PAGE); i++) {
			context.drawTexture(SOUL, width / 2 - 80 - 22, y + 2, 0, 0, 16, 16, 16, 16);
			y += 24;
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
