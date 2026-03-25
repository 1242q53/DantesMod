package net.salam;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.salam.entity.ModEntities;
import net.salam.entity.client.Dantes;
import net.salam.entity.client.DantesRenderer;
import net.salam.entity.client.ModModelLayers;


public class TutorialmodClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.DANTES, DantesRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.DANTES, Dantes::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.BULLET_ENTITY, FlyingItemEntityRenderer::new);
    }

}
