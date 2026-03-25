package net.salam.bossbar;

import net.minecraft.entity.boss.BossBar;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DualColorBossBarManager {

    private static final Map<UUID, DualColorData> REGISTRY = new ConcurrentHashMap<>();

    private DualColorBossBarManager() {}

    /**
     * Регистрирует бар.
     * Для кастомного чёрного цвета: передай любой цвет (например WHITE) и isBlack=true.
     * Mixin подставит текстуру salam:boss_bar/black_*.
     */
    public static void register(UUID uuid,
                                BossBar.Color color1, boolean color1Black,
                                BossBar.Color color2, boolean color2Black,
                                float splitRatio) {
        REGISTRY.put(uuid, new DualColorData(color1, color1Black, color2, color2Black, splitRatio));
    }

    public static void unregister(UUID uuid) {
        REGISTRY.remove(uuid);
    }

    @Nullable
    public static DualColorData get(UUID uuid) {
        return REGISTRY.get(uuid);
    }

    public static void updateSplit(UUID uuid, float newSplitRatio) {
        DualColorData old = REGISTRY.get(uuid);
        if (old != null) {
            REGISTRY.put(uuid, new DualColorData(
                    old.getColor1(), old.isColor1Black(),
                    old.getColor2(), old.isColor2Black(),
                    newSplitRatio
            ));
        }
    }
}