package com.dedsafio3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Activa y desactiva los cambios de la serie y los anuncia (ruleta naranja o solo chat). */
public final class Changes {
	private record Task(long at, Runnable run) {
	}

	private static final List<Task> TASKS = new ArrayList<>();
	/** Cambios cuya ruleta de anuncio está girando (aún no se han activado). */
	private static final Set<ChangeType> PENDING = EnumSet.noneOf(ChangeType.class);

	private Changes() {
	}

	/**
	 * Activa o desactiva un cambio. Al activar, según el ajuste "aviso" del menú, muestra la ruleta naranja a todos
	 * y activa el cambio al terminar, o lo activa al instante y avisa solo por chat.
	 */
	public static void set(MinecraftServer server, ChangeType type, boolean on) {
		if (!on) {
			PENDING.remove(type);
			if (type.isActive()) {
				setActive(server, type, false);
				server.getPlayerManager().broadcast(
						Text.translatable("message.dedsafio3.change_off", Text.translatable(type.nameKey())), false);
			}
			return;
		}
		if (type.isActive() || PENDING.contains(type)) {
			return;
		}
		if (DedsafioConfig.get().wheelAnnounce) {
			int delay = Roulette.announce(server, type.wheelKey()) + 15;
			PENDING.add(type);
			schedule(server, delay, () -> {
				if (PENDING.remove(type)) {
					setActive(server, type, true);
					notifyChat(server, type);
				}
			});
		} else {
			setActive(server, type, true);
			notifyChat(server, type);
		}
	}

	/** Cambia el estado sin avisar (el aviso lo hace quien llama). */
	static void setActive(MinecraftServer server, ChangeType type, boolean on) {
		if (type == ChangeType.REVIIL_MOMENT) {
			if (on) {
				ReviilEvent.start(server);
			} else {
				ReviilEvent.cancel(server);
			}
		} else {
			List<String> list = DedsafioConfig.get().changes;
			list.remove(type.id());
			if (on) {
				list.add(type.id());
			}
			DedsafioConfig.save();
		}
		MenuService.refreshAll(server);
	}

	/** Anuncio en el chat (sin ruleta): encabezado de color + descripción. */
	public static void notifyChat(MinecraftServer server, ChangeType type) {
		MutableText message = Text.translatable(type.titleKey()).formatted(type.kind().color(), Formatting.BOLD)
				.append(Text.literal("\n"))
				.append(Text.translatable(type.descKey()).formatted(Formatting.WHITE));
		server.getPlayerManager().broadcast(message, false);
	}

	private static void schedule(MinecraftServer server, int delayTicks, Runnable run) {
		TASKS.add(new Task(server.getTicks() + delayTicks, run));
	}

	public static void register() {
		ChangeSpawner.init();
		ChangeEffects.register();

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!TASKS.isEmpty()) {
				long now = server.getTicks();
				Iterator<Task> it = TASKS.iterator();
				List<Runnable> due = new ArrayList<>();
				while (it.hasNext()) {
					Task task = it.next();
					if (task.at() <= now) {
						due.add(task.run());
						it.remove();
					}
				}
				due.forEach(Runnable::run);
			}
			ChangeEffects.tick(server);
			ChangeSpawner.tick(server);
			ReviilEvent.tick(server);
		});

		// En mundos locales el estado estático sobreviviría al cerrar el mundo: se limpia al parar.
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			TASKS.clear();
			PENDING.clear();
			ReviilEvent.reset();
			ChangeEffects.reset();
		});
	}
}
