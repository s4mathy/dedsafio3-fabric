package com.dedsafio3.client;

import com.dedsafio3.DactiloEntity;
import com.dedsafio3.GrassCreeperEntity;
import com.dedsafio3.Dedsafio3;
import com.dedsafio3.HerobrineEntity;
import com.dedsafio3.MagmaCreeperEntity;
import com.dedsafio3.NarvalEntity;
import com.dedsafio3.PhoraEntity;
import com.dedsafio3.WardenBombEntity;
import net.minecraft.entity.mob.MobEntity;
import com.dedsafio3.ModEntities;
import com.dedsafio3.NuclearCreeperEntity;
import com.dedsafio3.ReptilianEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.IronGolemEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.ChickenEntityModel;
import net.minecraft.client.render.entity.model.CreeperEntityModel;
import net.minecraft.client.render.entity.model.DolphinEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.util.Identifier;

/**
 * Renderers de los mobs nuevos. Usan los modelos de vanilla (creeper, zombi, golem) con una textura propia
 * (assets/dedsafio3/textures/entity/<id>.png), así que puedes reemplazar los PNG sin tocar el código.
 */
public final class ModRenderers {
	private ModRenderers() {
	}

	private static Identifier texture(String id) {
		return Dedsafio3.id("textures/entity/" + id + ".png");
	}

	public static void register() {
		EntityRendererRegistry.register(ModEntities.NUCLEAR_CREEPER, NuclearCreeperRenderer::new);
		EntityRendererRegistry.register(ModEntities.PLUMOSAURIO, PlumosaurioRenderer::new);
		EntityRendererRegistry.register(ModEntities.MAGMA_CREEPER, MagmaCreeperRenderer::new);
		EntityRendererRegistry.register(ModEntities.DACTILO, DactiloRenderer::new);
		EntityRendererRegistry.register(ModEntities.NARVAL, NarvalRenderer::new);
		EntityRendererRegistry.register(ModEntities.HEROBRINE, HerobrineRenderer::new);
		EntityRendererRegistry.register(ModEntities.GRASS_CREEPER, ctx -> new FlyerRenderer<GrassCreeperEntity>(ctx, "creeper_pasto", 0.6f, 0.6f, 0.6f));
		EntityRendererRegistry.register(ModEntities.WARDEN_BOMB,
				ctx -> new FlyerRenderer<WardenBombEntity>(ctx, "bomba_warden", 0.8f, 0.8f, 0.8f));
		EntityRendererRegistry.register(ModEntities.POD_PHORA,
				ctx -> new FlyerRenderer<PhoraEntity>(ctx, "pod_phora", 1.0f, 0.8f, 1.0f));
		EntityRendererRegistry.register(ModEntities.SKIFF_PHORA,
				ctx -> new FlyerRenderer<PhoraEntity>(ctx, "skiff_phora", 1.3f, 0.7f, 1.3f));
		ModEntities.REPTILIANS.values().forEach(type -> EntityRendererRegistry.register(type, ReptilianRenderer::new));
	}

	private static class NuclearCreeperRenderer extends MobEntityRenderer<NuclearCreeperEntity, CreeperEntityModel<NuclearCreeperEntity>> {
		NuclearCreeperRenderer(EntityRendererFactory.Context context) {
			super(context, new CreeperEntityModel<>(context.getPart(EntityModelLayers.CREEPER)), 0.6f);
		}

		@Override
		public Identifier getTexture(NuclearCreeperEntity entity) {
			return texture("creeper_nuclear");
		}

		@Override
		protected void scale(NuclearCreeperEntity entity, MatrixStack matrices, float amount) {
			matrices.scale(1.25f, 1.25f, 1.25f);
		}
	}

	private static class PlumosaurioRenderer extends IronGolemEntityRenderer {
		PlumosaurioRenderer(EntityRendererFactory.Context context) {
			super(context);
		}

