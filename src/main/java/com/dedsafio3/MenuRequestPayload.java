package com.dedsafio3;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Cliente -> servidor: pide abrir el menú (el servidor comprueba permisos). */
public record MenuRequestPayload() implements CustomPayload {
	public static final CustomPayload.Id<MenuRequestPayload> ID = new CustomPayload.Id<>(Dedsafio3.id("menu_request"));

	public static final PacketCodec<RegistryByteBuf, MenuRequestPayload> CODEC = PacketCodec.unit(new MenuRequestPayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
