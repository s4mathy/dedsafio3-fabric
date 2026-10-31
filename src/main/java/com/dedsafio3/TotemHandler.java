package com.dedsafio3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;

public final class TotemHandler {
	private TotemHandler() {
	}

	public static void register() {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayerEntity player)) {
				return true;
			}
			// Como el tótem vanilla, no salva de /kill ni del vacío.
			if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
				return true;
			}
			PlayerInventory inventory = player.getInventory();
			for (int i = 0; i < inventory.size(); i++) {
				ItemStack stack = inventory.getStack(i);
				if (stack.getItem() instanceof TotemItem totem) {
					stack.decrement(1);
					totem.revive(player);
					return false; // cancela la muerte
				}
			}
			return true;
		});
	}
}
