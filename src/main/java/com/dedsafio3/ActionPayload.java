package com.dedsafio3;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Cliente -> servidor: ejecuta una acción del menú (girar ruleta, probar muerte, quitar modificaciones). */
public record ActionPayload(String action, String arg) implements CustomPayload {
	public static final CustomPayload.Id<ActionPayload> ID = new CustomPayload.Id<>(Dedsafio3.id("action"));

	public static final PacketCodec<RegistryByteBuf, ActionPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, ActionPayload::action,
			PacketCodecs.STRING, ActionPayload::arg,
			ActionPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
