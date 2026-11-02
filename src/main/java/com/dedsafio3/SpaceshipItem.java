package com.dedsafio3;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/** Nave espacial: solo se puede colocar sobre una Plataforma de Despegue. */
public class SpaceshipItem extends TooltipItem {
	public SpaceshipItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		PlayerEntity player = context.getPlayer();
		if (!world.getBlockState(pos).isOf(ModBlocks.PLATFORM)) {
			if (!world.isClient && player != null) {
				player.sendMessage(Text.translatable("message.dedsafio3.ship_only_platform"), true);
			}
			return ActionResult.FAIL;
		}
		if (!(world instanceof ServerWorld serverWorld)) {
			return ActionResult.SUCCESS;
		}
		if (SpaceshipEntity.destinationFrom(serverWorld.getRegistryKey()) == null) {
			if (player != null) {
				player.sendMessage(Text.translatable("message.dedsafio3.ship_unavailable"), true);
			}
			return ActionResult.FAIL;
		}
		BlockPos above = pos.up();
		boolean free = world.getBlockState(above).isAir() && world.getBlockState(above.up()).isAir()
				&& world.getBlockState(above.up(2)).isAir()
				&& serverWorld.getEntitiesByClass(SpaceshipEntity.class, new Box(above).expand(1.0, 3.0, 1.0), e -> true).isEmpty();
		if (!free) {
			if (player != null) {
				player.sendMessage(Text.translatable("message.dedsafio3.ship_blocked"), true);
			}
			return ActionResult.FAIL;
		}
		SpaceshipEntity ship = ModEntities.SPACESHIP.create(serverWorld);
		if (ship == null) {
			return ActionResult.FAIL;
		}
		ship.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, context.getPlayerYaw(), 0f);
		serverWorld.spawnEntity(ship);
		serverWorld.playSound(null, above, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 1.0f, 0.7f);
		if (player == null || !player.isCreative()) {
			context.getStack().decrement(1);
		}
		return ActionResult.SUCCESS;
	}
}
