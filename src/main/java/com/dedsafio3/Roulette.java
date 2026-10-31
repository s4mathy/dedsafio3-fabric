package com.dedsafio3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Lógica de las ruletas: elige el premio en el servidor, manda la animación al cliente
 * y aplica el premio cuando termina el giro.
 *
 * Para añadir un premio: agrega una línea {@code add(COLOR, "id", peso, (p, w) -> ...)} abajo
 * y su traducción {@code roulette.dedsafio3.<color>.<id>} en los archivos de idioma.
 */
public final class Roulette {
	/** Duración del giro en ticks (5 s). */
	public static final int SPIN_TICKS = 100;
	/** Margen extra antes de aplicar el premio (la animación mantiene el resultado en pantalla). */
	private static final int APPLY_DELAY_TICKS = 15;
	/** Casillas que se desplazan en la animación. */
	private static final int CELLS = 40;
	/** Posición de la casilla ganadora dentro de la tira. */
	private static final int WINNER_INDEX = 32;
	/** Espera entre giros del mismo jugador (ticks). */
	private static final int COOLDOWN_TICKS = 40;

	public record Prize(String id, int weight, BiConsumer<ServerPlayerEntity, ServerWorld> action) {
	}

	private record Pending(long applyAt, RouletteColor color, Prize prize) {
	}

	private static final Map<RouletteColor, List<Prize>> PRIZES = new EnumMap<>(RouletteColor.class);
	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	private static final Map<UUID, Long> COOLDOWN = new HashMap<>();

	static {
		defineRed();
		defineOrange();
		defineYellow();
		defineGreen();
		defineBlue();
		definePurple();
	}

	private Roulette() {
	}

	// ------------------------------------------------------------------ premios

	/** ROJA: caos y castigos. */
	private static void defineRed() {
		add(RouletteColor.RED, "explosion", 10, (p, w) ->
				w.createExplosion(null, p.getX(), p.getY(), p.getZ(), 2.5f, World.ExplosionSourceType.NONE));
		add(RouletteColor.RED, "lightning", 10, (p, w) -> {
			LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
			if (bolt != null) {
				bolt.refreshPositionAfterTeleport(p.getX(), p.getY(), p.getZ());
				w.spawnEntity(bolt);
			}
		});
		add(RouletteColor.RED, "zombies", 12, (p, w) -> spawnAround(p, w, EntityType.ZOMBIE, 4));
		add(RouletteColor.RED, "hunger", 14, (p, w) -> {
			effect(p, StatusEffects.HUNGER, 30, 2);
			effect(p, StatusEffects.NAUSEA, 10, 0);
		});
		add(RouletteColor.RED, "wither", 10, (p, w) -> effect(p, StatusEffects.WITHER, 8, 0));
		add(RouletteColor.RED, "weakness", 14, (p, w) -> {
			effect(p, StatusEffects.WEAKNESS, 45, 1);
			effect(p, StatusEffects.MINING_FATIGUE, 45, 1);
		});
	}

