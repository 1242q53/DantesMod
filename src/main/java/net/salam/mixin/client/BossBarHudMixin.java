package net.salam.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.util.Identifier;
import net.salam.bossbar.DualColorBossBarManager;
import net.salam.bossbar.DualColorData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    @Unique private static final int BAR_WIDTH  = 182;
    @Unique private static final int HALF_WIDTH = 91;
    @Unique private static final int BAR_HEIGHT = 5;

    // Оверлей: ровно 182x20 пикселей
    // Бар занимает y=7..11 внутри оверлея (центр = y=10 из 20)
    @Unique private static final int OVR_W = 182;
    @Unique private static final int OVR_H = 20;

    @Unique private static final Identifier BLACK_BG =
            new Identifier("tutorialmod", "boss_bar/black_background");
    @Unique private static final Identifier BLACK_PROGRESS =
            new Identifier("tutorialmod", "boss_bar/black_progress");
    @Unique private static final Identifier WHITE_PROGRESS =
            new Identifier("minecraft",   "boss_bar/white_progress");
    @Unique private static final Identifier RED_PROGRESS =
            new Identifier("minecraft",   "boss_bar/red_progress");
    @Unique private static final Identifier OVERLAY =
            new Identifier("tutorialmod", "textures/gui/sprites/boss_bar/dantes_overlay.png");

    @Inject(
            method = "renderBossBar(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/entity/boss/BossBar;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void salam$renderDualColor(DrawContext context,
                                       int x, int y,
                                       BossBar bossBar,
                                       CallbackInfo ci) {

        if (!(bossBar instanceof ClientBossBar clientBar)) return;
        DualColorData data = DualColorBossBarManager.get(clientBar.getUuid());
        if (data == null) return;

        float hpPercent = data.getSplitRatio();
        int redPx   = Math.round((1.0f - hpPercent) * BAR_WIDTH);
        int whitePx = Math.max(0, HALF_WIDTH - redPx);
        int blackPx = BAR_WIDTH - redPx - whitePx;

        // ── 1. Чёрный фон ─────────────────────────────────────────────────
        context.drawGuiTexture(BLACK_BG, x, y, BAR_WIDTH, BAR_HEIGHT);

        // ── 2. Чёрный прогресс ────────────────────────────────────────────
        if (blackPx > 0) {
            context.drawGuiTexture(BLACK_PROGRESS,
                    BAR_WIDTH, BAR_HEIGHT, 0, 0,
                    x, y, blackPx, BAR_HEIGHT);
        }

        // ── 3. Белый прогресс ─────────────────────────────────────────────
        if (whitePx > 0) {
            context.drawGuiTexture(WHITE_PROGRESS,
                    BAR_WIDTH, BAR_HEIGHT, HALF_WIDTH, 0,
                    x + blackPx, y, whitePx, BAR_HEIGHT);
        }

        // ── 4. Красный прогресс ───────────────────────────────────────────
        if (redPx > 0) {
            context.drawGuiTexture(RED_PROGRESS,
                    BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH - redPx, 0,
                    x + blackPx + whitePx, y, redPx, BAR_HEIGHT);
        }

        // ── 5. Декоративный оверлей ───────────────────────────────────────
        // Оверлей 182x20, центр оверлея по Y = 10
        // Центр бара по Y = y + BAR_HEIGHT/2 = y + 2
        // overlayY = (y + 2) - 10 = y - 8
        int overlayY = y - 8;
        context.drawTexture(OVERLAY,
                x, overlayY,        // позиция: точно по X бара, центрировано по Y
                0, 0,
                OVR_W, OVR_H,       // рисуем 182x20 на экране
                OVR_W, OVR_H        // текстура тоже 182x20
        );

        ci.cancel();
    }
}