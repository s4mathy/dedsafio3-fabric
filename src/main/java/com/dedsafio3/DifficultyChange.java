package com.dedsafio3;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Cambios de dificultad, nuevos mobs y el Momento Reviil de la serie. Cada uno se activa o desactiva desde el menú (K).
 * Al activarlo sale la ruleta con su nombre (naranja; roja para los Momentos Reviil); el botón "Avisar" solo lo anuncia en el chat.
 */
public enum DifficultyChange {
	BLAZES("blazes", Kind.DIFFICULTY),
	CREEPER_NUCLEAR("creeper_nuclear", Kind.MOB),
	REPTISAURIO_SALVAJE("reptisaurio_salvaje", Kind.MOB),
	PLUMOSAURIO("plumosaurio", Kind.MOB),
	DROMORAPTOR_ROJO("dromoraptor_rojo", Kind.MOB),
	REPTISAURIO_GUERRERO("reptisaurio_guerrero", Kind.MOB),
	REPTISAURIO_ARQUERO("reptisaurio_arquero", Kind.MOB),
	REPTISAURIO_LANZA("reptisaurio_lanza", Kind.MOB),
	NO_HONEY("no_honey", Kind.DIFFICULTY),
	MAGMA_DEADLY("magma_deadly", Kind.DIFFICULTY),
	ZOMBIE_CLIMB("zombie_climb", Kind.DIFFICULTY),
	REVIIL("reviil", Kind.MOMENT),
	// --- v1.2.0
	DACTILO_BEBE("dactilo_bebe", Kind.MOB),
	CREEPER_MAGMA("creeper_magma", Kind.MOB),
	DROMORAPTOR_AZUL("dromoraptor_azul", Kind.MOB),
	ZOMBIE_ELITE("zombie_elite", Kind.DIFFICULTY),
	HEROBRINE("herobrine", Kind.DIFFICULTY),
	NARVAL("narval", Kind.DIFFICULTY),
	COBWEB_SPIDER("cobweb_spider", Kind.DIFFICULTY),
	REVIIL_LUNA("reviil_luna", Kind.MOMENT),
	REVIIL_METEOROS("reviil_meteoros", Kind.MOMENT);

	public enum Kind {
		DIFFICULTY, MOB, MOMENT
	}

	/** Cambios cuya ruleta ya está girando (evita activarlos dos veces). */
	private static final Set<DifficultyChange> WAITING = EnumSet.noneOf(DifficultyChange.class);

	private final String id;
	private final Kind kind;

	DifficultyChange(String id, Kind kind) {
		this.id = id;
		this.kind = kind;
	}

	public String id() {
		return id;
	}

	public Kind kind() {
		return kind;
	}

	public String titleKey() {
		return "change." + Dedsafio3.MOD_ID + ".title." + kind.name().toLowerCase();
	}

	public String nameKey() {
		return "change." + Dedsafio3.MOD_ID + "." + id + ".name";
	}

	public String descKey() {
		return "change." + Dedsafio3.MOD_ID + "." + id + ".desc";
	}

	/** Texto que muestra la ruleta al terminar de girar. */
	public String wheelKey() {
		return "change." + Dedsafio3.MOD_ID + "." + id + ".wheel";
	}

	public static DifficultyChange byId(String id) {
		for (DifficultyChange change : values()) {
			if (change.id.equals(id)) {
				return change;
			}
		}
		return null;
	}

	public boolean isActive() {
		if (this == REVIIL || this == REVIIL_METEOROS) {
			return ReviilEvent.isRunning(this == REVIIL_METEOROS ? ReviilEvent.Type.METEORS : ReviilEvent.Type.BEAM);
		}
		return DedsafioConfig.get().changes.contains(id);
	}

	/** Activa o desactiva sin ruleta ni aviso. {@code source} solo lo usa el Momento Reviil (para ubicar el búnker). */
	public void setActive(MinecraftServer server, boolean on, ServerPlayerEntity source) {
		if (this == REVIIL || this == REVIIL_METEOROS) {
			if (on) {
				ReviilEvent.start(server, source, this == REVIIL_METEOROS ? ReviilEvent.Type.METEORS : ReviilEvent.Type.BEAM);
			} else {
				ReviilEvent.stop(server);
			}
			return;
		}
		List<String> list = DedsafioConfig.get().changes;
		list.remove(id);
		if (on) {
			list.add(id);
		}
		DedsafioConfig.save();
	}

	/** Gira la ruleta naranja para todos; al terminar activa el cambio y lo anuncia en el chat. */
	public void activateWithRoulette(MinecraftServer server, ServerPlayerEntity source) {
		if (isActive() || WAITING.contains(this)) {
			return;
		}
		// Solo puede haber un Momento Reviil con cuenta atrás a la vez.
		if ((this == REVIIL || this == REVIIL_METEOROS) && ReviilEvent.isRunning()) {
			return;
		}
		WAITING.add(this);
		RouletteColor wheelColor = kind == Kind.MOMENT ? RouletteColor.RED : RouletteColor.ORANGE;
		Roulette.announce(server, wheelColor, wheelKey(), () -> {
			WAITING.remove(this);
			setActive(server, true, source);
			broadcastNotice(server);
			MenuService.refreshAll(server);
		});
	}

	public void deactivate(MinecraftServer server) {
		if (!isActive()) {
			return;
		}
		setActive(server, false, null);
		server.getPlayerManager().broadcast(Text.translatable("message.dedsafio3.change_off", Text.translatable(nameKey())), false);
		MenuService.refreshAll(server);
	}

	/** Anuncia el cambio en el chat, sin ruleta. */
	public void broadcastNotice(MinecraftServer server) {
		server.getPlayerManager().broadcast(Text.translatable("message.dedsafio3.change_notice",
				Text.translatable(titleKey()), Text.translatable(nameKey()), Text.translatable(descKey())), false);
	}

	public static void resetRuntime() {
		WAITING.clear();
	}
}
