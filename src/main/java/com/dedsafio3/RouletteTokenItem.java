package com.dedsafio3;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Ficha de ruleta: al usarla gira la rueda de colores (el color lo decide la rueda). */
public class RouletteTokenItem extends TooltipItem {
	public RouletteTokenItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) {
			return TypedActionResult.success(stack);
		}
		if (user instanceof ServerPlayerEntity player) {
			if (Roulette.spin(player, null) && !player.isCreative()) {
				stack.decrement(1);
			}
		}
		return TypedActionResult.consume(stack);
	}
}
