package com.dedsafio3;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Cliente -> servidor: cambia un ajuste del menú (clave + valor entero). */
public record SettingPayload(String key, int value) implements CustomPayload {
	public static final CustomPayload.Id<SettingPayload> ID = new CustomPayload.Id<>(Dedsafio3.id("setting"));

	public static final PacketCodec<RegistryByteBuf, SettingPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, SettingPayload::key,
			PacketCodecs.INTEGER, SettingPayload::value,
			SettingPayload::new
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
