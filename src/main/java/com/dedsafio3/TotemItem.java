package com.dedsafio3;

import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * Tótem especial. Funciona desde cualquier casilla del inventario:
 * al recibir un golpe mortal se consume, cancela la muerte y aplica sus efectos.
 */
public class TotemItem extends TooltipItem {
	public record Buff(RegistryEntry<StatusEffect> effect, int seconds, int amplifier) {
	}

	private final float reviveHealth;
	private final List<Buff> buffs;

	public TotemItem(Settings settings, float reviveHealth, List<Buff> buffs) {
		super(settings);
		this.reviveHealth = reviveHealth;
		this.buffs = buffs;
	}

	public void revive(ServerPlayerEntity player) {
		player.setHealth(reviveHealth);
		player.clearStatusEffects();
		for (Buff buff : buffs) {
			player.addStatusEffect(new StatusEffectInstance(buff.effect(), buff.seconds() * 20, buff.amplifier()));
		}
		player.getWorld().sendEntityStatus(player, EntityStatuses.USE_TOTEM_OF_UNDYING);
	}
}
