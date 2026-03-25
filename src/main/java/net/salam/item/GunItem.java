package net.salam.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.salam.entity.BulletEntity;

public class GunItem extends Item {

    private static final int COOLDOWN_TICKS = 20;

    public GunItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient) {
            ItemStack bulletStack = findBullets(user);

            if (bulletStack != null || user.isCreative()) {
                BulletEntity bullet = new BulletEntity(world, user);
                bullet.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f,
                        BulletEntity.SPEED,
                        BulletEntity.SPREAD
                );
                world.spawnEntity(bullet);

                // ✅ Дымок из дула — в направлении взгляда игрока
                ServerWorld serverWorld = (ServerWorld) world;

                // Вектор направления взгляда
                double dirX =  -Math.sin(Math.toRadians(user.getYaw())) * Math.cos(Math.toRadians(user.getPitch()));
                double dirY = -Math.sin(Math.toRadians(user.getPitch()));
                double dirZ = Math.cos(Math.toRadians(user.getYaw())) * Math.cos(Math.toRadians(user.getPitch()));

                // Позиция дула — чуть впереди игрока на уровне глаз
                double muzzleX = user.getX() + dirX * 0.8;
                double muzzleY = user.getEyeY() - 0.1 + dirY * 0.8;
                double muzzleZ = user.getZ() + dirZ * 0.8;

                // Основной дым
                serverWorld.spawnParticles(ParticleTypes.POOF,
                        muzzleX, muzzleY, muzzleZ,
                        6, 0.05, 0.05, 0.05, 0.02);

                // Небольшая вспышка
                serverWorld.spawnParticles(ParticleTypes.FLASH,
                        muzzleX, muzzleY, muzzleZ,
                        1, 0.0, 0.0, 0.0, 0.0);

                // Искры
                serverWorld.spawnParticles(ParticleTypes.CRIT,
                        muzzleX, muzzleY, muzzleZ,
                        4, 0.05, 0.05, 0.05, 0.1);

                if (!user.isCreative() && bulletStack != null) {
                    bulletStack.decrement(1);
                }

                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EXPLODE,
                        net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.5f);
            }
        }

        user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        return TypedActionResult.success(stack);
    }

    private ItemStack findBullets(PlayerEntity player) {
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() == ModItems.BULLET && !stack.isEmpty()) {
                return stack;
            }
        }
        return null;
    }
}