package com.dedsafio3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/** Lado servidor de la animación de muerte: efectos en el mundo + aviso a los clientes. */
public final class DeathAnimation {
	private DeathAnimation() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayerEntity dead) {
				ServerWorld world = dead.getServerWorld();
				worldEffects(world, dead);
				broadcast(dead);
			}
		});
	}

	/** Partículas y sonido en el lugar donde murió el jugador. */
	public static void worldEffects(ServerWorld world, ServerPlayerEntity player) {
		world.spawnParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0, player.getZ(),
				40, 0.4, 0.8, 0.4, 0.05);
		world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.2, player.getZ(),
				20, 0.3, 0.1, 0.3, 0.02);
		world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITHER_DEATH,
				SoundCategory.PLAYERS, 0.6f, 0.8f);
	}

	/** Envía la animación a todos los jugadores del servidor (a quien murió se le muestra a pantalla completa). */
	public static void broadcast(ServerPlayerEntity dead) {
		MinecraftServer server = dead.getServer();
		if (server == null) {
			return;
		}
		String name = dead.getName().getString();
		for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
			ServerPlayNetworking.send(other, new DeathAnimPayload(name, other == dead));
		}
	}
}