		@Override
		public Identifier getTexture(IronGolemEntity entity) {
			return texture("plumosaurio");
		}

		@Override
		protected void scale(IronGolemEntity entity, MatrixStack matrices, float amount) {
			matrices.scale(1.5f, 1.5f, 1.5f);
		}
	}

	/** Naves y criaturas voladoras: modelo de creeper (cuerpo compacto) con textura y escala propias. */
	private static class FlyerRenderer<T extends MobEntity> extends MobEntityRenderer<T, CreeperEntityModel<T>> {
		private final String textureId;
		private final float sx, sy, sz;

		FlyerRenderer(EntityRendererFactory.Context context, String textureId, float sx, float sy, float sz) {
			super(context, new CreeperEntityModel<>(context.getPart(EntityModelLayers.CREEPER)), 0.4f);
			this.textureId = textureId;
			this.sx = sx;
			this.sy = sy;
			this.sz = sz;
		}

		@Override
		public Identifier getTexture(T entity) {
			return texture(textureId);
		}

		@Override
		protected void scale(T entity, MatrixStack matrices, float amount) {
			matrices.scale(sx, sy, sz);
		}
	}

	private static class MagmaCreeperRenderer extends MobEntityRenderer<MagmaCreeperEntity, CreeperEntityModel<MagmaCreeperEntity>> {
		MagmaCreeperRenderer(EntityRendererFactory.Context context) {
			super(context, new CreeperEntityModel<>(context.getPart(EntityModelLayers.CREEPER)), 0.5f);
		}

		@Override
		public Identifier getTexture(MagmaCreeperEntity entity) {
			return texture("creeper_magma");
		}
	}

	private static class DactiloRenderer extends MobEntityRenderer<DactiloEntity, ChickenEntityModel<DactiloEntity>> {
		DactiloRenderer(EntityRendererFactory.Context context) {
			super(context, new ChickenEntityModel<>(context.getPart(EntityModelLayers.CHICKEN)), 0.3f);
		}

		@Override
		public Identifier getTexture(DactiloEntity entity) {
			return texture("dactilo_bebe");
		}

		@Override
		protected void scale(DactiloEntity entity, MatrixStack matrices, float amount) {
			matrices.scale(1.4f, 1.4f, 1.4f);
		}
	}

	private static class NarvalRenderer extends MobEntityRenderer<NarvalEntity, DolphinEntityModel<NarvalEntity>> {
		NarvalRenderer(EntityRendererFactory.Context context) {
			super(context, new DolphinEntityModel<>(context.getPart(EntityModelLayers.DOLPHIN)), 0.5f);
		}

		@Override
		public Identifier getTexture(NarvalEntity entity) {
			return texture("narval");
		}

		@Override
		protected void scale(NarvalEntity entity, MatrixStack matrices, float amount) {
			matrices.scale(1.4f, 1.4f, 1.4f);
		}
	}

	private static class HerobrineRenderer extends BipedEntityRenderer<HerobrineEntity, BipedEntityModel<HerobrineEntity>> {
		HerobrineRenderer(EntityRendererFactory.Context context) {
			super(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE)), 0.5f);
		}

		@Override
		public Identifier getTexture(HerobrineEntity entity) {
			return texture("herobrine");
		}
	}

	private static class ReptilianRenderer extends BipedEntityRenderer<ReptilianEntity, BipedEntityModel<ReptilianEntity>> {
		ReptilianRenderer(EntityRendererFactory.Context context) {
			super(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE)), 0.5f);
		}

		@Override
		protected void scale(ReptilianEntity entity, MatrixStack matrices, float amount) {
			if (entity.kind() == ReptilianEntity.Kind.SALVAJE) {
				matrices.scale(0.6f, 0.6f, 0.6f); // el Salvaje es una criatura pequeña
			}
		}

		@Override
		public Identifier getTexture(ReptilianEntity entity) {
			return texture(entity.kind().id);
		}
	}
}
