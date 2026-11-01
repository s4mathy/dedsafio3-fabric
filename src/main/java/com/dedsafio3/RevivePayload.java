package com.dedsafio3;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Cliente -> servidor: el alma elegida en el Ritual (uuid del jugador). */
public record RevivePayload(String uuid) implements CustomPayload {
	public static final CustomPayload.Id<RevivePayload> ID = new CustomPayload.Id<>(Dedsafio3.id("revive"));

	public static final PacketCodec<RegistryByteBuf, RevivePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, RevivePayload::uuid,
			RevivePayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
