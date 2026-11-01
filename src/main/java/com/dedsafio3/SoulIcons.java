package com.dedsafio3;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Set;
import java.util.UUID;

/** Jugadores que conservan su alma: llevan un icono de alma junto a su nombre (nametag y chat). */
public final class SoulIcons {
	public static final Identifier FONT = Dedsafio3.id("icons");
	private static volatile Set<UUID> holders = Set.of();

	private SoulIcons() {
	}

	public static void set(Set<UUID> players) {
		holders = Set.copyOf(players);
	}

	public static boolean has(UUID id) {
		return holders.contains(id);
	}

	/** Un carácter de la fuente "icons" (assets/dedsafio3/font/icons.json) que dibuja el icono de alma. */
	public static Text icon() {
		return Text.literal("").setStyle(Style.EMPTY.withFont(FONT));
	}
}
