package com.dedsafio3;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** Fruta del Espacio entre Dimensiones: al comerla te teletransporta lejos (como la Fruta de Chorus), para escapar de los Dáctylos Adultos. */
public class SpaceFruitItem extends TooltipItem {
	public SpaceFruitItem(Settings settings) {
		super(settings);
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		ItemStack result = super.finishUsing(stack, world, user);
		if (!world.isClient && user instanceof ServerPlayerEntity player) {
			for (int attempt = 0; attempt < 32; attempt++) {
				double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 80.0;
				double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 80.0;
				double y = MathHelper.clamp(player.getY() + (player.getRandom().nextInt(24) - 12),
						world.getBottomY(), world.getBottomY() + world.getDimension().logicalHeight() - 1);
				if (player.hasVehicle()) {
					player.stopRiding();
				}
				if (player.teleport(x, y, z, true)) {
					break;
				}
			}
			player.sendMessage(Text.translatable("message.dedsafio3.space_fruit"), true);
		}
		return result;
	}
}
