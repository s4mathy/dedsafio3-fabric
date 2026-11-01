package com.dedsafio3.client;

import com.dedsafio3.Dedsafio3;
import com.dedsafio3.ModEntities;
import com.dedsafio3.NuclearCreeperEntity;
import com.dedsafio3.ReptilianEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.IronGolemEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.CreeperEntityModel;
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

	private static class ReptilianRenderer extends BipedEntityRenderer<ReptilianEntity, BipedEntityModel<ReptilianEntity>> {
		ReptilianRenderer(EntityRendererFactory.Context context) {
			super(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE)), 0.5f);
		}

		@Override
		public Identifier getTexture(ReptilianEntity entity) {
			return texture(entity.kind().id);
		}
	}
}
