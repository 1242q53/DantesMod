package net.salam.item;

import net.minecraft.item.Item;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.FireballEntity; // Можно использовать любой снаряд
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;

public class BulletItem extends Item {
    public BulletItem(Settings settings) {
        super(settings);
    }
}