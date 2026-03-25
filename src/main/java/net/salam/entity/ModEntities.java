package net.salam.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.salam.Tutorialmod;
import net.salam.entity.custom.DantesEntity;

public class ModEntities {
    public static final EntityType<DantesEntity> DANTES = Registry.register(Registries.ENTITY_TYPE,
            new Identifier(Tutorialmod.MOD_ID, "dantes"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, DantesEntity::new).dimensions(
                    EntityDimensions.fixed(0.8f,2f)).build());
    public static final EntityType<BulletEntity> BULLET_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(Tutorialmod.MOD_ID, "bullet_entity"),
            FabricEntityTypeBuilder.<BulletEntity>create(SpawnGroup.MISC, BulletEntity::new)
                    .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                    .build()
    );

    public static void registerModEntities() {
        Tutorialmod.LOGGER.info("Registering Entities for " + Tutorialmod.MOD_ID);
    }
}
