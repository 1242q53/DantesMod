package net.salam.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.salam.item.ModItems;

public class BulletEntity extends ThrownItemEntity {

    // ======= НАСТРОЙКИ ПУЛИ =======
    private static final float DEFAULT_DAMAGE = 2.0f;
    public  static final float SPEED          = 3.0f;
    public  static final float SPREAD         = 0.5f;
    private static final float GRAVITY        = 0.03f;
    private static final boolean ON_FIRE      = false;
    private static final int FIRE_TICKS       = 60;
    private static final boolean PIERCING     = false;
    private static final float KNOCKBACK      = 0.5f;
    // ==============================

    private float damage      = DEFAULT_DAMAGE;
    /** true = пробивает щит (только фаза 2) */
    private boolean breaksShield = false;

    public BulletEntity(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    public BulletEntity(World world, LivingEntity owner) {
        super(ModEntities.BULLET_ENTITY, owner, world);
        this.setNoGravity(false);
    }

    public void setDamage(float damage)        { this.damage = damage; }
    public float getDamageAmount()             { return this.damage; }

    /** Вызывать только для пуль фазы 2 */
    public void setBreaksShield(boolean value) { this.breaksShield = value; }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BULLET;
    }

    // Пуля летит сквозь стены
    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) { }

    @Override
    protected void onCollision(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY) {
            super.onCollision(result);
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult result) {
        super.onEntityHit(result);

        var entity = result.getEntity();

        // ✅ Ломаем щит только если флаг выставлен (фаза 2)
        if (breaksShield && entity instanceof PlayerEntity player && player.isBlocking()) {
            player.disableShield(true);
        }

        entity.damage(this.getDamageSources().thrown(this, this.getOwner()), this.damage);

        if (ON_FIRE) {
            entity.setOnFireFor(FIRE_TICKS / 20);
        }

        if (entity instanceof LivingEntity living) {
            double dx = this.getVelocity().x;
            double dz = this.getVelocity().z;
            living.takeKnockback(KNOCKBACK, -dx, -dz);
        }

        if (!PIERCING) {
            this.discard();
        }
    }

    @Override
    protected float getGravity() {
        return GRAVITY;
    }
}