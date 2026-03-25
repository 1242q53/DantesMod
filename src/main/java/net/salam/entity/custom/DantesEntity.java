package net.salam.entity.custom;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import net.salam.entity.BulletEntity;
import net.salam.entity.ModEntities;
import net.salam.entity.ai.DantesAttackGoal;
import net.salam.item.GunItem;
import net.salam.item.ModItems;
import net.salam.bossbar.DualColorBossBarManager;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DantesEntity extends HostileEntity {

    // ── TrackedData ────────────────────────────────────────────────────────
    private static final TrackedData<Boolean> ATTACKING =
            DataTracker.registerData(DantesEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SHOOTING =
            DataTracker.registerData(DantesEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> DUEL_WALKING =
            DataTracker.registerData(DantesEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    // ── Обычная стрельба (фаза 2) ──────────────────────────────────────────
    private static final int SHOOT_INTERVAL    = 100; // 5 сек
    private static final int SHOOT_DELAY_TICKS = 22;
    private static final int ANIMATION_TICKS   = 30;
    private int shootCooldown                  = SHOOT_INTERVAL;
    private int shootAnimTimer                 = 0;
    private int regularShootDelay             = 0;
    private LivingEntity regularShootTarget   = null;

    // ── Дуэльный паттерн ФАЗА 1 ───────────────────────────────────────────
    private static final int DUEL_INTERVAL     = 200; // 10 сек
    private static final float DUEL_MAX_DIST   = 50f;
    private static final int DUEL_WALK_TICKS   = 25;
    private int duelCooldown                   = DUEL_INTERVAL;
    private int duelWalkTimer                  = 0;
    private int duelShootDelay                 = 0;
    private LivingEntity duelShootTarget       = null;
    private boolean duelActive                 = false;

    // ── Дуэльный паттерн ФАЗА 2 ───────────────────────────────────────────
    private static final int DUEL2_INTERVAL    = 120; // 6 сек
    private static final float DUEL2_MAX_DIST  = 50f;
    private static final int DUEL2_WALK_TICKS  = 15;  // быстрее фазы 1
    private static final int DUEL2_BURST_COUNT = 3;   // 3 залпа подряд
    private static final int DUEL2_BURST_DELAY = 6;   // тиков между залпами
    private int duel2Cooldown                  = DUEL2_INTERVAL;
    private int duel2WalkTimer                 = 0;
    private int duel2BurstTimer                = 0;
    private int duel2BurstsDone                = 0;
    private LivingEntity duel2Target           = null;
    private boolean duel2Active                = false;

    // ── Нагрудник ──────────────────────────────────────────────────────────
    @Nullable
    private ItemEntity droppedCoat  = null;
    private int coatRemoveTimer     = 10;
    private static final int COAT_TTL = 60;

    // ── Boss bar ───────────────────────────────────────────────────────────
    private final ServerBossBar bossBar = new ServerBossBar(
            Text.literal("Дантес"),
            BossBar.Color.WHITE,
            BossBar.Style.NOTCHED_10
    );

    // ── AnimationState ─────────────────────────────────────────────────────
    public final AnimationState dashingAnimationState = new AnimationState();
    public final AnimationState idleAnimationState    = new AnimationState();
    public final AnimationState attackAnimationState  = new AnimationState();
    public final AnimationState shootAnimationState   = new AnimationState();
    private int idleAnimationTimeout   = 0;
    public  int attackAnimationTimeout = 0;

    // ══════════════════════════════════════════════════════════════════════
    public DantesEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.PISTOL));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
        DualColorBossBarManager.register(
                bossBar.getUuid(),
                BossBar.Color.WHITE, false,
                BossBar.Color.RED,   false,
                1.0f
        );
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new DantesAttackGoal(this, 1D, true));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1D));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public static DefaultAttributeContainer.Builder createDantesAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 100)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 1f)
                .add(EntityAttributes.GENERIC_ARMOR, 0.5f)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 100);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        float hpPercent = this.getHealth() / this.getMaxHealth();
        float multiplier = hpPercent >= 0.5f ? 3.0f : 1.5f;

        if (source.getSource() instanceof BulletEntity)
            return super.damage(source, amount * multiplier);
        if (source.getAttacker() instanceof PlayerEntity player)
            if (player.getMainHandStack().getItem() instanceof GunItem)
                return super.damage(source, amount * multiplier);
        return false;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ATTACKING, false);
        this.dataTracker.startTracking(SHOOTING, false);
        this.dataTracker.startTracking(DUEL_WALKING, false);
    }

    public void setAttacking(boolean v)   { this.dataTracker.set(ATTACKING, v); }
    @Override
    public boolean isAttacking()          { return this.dataTracker.get(ATTACKING); }
    public void setShooting(boolean v)    { this.dataTracker.set(SHOOTING, v); }
    public boolean isShooting()           { return this.dataTracker.get(SHOOTING); }
    public void setDuelWalking(boolean v) { this.dataTracker.set(DUEL_WALKING, v); }
    public boolean isDuelWalking()        { return this.dataTracker.get(DUEL_WALKING); }

    // ══════════════════════════════════════════════════════════════════════
    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient()) {
            float hpPercent = this.getHealth() / this.getMaxHealth();
            bossBar.setPercent(1.0f);
            DualColorBossBarManager.updateSplit(bossBar.getUuid(), hpPercent);

            boolean phase1 = hpPercent >= 0.5f;
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                    .setBaseValue(0.35f);

            // Сброс анимации выстрела
            if (this.isShooting()) {
                shootAnimTimer--;
                if (shootAnimTimer <= 0) this.setShooting(false);
            }

            // ── ФАЗА 2: стрельба с любой дистанции ────────────────────────
            if (!phase1) {
                spawnPhase2Particles((ServerWorld) this.getWorld());
                tickRegularShoot();
            } else {
                shootCooldown      = SHOOT_INTERVAL;
                regularShootDelay  = 0;
                regularShootTarget = null;
            }

            tickDuel();
            tickCoatRemoval();
        }

        if (this.getWorld().isClient()) setupAnimationStates();
    }

    // ── Обычная стрельба ───────────────────────────────────────────────────
    private void tickRegularShoot() {
        if (regularShootDelay > 0) {
            regularShootDelay--;
            if (regularShootDelay == 0 && regularShootTarget != null) {
                spawnBullet(regularShootTarget, false);

                if (this.random.nextFloat() < 0.5f) {
                    spawnBullet(regularShootTarget, false);
                    spawnBullet(regularShootTarget, false);
                }

                regularShootTarget = null;
            }
            return;
        }

        shootCooldown--;
        if (shootCooldown <= 0) {
            shootCooldown = SHOOT_INTERVAL;

            LivingEntity target = this.getTarget();
            if (target == null) {
                target = this.getWorld().getClosestPlayer(this, 64.0);
            }

            if (target != null) {
                this.getLookControl().lookAt(
                        target.getX(), target.getEyeY(), target.getZ());
                startShootAnim();
                regularShootDelay  = SHOOT_DELAY_TICKS;
                regularShootTarget = target;
            }
        }
    }

    // ── Диспетчер дуэлей: выбирает нужную фазу ────────────────────────────
    private void tickDuel() {
        float hpPercent = this.getHealth() / this.getMaxHealth();
        if (hpPercent < 0.5f) {
            tickDuel2();
        } else {
            tickDuel1();
        }
    }

    // ── Дуэль ФАЗА 1 (оригинал) ───────────────────────────────────────────
    private void tickDuel1() {
        // ШАГ 1: идём к игроку
        if (duelActive && duelWalkTimer > 0) {
            duelWalkTimer--;
            LivingEntity target = this.getTarget();
            if (target != null) {
                this.getNavigation().startMovingTo(target, 1.8D);
                this.setDuelWalking(true);
                this.getLookControl().lookAt(target.getX(), target.getEyeY(), target.getZ());
            }
            if (duelWalkTimer == 0) {
                this.getNavigation().stop();
                this.setDuelWalking(false);
                LivingEntity t = this.getTarget();
                if (t != null) {
                    dropChestplate();
                    startShootAnim();
                    duelShootDelay  = SHOOT_DELAY_TICKS;
                    duelShootTarget = t;
                    this.getWorld().playSound(null,
                            this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ENTITY_ARROW_SHOOT,
                            SoundCategory.HOSTILE, 0.6f, 0.7f);
                }
            }
            return;
        }

        // ШАГ 2: ждём и стреляем
        if (duelActive && duelShootDelay > 0) {
            duelShootDelay--;
            if (duelShootDelay == 0 && duelShootTarget != null) {
                spawnBullet(duelShootTarget, true);
                spawnBullet(duelShootTarget, true);
                duelShootTarget = null;
                duelActive      = false;
                spawnFlashParticles();
            }
            return;
        }

        // ШАГ 3: cooldown
        duelCooldown--;
        if (duelCooldown <= 0) {
            duelCooldown = DUEL_INTERVAL;
            LivingEntity target = this.getTarget();
            if (target == null)
                target = this.getWorld().getClosestPlayer(this, DUEL_MAX_DIST);
            if (target != null && this.distanceTo(target) <= DUEL_MAX_DIST) {
                duelActive      = true;
                duelWalkTimer   = DUEL_WALK_TICKS;
                duelShootDelay  = 0;
                duelShootTarget = null;
            }
        }
    }

    // ── Дуэль ФАЗА 2 (усиленная) ──────────────────────────────────────────
    // Поведение:
    //  1. Быстрый рывок к игроку (15 тиков, скорость 3.0)
    //  2. Резкая остановка + вспышка + звук
    //  3. Тройной залп: каждый залп — 2 пули (точная + с разбросом)
    //  4. После всех залпов — отскок назад от цели
    private void tickDuel2() {
        // ШАГ 1: рывок
        if (duel2Active && duel2WalkTimer > 0) {
            duel2WalkTimer--;
            LivingEntity target = this.getTarget();
            if (target != null) {
                this.getNavigation().startMovingTo(target, 3.0D);
                this.setDuelWalking(true);
                this.getLookControl().lookAt(target.getX(), target.getEyeY(), target.getZ());

                // Частицы рывка каждые 3 тика
                if (this.getWorld() instanceof ServerWorld sw && duel2WalkTimer % 3 == 0) {
                    sw.spawnParticles(net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME,
                            this.getX(), this.getY() + 0.5, this.getZ(),
                            4, 0.2, 0.2, 0.2, 0.05);
                }
            }

            if (duel2WalkTimer == 0) {
                this.getNavigation().stop();
                this.setDuelWalking(false);

                duel2Target     = this.getTarget();
                duel2BurstsDone = 0;
                duel2BurstTimer = 5; // короткая пауза перед первым залпом

                // Яркая вспышка при остановке
                if (this.getWorld() instanceof ServerWorld sw) {
                    sw.spawnParticles(net.minecraft.particle.ParticleTypes.FLASH,
                            this.getX(), this.getEyeY(), this.getZ(),
                            5, 0.2, 0.2, 0.2, 0.0);
                    sw.spawnParticles(net.minecraft.particle.ParticleTypes.EXPLOSION,
                            this.getX(), this.getY() + 1.0, this.getZ(),
                            3, 0.3, 0.3, 0.3, 0.0);
                }

                this.getWorld().playSound(null,
                        this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                        SoundCategory.HOSTILE, 0.5f, 1.8f);
            }
            return;
        }

        // ШАГ 2: серия залпов
        if (duel2Active && duel2BurstsDone < DUEL2_BURST_COUNT) {
            if (duel2BurstTimer > 0) {
                duel2BurstTimer--;
                return;
            }

            if (duel2Target != null) {
                startShootAnim();

                // Точная пуля
                spawnBulletPhase2(duel2Target, 0.0, 0.0);
                // Пуля с разбросом
                spawnBulletPhase2(duel2Target,
                        (this.random.nextDouble() - 0.5) * 0.6,
                        (this.random.nextDouble() - 0.5) * 0.6);

                this.getWorld().playSound(null,
                        this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_GENERIC_EXPLODE,
                        SoundCategory.HOSTILE, 0.6f, 1.9f);
            }

            duel2BurstsDone++;
            duel2BurstTimer = DUEL2_BURST_DELAY;

            // Последний залп — финальный эффект + отскок
            if (duel2BurstsDone >= DUEL2_BURST_COUNT) {
                spawnFlashParticles();
                duel2Active = false;

                if (duel2Target != null) {
                    Vec3d away = this.getPos().subtract(duel2Target.getPos()).normalize();
                    this.setVelocity(away.x * 1.2, 0.35, away.z * 1.2);
                    this.velocityDirty = true;
                }
            }
            return;
        }

        // ШАГ 3: cooldown
        duel2Cooldown--;
        if (duel2Cooldown <= 0) {
            duel2Cooldown = DUEL2_INTERVAL;
            LivingEntity target = this.getTarget();
            if (target == null)
                target = this.getWorld().getClosestPlayer(this, DUEL2_MAX_DIST);
            if (target != null && this.distanceTo(target) <= DUEL2_MAX_DIST) {
                duel2Active     = true;
                duel2WalkTimer  = DUEL2_WALK_TICKS;
                duel2BurstTimer = 0;
                duel2BurstsDone = 0;
                duel2Target     = null;
            }
        }
    }

    // ── Удаление нагрудника ────────────────────────────────────────────────
    private void tickCoatRemoval() {
        if (droppedCoat == null) return;
        coatRemoveTimer--;
        if (coatRemoveTimer <= 0) {
            if (!droppedCoat.isRemoved()) droppedCoat.discard();
            droppedCoat     = null;
            coatRemoveTimer = 0;
        }
    }

    // ── Вспомогательные ───────────────────────────────────────────────────
    private void startShootAnim() {
        this.setShooting(true);
        this.shootAnimTimer = ANIMATION_TICKS;
    }

    private void dropChestplate() {
        if (droppedCoat != null && !droppedCoat.isRemoved()) droppedCoat.discard();
        ItemEntity item = new ItemEntity(
                this.getWorld(),
                this.getX(), this.getY() + 0.5, this.getZ(),
                new ItemStack(Items.LEATHER_CHESTPLATE)
        );
        Vec3d fwd = this.getRotationVector().multiply(0.4);
        item.setVelocity(fwd.x, 0.25, fwd.z);
        item.setPickupDelay(60);
        this.getWorld().spawnEntity(item);
        droppedCoat     = item;
        coatRemoveTimer = COAT_TTL;
    }

    /** Стандартная пуля (фаза 1 дуэль + обычная стрельба фазы 2) */
    private void spawnBullet(LivingEntity target, boolean doubleDamage) {
        World world = this.getWorld();
        BulletEntity bullet = new BulletEntity(world, this);
        bullet.setDamage(doubleDamage ? 4.0f : 2.0f);

        float hpPercent = this.getHealth() / this.getMaxHealth();
        bullet.setBreaksShield(hpPercent < 0.5f);

        double dx = target.getX() - this.getX();
        double dy = target.getEyeY() - this.getEyeY();
        double dz = target.getZ() - this.getZ();
        if (doubleDamage) {
            dx += (this.random.nextDouble() - 0.5) * 0.4;
            dz += (this.random.nextDouble() - 0.5) * 0.4;
        }
        bullet.setVelocity(dx, dy, dz, BulletEntity.SPEED, doubleDamage ? 0.3f : 1.0f);
        world.spawnEntity(bullet);

        if (world instanceof ServerWorld sw) {
            double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
            double mx = this.getX()    + (dx/len)*0.8;
            double my = this.getEyeY() - 0.1 + (dy/len)*0.8;
            double mz = this.getZ()    + (dz/len)*0.8;
            sw.spawnParticles(net.minecraft.particle.ParticleTypes.POOF,  mx, my, mz, 6, 0.05, 0.05, 0.05, 0.02);
            sw.spawnParticles(net.minecraft.particle.ParticleTypes.FLASH, mx, my, mz, 1, 0, 0, 0, 0);
            sw.spawnParticles(net.minecraft.particle.ParticleTypes.CRIT,  mx, my, mz, 4, 0.05, 0.05, 0.05, 0.1);
        }
        world.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE,
                doubleDamage ? 0.7f : 0.4f,
                doubleDamage ? 1.8f : 1.5f);
    }

    /** Пуля дуэли ФАЗЫ 2 — больший урон, всегда пробивает щит, кастомный разброс */
    private void spawnBulletPhase2(LivingEntity target, double spreadX, double spreadZ) {
        World world = this.getWorld();
        BulletEntity bullet = new BulletEntity(world, this);
        bullet.setDamage(5.0f);
        bullet.setBreaksShield(true); // фаза 2 — всегда пробивает щит

        double dx = (target.getX() - this.getX()) + spreadX;
        double dy = target.getEyeY() - this.getEyeY();
        double dz = (target.getZ() - this.getZ()) + spreadZ;

        bullet.setVelocity(dx, dy, dz, BulletEntity.SPEED, 0.1f); // высокая точность
        world.spawnEntity(bullet);

        if (world instanceof ServerWorld sw) {
            double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
            double mx = this.getX()    + (dx/len) * 0.8;
            double my = this.getEyeY() + (dy/len) * 0.8 - 0.1;
            double mz = this.getZ()    + (dz/len) * 0.8;
            sw.spawnParticles(net.minecraft.particle.ParticleTypes.FLASH, mx, my, mz, 1, 0, 0, 0, 0);
            sw.spawnParticles(net.minecraft.particle.ParticleTypes.CRIT,  mx, my, mz, 6, 0.05, 0.05, 0.05, 0.15);
        }
    }

    /** Вспышка частиц при завершении дуэли */
    private void spawnFlashParticles() {
        if (this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(net.minecraft.particle.ParticleTypes.FLASH,
                    this.getX(), this.getEyeY(), this.getZ(),
                    3, 0.1, 0.1, 0.1, 0.0);
        }
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = this.random.nextInt(40) + 80;
            this.idleAnimationState.start(this.age);
        } else {
            --this.idleAnimationTimeout;
        }
        if (this.isShooting()) {
            this.shootAnimationState.startIfNotRunning(this.age);
        } else {
            this.shootAnimationState.stop();
        }
    }

    @Override
    protected void updateLimbs(float posDelta) {
        float f = this.getPose() == EntityPose.STANDING ? Math.min(posDelta * 6.0f, 1.0f) : 0.0f;
        this.limbAnimator.updateLimbs(f, 0.2f);
    }

    @Override
    public void takeKnockback(double strength, double x, double z) {
        super.takeKnockback(strength * 0.05, x, z); // 5% от обычной отдачи
    }

    private void spawnPhase2Particles(ServerWorld sw) {
        for (int i = 0; i < 3; i++) {
            double ox = (this.random.nextDouble()-0.5)*1.2;
            double oy = this.random.nextDouble()*2.0;
            double oz = (this.random.nextDouble()-0.5)*1.2;
            sw.spawnParticles(net.minecraft.particle.ParticleTypes.ENCHANT,
                    this.getX()+ox, this.getY()+oy, this.getZ()+oz, 1, 0, 0, 0, 0.05);
            sw.spawnParticles(net.minecraft.particle.ParticleTypes.WITCH,
                    this.getX()+ox, this.getY()+oy, this.getZ()+oz, 1, 0, 0, 0, 0.05);
        }
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (droppedCoat != null && !droppedCoat.isRemoved()) droppedCoat.discard();
        DualColorBossBarManager.unregister(bossBar.getUuid());
        DantesDeathHandler.onDantesKilled(source, this);
    }

    public Monster createChild(ServerWorld world, PassiveEntity entity) {
        return ModEntities.DANTES.create(world);
    }

    @Override protected @Nullable SoundEvent getAmbientSound() { return SoundEvents.ENTITY_FOX_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.ENTITY_CAT_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_DOLPHIN_DEATH; }
    public int getIdleAnimationTimeout() { return idleAnimationTimeout; }
    public void setIdleAnimationTimeout(int t) { this.idleAnimationTimeout = t; }
    public void initialize(ServerWorld sw, LocalDifficulty ld, SpawnReason sr, Object o) {}
}