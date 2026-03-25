package net.salam.entity.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.salam.Tutorialmod;
import net.salam.entity.custom.DantesEntity;

public class DantesRenderer extends MobEntityRenderer<DantesEntity, Dantes<DantesEntity>> {

    private static final Identifier TEXTURE =
            new Identifier(Tutorialmod.MOD_ID, "textures/entity/dantes.png");

    public DantesRenderer(EntityRendererFactory.Context context) {
        super(context, new Dantes<>(context.getPart(ModModelLayers.DANTES)), 0.6f);

        // ✅ Это заставляет рендерер показывать предмет в руке (пистолет)
        this.addFeature(new HeldItemFeatureRenderer<>(this, context.getHeldItemRenderer()));
    }

    @Override
    public Identifier getTexture(DantesEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(DantesEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (entity.isBaby()) {
            matrices.scale(0.5f, 0.5f, 0.5f);
        }
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}