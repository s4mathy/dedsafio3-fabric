package com.dedsafio3;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Fogata del Ritual: una fogata más grande (un bloque completo de ancho) siempre encendida, donde se usa la Cuchara. */
public class RitualCampfireBlock extends Block {
	public static final MapCodec<RitualCampfireBlock> CODEC = createCodec(RitualCampfireBlock::new);
	private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 10, 16);

	public RitualCampfireBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (!world.isClient && entity instanceof LivingEntity && !entity.isFireImmune()) {
			entity.damage(world.getDamageSources().inFire(), 1.0f);
		}
		super.onEntityCollision(state, world, pos, entity);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
		double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
		if (random.nextInt(3) == 0) {
			world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, pos.getY() + 1.0, z, 0.0, 0.07, 0.0);
		}
		if (random.nextInt(4) == 0) {
			world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, pos.getY() + 0.8, z, 0.0, 0.03, 0.0);
		}
	}
}
