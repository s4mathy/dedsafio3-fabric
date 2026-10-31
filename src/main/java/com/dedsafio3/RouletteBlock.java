package com.dedsafio3;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Bloque de ruleta: clic derecho para girar. */
public class RouletteBlock extends Block {
	public static final MapCodec<RouletteBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.STRING.fieldOf("color").forGetter(block -> block.color.id()),
			createSettingsCodec()
	).apply(instance, (colorId, settings) -> new RouletteBlock(RouletteColor.byId(colorId), settings)));

	private final RouletteColor color;

	public RouletteBlock(RouletteColor color, Settings settings) {
		super(settings);
		this.color = color;
	}

	public RouletteColor getColor() {
		return color;
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (player instanceof ServerPlayerEntity serverPlayer) {
			Roulette.spin(serverPlayer, color);
		}
		return ActionResult.CONSUME;
	}
}
