package com.dedsafio3.client;

import com.dedsafio3.ActionPayload;
import com.dedsafio3.DifficultyChange;
import com.dedsafio3.RouletteColor;
import com.dedsafio3.SettingPayload;
import com.dedsafio3.WorldModifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Menú interactivo (tecla K o /dedsafio menu). Tres pestañas:
 * General (dificultad, PvP, animación de muerte...), Ruletas (peso de cada color y giro de prueba)
 * y Mundo (modificaciones activas). Cada cambio se envía al servidor, que lo valida y lo aplica.
 */
public class DedsafioMenuScreen extends Screen {
	private enum Tab {
		GENERAL("general"), WHEELS("wheels"), WORLD("world"), CHANGES("changes");

		private final String id;

		Tab(String id) {
			this.id = id;
		}
	}

	private record Label(Text text, int x, int y, int color) {
	}

	private MenuState state;
	private static final int CHANGES_PER_PAGE = 6;

	private Tab tab = Tab.GENERAL;
	private int page;
	private final List<Label> labels = new ArrayList<>();

	public DedsafioMenuScreen(MenuState state) {
		super(Text.translatable("gui.dedsafio3.title"));
		this.state = state;
	}

	/** El servidor mandó un estado nuevo: se reconstruye la pantalla con los valores actuales. */
	public void update(MenuState newState) {
		this.state = newState;
		clearAndInit();
	}

	@Override
	public boolean shouldPause() {
		// Sin pausar: en un mundo local el servidor pausado no procesaría los cambios.
		return false;
	}

	@Override
	protected void init() {
		labels.clear();
		int cx = width / 2;

		// Pestañas.
		Tab[] tabs = Tab.values();
		int tabW = Math.min(100, (width - 24) / tabs.length);
		int tabX = cx - (tabW * tabs.length + 4 * (tabs.length - 1)) / 2;
		for (int i = 0; i < tabs.length; i++) {
			Tab t = tabs[i];
			ButtonWidget button = ButtonWidget.builder(Text.translatable("gui.dedsafio3.tab." + t.id), b -> {
				tab = t;
				clearAndInit();
			}).dimensions(tabX + i * (tabW + 4), 6, tabW, 20).build();
			button.active = tab != t;
			addDrawableChild(button);
		}

		switch (tab) {
			case GENERAL -> initGeneral(cx);
			case WHEELS -> initWheels(cx);
			case WORLD -> initWorld(cx);
			case CHANGES -> initChanges(cx);
		}

		addDrawableChild(ButtonWidget.builder(Text.translatable("gui.dedsafio3.close"), b -> close())
				.dimensions(cx - 60, height - 26, 120, 20).build());
	}

	// ------------------------------------------------------------------ pestañas

	private void initGeneral(int cx) {
		int w = 240;
		int x = cx - w / 2;

		labels.add(new Label(Text.translatable("gui.dedsafio3.difficulty"), cx, 34, 0xFFFFFF));
		int diffW = 58;
		int diffX = cx - (diffW * 4 + 6) / 2;
		for (int i = 0; i < 4; i++) {
			int value = i;
			ButtonWidget button = ButtonWidget.builder(Text.translatable("gui.dedsafio3.difficulty." + i),
					b -> setting("difficulty", value)).dimensions(diffX + i * (diffW + 2), 45, diffW, 20).build();
			button.active = state.difficulty() != i;
			addDrawableChild(button);
		}

		addToggle(x, 72, w, "gui.dedsafio3.pvp", "pvp", state.pvp());
		addToggle(x, 94, w, "gui.dedsafio3.keep_inventory", "keep_inventory", state.keepInventory());
		addToggle(x, 116, w, "gui.dedsafio3.death_animation", "death_animation", state.deathAnimation());
		addToggle(x, 138, w, "gui.dedsafio3.totems", "totems", state.totems());

		addDrawableChild(new IntSlider(x, 164, w, 20, Text.translatable("gui.dedsafio3.cooldown"),
				0, 30, state.cooldown(), v -> setting("cooldown", v)));
		addDrawableChild(new IntSlider(x, 186, w, 20, Text.translatable("gui.dedsafio3.spin"),
				3, 15, state.spin(), v -> setting("spin", v)));
	}

