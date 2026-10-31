package com.dedsafio3;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Servidor -> cliente: estado actual del menú (como JSON).
 *
 * @param open {@code true} para abrir la pantalla; {@code false} solo refresca si ya está abierta
 */
public record ConfigPayload(boolean open, String json) implements CustomPayload {
	public static final CustomPayload.Id<ConfigPayload> ID = new CustomPayload.Id<>(Dedsafio3.id("config"));

	public static final PacketCodec<RegistryByteBuf, ConfigPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL, ConfigPayload::open,
			PacketCodecs.STRING, ConfigPayload::json,
			ConfigPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
