package com.dedsafio3;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

/**
 * Servidor -> cliente: inicia la animación de la ruleta.
 *
 * @param color    ordinal de {@link RouletteColor}
 * @param keys     claves de traducción de cada casilla que se desplaza
 * @param winner   índice de la casilla ganadora dentro de {@code keys}
 * @param spinTicks duración del giro en ticks
 */
public record RoulettePayload(int color, List<String> keys, int winner, int spinTicks) implements CustomPayload {
	public static final CustomPayload.Id<RoulettePayload> ID = new CustomPayload.Id<>(Dedsafio3.id("roulette"));

	public static final PacketCodec<RegistryByteBuf, RoulettePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER, RoulettePayload::color,
			PacketCodecs.STRING.collect(PacketCodecs.toList()), RoulettePayload::keys,
			PacketCodecs.INTEGER, RoulettePayload::winner,
			PacketCodecs.INTEGER, RoulettePayload::spinTicks,
			RoulettePayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
