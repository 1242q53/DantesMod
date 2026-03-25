package net.salam.bossbar;

import net.minecraft.entity.boss.BossBar;

/**
 * Хранит данные двухцветного бара.
 * isColor1Black / isColor2Black — флаги для кастомной чёрной текстуры,
 * так как BossBar.Color не содержит BLACK.
 */
public class DualColorData {

    private final BossBar.Color color1;
    private final BossBar.Color color2;
    private final float splitRatio;
    private final boolean color1Black;
    private final boolean color2Black;

    public DualColorData(BossBar.Color color1, boolean color1Black,
                         BossBar.Color color2, boolean color2Black,
                         float splitRatio) {
        this.color1      = color1;
        this.color2      = color2;
        this.color1Black = color1Black;
        this.color2Black = color2Black;
        this.splitRatio  = Math.max(0f, Math.min(1f, splitRatio));
    }

    public BossBar.Color getColor1()  { return color1; }
    public BossBar.Color getColor2()  { return color2; }
    public float getSplitRatio()      { return splitRatio; }
    public boolean isColor1Black()    { return color1Black; }
    public boolean isColor2Black()    { return color2Black; }
}