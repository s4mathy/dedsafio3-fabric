package com.dedsafio3;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Servidor -> cliente: abre el menú del Ritual con las almas disponibles. Una por línea: "uuid|nombre". */
public record RitualPayload(String souls) implements CustomPayload {
	public static final CustomPayload.Id<RitualPayload> ID = new CustomPayload.Id<>(Dedsafio3.id("ritual"));

	public static final PacketCodec<RegistryByteBuf, RitualPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, RitualPayload::souls,
			RitualPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
