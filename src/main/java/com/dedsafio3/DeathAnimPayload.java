package com.dedsafio3;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Servidor -> cliente: reproduce la animación de muerte.
 *
 * @param name nombre del jugador que murió
 * @param self {@code true} si el receptor es quien murió (animación a pantalla completa)
 */
public record DeathAnimPayload(String name, boolean self) implements CustomPayload {
	public static final CustomPayload.Id<DeathAnimPayload> ID = new CustomPayload.Id<>(Dedsafio3.id("death_anim"));

	public static final PacketCodec<RegistryByteBuf, DeathAnimPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, DeathAnimPayload::name,
			PacketCodecs.BOOL, DeathAnimPayload::self,
			DeathAnimPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
