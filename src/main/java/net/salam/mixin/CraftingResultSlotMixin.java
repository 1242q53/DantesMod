package net.salam.mixin;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.salam.item.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Таргетируем базовый Slot — именно там определён canTakeItems в 1.20.3
@Mixin(Slot.class)
public class CraftingResultSlotMixin {

    @Inject(method = "canTakeItems", at = @At("HEAD"), cancellable = true)
    private void blockLockedCrafting(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        // Проверяем что это именно слот результата крафта
        if (!((Object) this instanceof CraftingResultSlot)) return;

        // Работаем только на сервере
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        ItemStack result = ((Slot) (Object) this).getStack();
        boolean isLocked = result.getItem() == ModItems.PISTOL
                || result.getItem() == ModItems.BULLET;

        if (!isLocked) return;

        AdvancementEntry advancement = serverPlayer.server
                .getAdvancementLoader()
                .get(new Identifier("tutorialmod", "has_custom_book"));

        if (advancement == null) return;

        AdvancementProgress progress = serverPlayer
                .getAdvancementTracker()
                .getProgress(advancement);

        if (!progress.isDone()) {
            serverPlayer.sendMessage(
                    Text.literal("§cВам нужна особая книга, чтобы скрафтить это."),
                    true // actionbar — не спамит чат
            );
            cir.setReturnValue(false);
        }
    }
}