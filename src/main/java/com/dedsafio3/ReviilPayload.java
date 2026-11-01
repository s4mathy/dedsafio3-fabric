package com.dedsafio3;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Servidor -> cliente: durante el Momento Reviil se oculta la vida del jugador. {@code seconds} = 0 lo cancela. */
public record ReviilPayload(int seconds) implements CustomPayload {
	public static final CustomPayload.Id<ReviilPayload> ID = new CustomPayload.Id<>(Dedsafio3.id("reviil"));

	public static final PacketCodec<RegistryByteBuf, ReviilPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER, ReviilPayload::seconds,
			ReviilPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
