package com.dedsafio3;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Fruta del Espacio entre Dimensiones: al comerla desaparecen todos los Dáctylos Adultos del mundo y se quita el Insomnia. */
public class SpaceFruitItem extends TooltipItem {
	public SpaceFruitItem(Settings settings) {
		super(settings);
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (world instanceof ServerWorld serverWorld && user instanceof ServerPlayerEntity player) {
			List<Entity> adults = new ArrayList<>();
			for (ServerWorld w : serverWorld.getServer().getWorlds()) {
				for (Entity e : w.iterateEntities()) {
					if (e instanceof DactiloAdultEntity) {
						adults.add(e);
					}
				}
			}
			adults.forEach(Entity::discard);
			player.getStatHandler().setStat(player, Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST), 0);
			player.sendMessage(Text.translatable("message.dedsafio3.space_fruit", adults.size()), false);
		}
		return super.finishUsing(stack, world, user);
	}
}
