package net.salam.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.salam.entity.ModEntities;
import net.salam.entity.custom.DantesEntity;
import net.salam.item.ModItems;

public class DantesSpawnBlock extends Block {

    public DantesSpawnBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {

        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();

        boolean hasBook = mainHand.isOf(ModItems.MY_CUSTOM_BOOK)
                || offHand.isOf(ModItems.MY_CUSTOM_BOOK);

        if (!hasBook) {
            if (!world.isClient()) {
                player.sendMessage(Text.literal("§cВам нужна особая книга!"), true);
            }
            return ActionResult.SUCCESS;
        }

        if (!world.isClient()) {
            ServerWorld serverWorld = (ServerWorld) world;

            double spawnX = pos.getX() + 0.5;
            double spawnY = pos.getY() + 1.0;
            double spawnZ = pos.getZ() + 0.5;

            // ✅ Частицы ПЕРЕД спавном моба

            // Большой взрыв в центре
            serverWorld.spawnParticles(ParticleTypes.EXPLOSION,
                    spawnX, spawnY, spawnZ,
                    3, 0.3, 0.3, 0.3, 0.0);

            // Огненные частицы по кругу
            for (int i = 0; i < 20; i++) {
                double angle = (2 * Math.PI / 20) * i;
                double radius = 1;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                serverWorld.spawnParticles(ParticleTypes.FLAME,
                        spawnX + offsetX, spawnY, spawnZ + offsetZ,
                        1, 0.0, 0.05, 0.0, 0.05);
            }

            // Дым снизу вверх
            serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE,
                    spawnX, spawnY, spawnZ,
                    15, 0.4, 0.5, 0.4, 0.02);

            // Магические частицы вокруг
// ✅ Душевой огонь спиралью вверх на 10 блоков
            for (int i = 0; i < 200; i++) {
                double angle = (2 * Math.PI / 20) * i; // шаг угла спирали
                double radius = 0.8;
                double height = (10.0 / 200) * i;      // поднимается на 10 блоков
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                serverWorld.spawnParticles(ParticleTypes.WITCH,
                        spawnX + offsetX,
                        spawnY + height,
                        spawnZ + offsetZ,
                        1, 0.0, 0.0, 0.0, 0.0);
            }

// ✅ Душевой огонь спиралью вверх на 10 блоков
            for (int i = 0; i < 200; i++) {
                double angle = (2 * Math.PI / 20) * i; // шаг угла спирали
                double radius = 0.6;
                double height = (10.0 / 200) * i;      // поднимается на 10 блоков
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        spawnX + offsetX,
                        spawnY + height,
                        spawnZ + offsetZ,
                        1, 0.0, 0.0, 0.0, 0.0);
            }

            // ✅ Звуки спавна
            serverWorld.playSound(null, pos,
                    SoundEvents.ENTITY_WITHER_SPAWN,
                    SoundCategory.HOSTILE,
                    0.5f, 1.5f); // тихо и высокий тон

            serverWorld.playSound(null, pos,
                    SoundEvents.BLOCK_PORTAL_TRIGGER,
                    SoundCategory.BLOCKS,
                    0.4f, 0.8f);

            // ✅ Спавн Дантеса
            DantesEntity dantes = ModEntities.DANTES.create(serverWorld);

            if (dantes != null) {
                dantes.refreshPositionAndAngles(spawnX, spawnY, spawnZ, 0.0F, 0.0F);
                serverWorld.spawnEntity(dantes);
                player.sendMessage(Text.literal("§6Дантес призван!"), true);

                if (!player.isCreative()) {
                    if (mainHand.isOf(ModItems.MY_CUSTOM_BOOK)) {
                        mainHand.decrement(1);
                    } else {
                        offHand.decrement(1);
                    }
                }
            }
        }

        return ActionResult.SUCCESS;
    }
}