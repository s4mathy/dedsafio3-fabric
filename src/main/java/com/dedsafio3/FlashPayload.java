package com.dedsafio3;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Servidor -> cliente: cubre la pantalla de blanco (Flashbang) durante {@code ticks} ticks. */
public record FlashPayload(int ticks) implements CustomPayload {
	public static final CustomPayload.Id<FlashPayload> ID = new CustomPayload.Id<>(Dedsafio3.id("flash"));

	public static final PacketCodec<RegistryByteBuf, FlashPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER, FlashPayload::ticks, FlashPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