	private void initWheels(int cx) {
		labels.add(new Label(Text.translatable("gui.dedsafio3.wheels_hint"), cx, 30, 0xAAAAAA));
		RouletteColor[] colors = RouletteColor.values();
		for (int i = 0; i < colors.length; i++) {
			RouletteColor color = colors[i];
			int y = 42 + i * 22;
			int weight = i < state.weights().size() ? state.weights().get(i) : 10;
			addDrawableChild(new IntSlider(cx - 100, y, 150, 20, Text.translatable(color.nameKey()),
					0, 20, weight, v -> setting("weight." + color.id(), v)));
			addDrawableChild(ButtonWidget.builder(Text.translatable("gui.dedsafio3.spin_button"),
					b -> action("spin", color.id())).dimensions(cx + 54, y, 50, 20).build());
		}
		addDrawableChild(ButtonWidget.builder(Text.translatable("gui.dedsafio3.spin_random"),
				b -> action("spin", "random")).dimensions(cx - 100, 42 + colors.length * 22 + 4, 204, 20).build());
	}

	private void initWorld(int cx) {
		int w = 240;
		int x = cx - w / 2;
		labels.add(new Label(Text.translatable("gui.dedsafio3.modifiers"), cx, 34, 0xFFFFFF));
		WorldModifier[] modifiers = WorldModifier.values();
		for (int i = 0; i < modifiers.length; i++) {
			WorldModifier modifier = modifiers[i];
			boolean on = state.modifiers().contains(modifier.id());
			Text label = Text.translatable(modifier.translationKey()).append(": ")
					.append(Text.translatable(on ? "gui.dedsafio3.on" : "gui.dedsafio3.off"));
			addDrawableChild(ButtonWidget.builder(label, b -> setting("modifier." + modifier.id(), on ? 0 : 1))
					.dimensions(x, 46 + i * 24, w, 20).build());
		}
		int y = 46 + modifiers.length * 24 + 8;
		addDrawableChild(ButtonWidget.builder(Text.translatable("gui.dedsafio3.clear_modifiers"),
				b -> action("clear_modifiers", "")).dimensions(x, y, w, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.translatable("gui.dedsafio3.death_preview"),
				b -> action("death_preview", "")).dimensions(x, y + 24, w, 20).build());
	}

