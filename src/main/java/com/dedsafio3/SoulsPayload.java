package com.dedsafio3;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Servidor -> cliente: uuids (separados por coma) de los jugadores que conservan su alma. Vacío = ninguno. */
public record SoulsPayload(String holders) implements CustomPayload {
	public static final CustomPayload.Id<SoulsPayload> ID = new CustomPayload.Id<>(Dedsafio3.id("souls"));

	public static final PacketCodec<RegistryByteBuf, SoulsPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, SoulsPayload::holders,
			SoulsPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
