package com.dedsafio3;

import net.minecraft.util.Formatting;

/**
 * Cambios de la serie que se pueden activar desde el menú (tecla K): cambios de dificultad, mobs nuevos
 * y el Momento Reviil. Al activarlos se anuncian con la ruleta naranja (o solo por chat, según el menú).
 */
public enum ChangeType {
	BLAZES_NETHER("blazes_nether", Kind.DIFFICULTY),
	NUCLEAR_CREEPER("nuclear_creeper", Kind.MOB),
	REPTISAURIO_SALVAJE("reptisaurio_salvaje", Kind.MOB),
	PLUMOSAURIO("plumosaurio", Kind.MOB),
	DROMORAPTOR_ROJO("dromoraptor_rojo", Kind.MOB),
	REPTISAURIO_GUERRERO("reptisaurio_guerrero", Kind.MOB),
	REPTISAURIO_ARQUERO("reptisaurio_arquero", Kind.MOB),
	REPTISAURIO_LANZA("reptisaurio_lanza", Kind.MOB),
	NO_HONEY("no_honey", Kind.DIFFICULTY),
	MAGMA_DEADLY("magma_deadly", Kind.DIFFICULTY),
	ZOMBIE_CLIMB("zombie_climb", Kind.DIFFICULTY),
	REVIIL_MOMENT("reviil_moment", Kind.EVENT);

	public enum Kind {
		DIFFICULTY(Formatting.GOLD),
		MOB(Formatting.YELLOW),
		EVENT(Formatting.RED);

		private final Formatting color;

		Kind(Formatting color) {
			this.color = color;
		}

		public Formatting color() {
			return color;
		}
	}

	private final String id;
	private final Kind kind;

	ChangeType(String id, Kind kind) {
		this.id = id;
		this.kind = kind;
	}

	public String id() {
		return id;
	}

	public Kind kind() {
		return kind;
	}

	private String key(String suffix) {
		return "change." + Dedsafio3.MOD_ID + "." + id + "." + suffix;
	}

	/** Nombre corto (texto del botón del menú). */
	public String nameKey() {
		return key("name");
	}

	/** Encabezado del aviso: "Cambio de Dificultad", "Nuevo Mob - X" o "Momento Reviil". */
	public String titleKey() {
		return key("title");
	}

	/** Descripción del aviso. */
	public String descKey() {
		return key("desc");
	}

	/** Texto que muestra la rueda naranja al caer. */
	public String wheelKey() {
		return key("wheel");
	}

	public boolean isActive() {
		if (this == REVIIL_MOMENT) {
			return ReviilEvent.isRunning();
		}
		return DedsafioConfig.get().changes.contains(id);
	}

	public static ChangeType byId(String id) {
		for (ChangeType type : values()) {
			if (type.id.equals(id)) {
				return type;
			}
		}
		return null;
	}
}
