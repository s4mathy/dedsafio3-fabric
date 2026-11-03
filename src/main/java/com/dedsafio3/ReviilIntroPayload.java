package com.dedsafio3;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Servidor -> cliente: animación de la calavera con el texto "MOMENTO REVIIL" antes de que empiece el momento. */
public record ReviilIntroPayload(int ticks) implements CustomPayload {
	public static final CustomPayload.Id<ReviilIntroPayload> ID = new CustomPayload.Id<>(Dedsafio3.id("reviil_intro"));

	public static final PacketCodec<RegistryByteBuf, ReviilIntroPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER, ReviilIntroPayload::ticks, ReviilIntroPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
