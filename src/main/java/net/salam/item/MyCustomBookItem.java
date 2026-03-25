package net.salam.item;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.salam.block.DantesSpawnBlock;
import net.salam.screen.MyCustomGuiScreen;

public class MyCustomBookItem extends Item {
    public MyCustomBookItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        // Если смотрит на DantesSpawnBlock — не открывать GUI
        HitResult hit = player.raycast(5.0, 0, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            if (world.getBlockState(blockHit.getBlockPos()).getBlock() instanceof DantesSpawnBlock) {
                // ✅ pass = книга не использует свой эффект, блок обработает сам
                return TypedActionResult.pass(player.getStackInHand(hand));
            }
        }

        // Открываем GUI только если кликнули не на блок
        if (world.isClient()) {
            MinecraftClient.getInstance().execute(() ->
                    MinecraftClient.getInstance().setScreen(new MyCustomGuiScreen())
            );
        }

        return TypedActionResult.success(player.getStackInHand(hand));
    }
}