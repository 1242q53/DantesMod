package net.salam.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.client.MinecraftClient;
import net.salam.screen.ListikGuiScreen7;

public class ListikItem7 extends Item {
    public ListikItem7(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!world.isClient) {
            return TypedActionResult.success(player.getStackInHand(hand));
        } else {
            // Открываем ваше GUI
            MinecraftClient.getInstance().setScreen(new ListikGuiScreen7());
            return TypedActionResult.success(player.getStackInHand(hand));
        }
    }
}