	/** NARANJA: fuego, lava y magma. */
	private static void defineOrange() {
		add(RouletteColor.ORANGE, "ignite", 14, (p, w) -> p.setOnFireFor(10));
		add(RouletteColor.ORANGE, "magma_floor", 12, (p, w) -> {
			BlockPos below = p.getBlockPos().down();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos pos = below.add(dx, 0, dz);
					BlockState state = w.getBlockState(pos);
					if (!state.isAir() && state.getHardness(w, pos) >= 0) {
						w.setBlockState(pos, Blocks.MAGMA_BLOCK.getDefaultState());
					}
				}
			}
		});
		add(RouletteColor.ORANGE, "blazes", 8, (p, w) -> spawnAround(p, w, EntityType.BLAZE, 2));
		add(RouletteColor.ORANGE, "fire_resistance", 16, (p, w) -> effect(p, StatusEffects.FIRE_RESISTANCE, 120, 0));
		add(RouletteColor.ORANGE, "strength", 14, (p, w) -> effect(p, StatusEffects.STRENGTH, 60, 1));
		add(RouletteColor.ORANGE, "steak", 16, (p, w) -> give(p, new ItemStack(Items.COOKED_BEEF, 8)));
	}

	/** AMARILLA: dinero y botín. */
	private static void defineYellow() {
		add(RouletteColor.YELLOW, "deditas", 22, (p, w) -> give(p, new ItemStack(ModItems.DEDITA, 3)));
		add(RouletteColor.YELLOW, "silver_dedita", 10, (p, w) -> give(p, new ItemStack(ModItems.DEDITA_PLATA, 1)));
		add(RouletteColor.YELLOW, "gold_dedita", 3, (p, w) -> give(p, new ItemStack(ModItems.DEDITA_ORO, 1)));
		add(RouletteColor.YELLOW, "gold", 16, (p, w) -> give(p, new ItemStack(Items.GOLD_INGOT, 8)));
		add(RouletteColor.YELLOW, "golden_apples", 10, (p, w) -> give(p, new ItemStack(Items.GOLDEN_APPLE, 3)));
		add(RouletteColor.YELLOW, "experience", 18, (p, w) -> p.addExperienceLevels(5));
		add(RouletteColor.YELLOW, "luck", 10, (p, w) -> effect(p, StatusEffects.LUCK, 300, 1));
	}

	/** VERDE: naturaleza y mejoras. */
	private static void defineGreen() {
		add(RouletteColor.GREEN, "regeneration", 16, (p, w) -> effect(p, StatusEffects.REGENERATION, 30, 1));
		add(RouletteColor.GREEN, "speed", 16, (p, w) -> {
			effect(p, StatusEffects.SPEED, 60, 1);
			effect(p, StatusEffects.JUMP_BOOST, 60, 1);
		});
		add(RouletteColor.GREEN, "heal", 14, (p, w) -> {
			p.heal(20.0f);
			p.getHungerManager().add(20, 1.0f);
		});
		add(RouletteColor.GREEN, "feast", 14, (p, w) -> give(p, new ItemStack(Items.BREAD, 16)));
		add(RouletteColor.GREEN, "saturation", 12, (p, w) -> effect(p, StatusEffects.SATURATION, 20, 0));
		add(RouletteColor.GREEN, "bees", 6, (p, w) -> spawnAround(p, w, EntityType.BEE, 3));
	}

	/** AZUL: agua, hielo y ayuda. */
	private static void defineBlue() {
		add(RouletteColor.BLUE, "water_breathing", 16, (p, w) -> {
			effect(p, StatusEffects.WATER_BREATHING, 300, 0);
			effect(p, StatusEffects.DOLPHINS_GRACE, 120, 0);
		});
		add(RouletteColor.BLUE, "freeze", 12, (p, w) -> {
			p.setFrozenTicks(p.getMinFreezeDamageTicks() + 100);
			effect(p, StatusEffects.SLOWNESS, 10, 2);
		});
		add(RouletteColor.BLUE, "absorption", 16, (p, w) -> effect(p, StatusEffects.ABSORPTION, 120, 3));
		add(RouletteColor.BLUE, "levitation", 12, (p, w) -> effect(p, StatusEffects.LEVITATION, 5, 0));
		add(RouletteColor.BLUE, "rain", 10, (p, w) -> w.setWeather(0, 6000, true, true));
		add(RouletteColor.BLUE, "fish", 14, (p, w) -> give(p, new ItemStack(Items.COOKED_SALMON, 8)));
	}

	/** MORADA: magia y azar. */
	private static void definePurple() {
		add(RouletteColor.PURPLE, "teleport", 12, (p, w) -> {
			if (w.getDimension().hasCeiling()) {
				effect(p, StatusEffects.LEVITATION, 3, 0);
				return;
			}
			Random random = p.getRandom();
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = 20.0 + random.nextDouble() * 40.0;
			int x = (int) Math.floor(p.getX() + Math.cos(angle) * distance);
			int z = (int) Math.floor(p.getZ() + Math.sin(angle) * distance);
			int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
			p.requestTeleport(x + 0.5, y, z + 0.5);
		});
		add(RouletteColor.PURPLE, "totem", 8, (p, w) -> {
			Item totem = ModItems.TOTEMS.get(p.getRandom().nextInt(ModItems.TOTEMS.size()));
			give(p, new ItemStack(totem));
		});
		add(RouletteColor.PURPLE, "chaos_sword", 3, (p, w) -> give(p, new ItemStack(ModItems.ESPADA_CAOTICA)));
		add(RouletteColor.PURPLE, "invisibility", 14, (p, w) -> {
			effect(p, StatusEffects.INVISIBILITY, 60, 0);
			effect(p, StatusEffects.GLOWING, 30, 0);
		});
		add(RouletteColor.PURPLE, "night", 14, (p, w) -> w.setTimeOfDay(18000L));
		add(RouletteColor.PURPLE, "token", 12, (p, w) -> give(p, new ItemStack(ModItems.FICHA_RULETA, 2)));
	}

	// ------------------------------------------------------------------ helpers de premios

	private static void add(RouletteColor color, String id, int weight, BiConsumer<ServerPlayerEntity, ServerWorld> action) {
		PRIZES.computeIfAbsent(color, c -> new ArrayList<>()).add(new Prize(id, weight, action));
	}

	private static void effect(ServerPlayerEntity player, RegistryEntry<StatusEffect> effect, int seconds, int amplifier) {
		player.addStatusEffect(new StatusEffectInstance(effect, seconds * 20, amplifier));
	}

	private static void give(ServerPlayerEntity player, ItemStack stack) {
		player.getInventory().offerOrDrop(stack);
	}

	private static <T extends MobEntity> void spawnAround(ServerPlayerEntity player, ServerWorld world, EntityType<T> type, int count) {
		for (int i = 0; i < count; i++) {
			int dx = player.getRandom().nextBetween(-3, 3);
			int dz = player.getRandom().nextBetween(-3, 3);
			type.spawn(world, player.getBlockPos().add(dx, 0, dz), SpawnReason.COMMAND);
		}
	}

	private static String key(RouletteColor color, Prize prize) {
		return "roulette." + Dedsafio3.MOD_ID + "." + color.id() + "." + prize.id();
	}

	/** Todos los premios de una ruleta (útil para listados o pruebas). */
	public static List<Prize> prizes(RouletteColor color) {
		return PRIZES.getOrDefault(color, List.of());
	}

	// ------------------------------------------------------------------ giro

	private static Prize pick(List<Prize> pool, Random random) {
		int total = 0;
		for (Prize prize : pool) {
			total += prize.weight();
		}
		int roll = random.nextInt(total);
		for (Prize prize : pool) {
			roll -= prize.weight();
			if (roll < 0) {
				return prize;
			}
		}
		return pool.get(pool.size() - 1);
	}

	/**
	 * Hace girar una ruleta para el jugador. El premio se decide ya, pero se aplica al terminar la animación.
	 *
	 * @return {@code false} si el jugador ya tiene una ruleta en curso o está en espera
	 */
	public static boolean spin(ServerPlayerEntity player, RouletteColor color) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return false;
		}
		long now = server.getTicks();
		UUID id = player.getUuid();
		if (PENDING.containsKey(id) || COOLDOWN.getOrDefault(id, 0L) > now) {
			player.sendMessage(Text.translatable("message.dedsafio3.busy"), true);
			return false;
		}

		List<Prize> pool = PRIZES.get(color);
		Random random = player.getRandom();
		Prize winner = pick(pool, random);

		List<String> keys = new ArrayList<>(CELLS);
		for (int i = 0; i < CELLS; i++) {
			Prize shown = i == WINNER_INDEX ? winner : pool.get(random.nextInt(pool.size()));
			keys.add(key(color, shown));
		}

		ServerPlayNetworking.send(player, new RoulettePayload(color.ordinal(), keys, WINNER_INDEX, SPIN_TICKS));
		PENDING.put(id, new Pending(now + SPIN_TICKS + APPLY_DELAY_TICKS, color, winner));
		return true;
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (PENDING.isEmpty()) {
				return;
			}
			long now = server.getTicks();
			Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, Pending> entry = it.next();
				Pending pending = entry.getValue();
				if (pending.applyAt() > now) {
					continue;
				}
				it.remove();
				ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
				if (player == null || !player.isAlive()) {
					continue;
				}
				COOLDOWN.put(entry.getKey(), now + COOLDOWN_TICKS);
				try {
					pending.prize().action().accept(player, player.getServerWorld());
				} catch (Exception e) {
					Dedsafio3.LOGGER.error("Error aplicando el premio {}", pending.prize().id(), e);
				}
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.getPlayer().getUuid();
			PENDING.remove(id);
			COOLDOWN.remove(id);
		});
	}
}
