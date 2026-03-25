package net.salam.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.client.MinecraftClient;
import net.salam.screen.ListikGuiScreen6;

public class ListikItem6 extends Item {
    public ListikItem6(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!world.isClient) {
            return TypedActionResult.success(player.getStackInHand(hand));
        } else {
            // Открываем ваше GUI
            MinecraftClient.getInstance().setScreen(new ListikGuiScreen6());
            return TypedActionResult.success(player.getStackInHand(hand));
        }
    }
}