	/**
	 * Cambios de dificultad, mobs nuevos y Momento Reviil. El interruptor activa/desactiva el cambio (al activarlo,
	 * el servidor gira la ruleta naranja para todos); "Avisar" solo lo anuncia en el chat, sin ruleta.
	 */
	private void initChanges(int cx) {
		DifficultyChange[] all = DifficultyChange.values();
		int pages = (all.length + CHANGES_PER_PAGE - 1) / CHANGES_PER_PAGE;
		page = Math.max(0, Math.min(page, pages - 1));
		labels.add(new Label(Text.translatable("gui.dedsafio3.changes_hint"), cx, 30, 0xAAAAAA));

		int x = cx - 128;
		int rows = 0;
		boolean hasMoment = false;
		for (int i = 0; i < CHANGES_PER_PAGE; i++) {
			int index = page * CHANGES_PER_PAGE + i;
			if (index >= all.length) {
				break;
			}
			DifficultyChange change = all[index];
			boolean on = state.changes().contains(change.id());
			int y = 44 + i * 26;
			Text label = Text.translatable(change.nameKey()).append(": ")
					.append(Text.translatable(on ? "gui.dedsafio3.on" : "gui.dedsafio3.off"));
			addDrawableChild(ButtonWidget.builder(label, b -> setting("change." + change.id(), on ? 0 : 1))
					.dimensions(x, y, 190, 20)
					.tooltip(Tooltip.of(Text.translatable(change.descKey())))
					.build());
			addDrawableChild(ButtonWidget.builder(Text.translatable("gui.dedsafio3.notify"), b -> action("notify", change.id()))
					.dimensions(x + 194, y, 62, 20)
					.tooltip(Tooltip.of(Text.translatable("gui.dedsafio3.notify_tip")))
					.build());
			rows++;
			hasMoment |= change.kind() == DifficultyChange.Kind.MOMENT;
		}
		if (hasMoment) {
			addDrawableChild(new IntSlider(x, 44 + rows * 26 + 2, 256, 20, Text.translatable("gui.dedsafio3.reviil_seconds"),
					20, 180, state.reviilSeconds(), v -> setting("reviil_seconds", v)));
		}

		labels.add(new Label(Text.translatable("gui.dedsafio3.page", page + 1, pages), cx, height - 44, 0xAAAAAA));
		ButtonWidget prev = ButtonWidget.builder(Text.literal("<"), b -> {
			page--;
			clearAndInit();
		}).dimensions(cx - 128, height - 26, 60, 20).build();
		prev.active = page > 0;
		addDrawableChild(prev);
		ButtonWidget next = ButtonWidget.builder(Text.literal(">"), b -> {
			page++;
			clearAndInit();
		}).dimensions(cx + 68, height - 26, 60, 20).build();
		next.active = page < pages - 1;
		addDrawableChild(next);
	}

	// ------------------------------------------------------------------ utilidades

	private void addToggle(int x, int y, int w, String key, String setting, boolean on) {
		Text label = Text.translatable(key).append(": ")
				.append(Text.translatable(on ? "gui.dedsafio3.on" : "gui.dedsafio3.off"));
		addDrawableChild(ButtonWidget.builder(label, b -> setting(setting, on ? 0 : 1))
				.dimensions(x, y, w, 20).build());
	}

	private void setting(String key, int value) {
		ClientPlayNetworking.send(new SettingPayload(key, value));
	}

	private void action(String action, String arg) {
		ClientPlayNetworking.send(new ActionPayload(action, arg));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		for (Label label : labels) {
			context.drawCenteredTextWithShadow(textRenderer, label.text(), label.x(), label.y(), label.color());
		}
		if (tab == Tab.WHEELS) {
			// Muestra del color junto a cada deslizador.
			RouletteColor[] colors = RouletteColor.values();
			for (int i = 0; i < colors.length; i++) {
				int y = 42 + i * 22;
				int x = width / 2 - 100 - 18;
				context.fill(x - 1, y + 2, x + 15, y + 18, 0xFF000000);
				context.fill(x, y + 3, x + 14, y + 17, 0xFF000000 | colors[i].rgb());
			}
		}
	}

	/** Deslizador de enteros: envía el valor al soltarlo (no en cada movimiento). */
	private static final class IntSlider extends SliderWidget {
		private final int min;
		private final int max;
		private final Text label;
		private final Consumer<Integer> onCommit;

		IntSlider(int x, int y, int width, int height, Text label, int min, int max, int value, Consumer<Integer> onCommit) {
			super(x, y, width, height, Text.empty(), (value - min) / (double) (max - min));
			this.min = min;
			this.max = max;
			this.label = label;
			this.onCommit = onCommit;
			updateMessage();
		}

		private int current() {
			return min + (int) Math.round(value * (max - min));
		}

		@Override
		protected void updateMessage() {
			setMessage(label.copy().append(": " + current()));
		}

		@Override
		protected void applyValue() {
			// El valor se envía en onRelease.
		}

		@Override
		public void onRelease(double mouseX, double mouseY) {
			super.onRelease(mouseX, mouseY);
			onCommit.accept(current());
		}
	}
}
