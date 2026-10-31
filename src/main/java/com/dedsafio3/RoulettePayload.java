package com.dedsafio3;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

/**
 * Servidor -> cliente: inicia la animación de la rueda circular.
 *
 * @param colors    ordinales de {@link RouletteColor} que aparecen en la rueda (en orden)
 * @param weights   peso de cada sector (el ángulo de cada color es proporcional a su peso)
 * @param winner    índice (dentro de {@code colors}) del color ganador
 * @param frac      posición (0..1) dentro del sector ganador donde se detiene el puntero
 * @param spinTicks duración del giro en ticks
 * @param prizeKey  clave de traducción del premio que se mostrará al terminar
 */
public record RoulettePayload(List<Integer> colors, List<Integer> weights, int winner, float frac,
							  int spinTicks, String prizeKey) implements CustomPayload {
	public static final CustomPayload.Id<RoulettePayload> ID = new CustomPayload.Id<>(Dedsafio3.id("roulette"));

	public static final PacketCodec<RegistryByteBuf, RoulettePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER.collect(PacketCodecs.toList()), RoulettePayload::colors,
			PacketCodecs.INTEGER.collect(PacketCodecs.toList()), RoulettePayload::weights,
			PacketCodecs.INTEGER, RoulettePayload::winner,
			PacketCodecs.FLOAT, RoulettePayload::frac,
			PacketCodecs.INTEGER, RoulettePayload::spinTicks,
			PacketCodecs.STRING, RoulettePayload::prizeKey,
			RoulettePayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
