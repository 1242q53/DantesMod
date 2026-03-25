package net.salam.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.io.InputStream;

public class ListikGuiScreen extends Screen {
    private boolean fontLoaded = false;
    private Font customFont; // шрифт

    // Текст листка
    private String pageText = "Уж небо осенью дышало…\n" +
            "Уж небо осенью дышало,\n" +
            "Уж реже солнышко блистало,\n" +
            "Короче становился день,\n" +
            "Лесов таинственная сень\n" +
            "С печальным шумом обнажалась,\n" +
            "Ложился на поля туман,\n" +
            "Гусей крикливых караван\n" +
            "Тянулся к югу: приближалась\n" +
            "Довольно скучная пора;\n" +
            "Стоял ноябрь уж у двора";

    // Текущая текстура фона
    private Identifier backgroundTexture;
    private boolean useAlternateBackground = false;

    public ListikGuiScreen() {
        super(Text.of("Листок"));
        backgroundTexture = new Identifier("tutorialmod", "textures/item/listik1.png");
        loadFont(); // Загружаем шрифт при создании
    }

    private void loadFont() {
        try {
            InputStream fontStream = getClass().getClassLoader().getResourceAsStream("tutorialmod/fonts/19510.ttf");
            if (fontStream != null) {
                customFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(48f);
                fontLoaded = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void init() {
        super.init();
        // Нет кнопок
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        drawPageText(context);
    }

    private void renderBackground(DrawContext context) {
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        context.drawTexture(backgroundTexture, 0, 0, this.width, this.height, 0, 0, 512, 256, 512, 256);
    }

    private void drawPageText(DrawContext context) {
        int margin = 20;
        int maxWidth = this.width - margin * 2;
        int startY = margin + 20;

        if (fontLoaded && customFont != null) {
            drawWrappedText(context, pageText, margin, startY + 105, maxWidth, 9);
        } else {
            drawWrappedText(context, pageText, margin, startY + 105, maxWidth, 9);
        }
    }

    private void drawWrappedText(DrawContext context, String text, int x, int y, int maxWidth, int lineHeight) {
        String[] paragraphs = text.split("\n");
        int curY = y;
        int textColor = 0x000000; // белый

        for (String paragraph : paragraphs) {
            String[] words = paragraph.split(" ");
            StringBuilder line = new StringBuilder();

            for (String word : words) {
                String testLine = line.length() == 0 ? word : line + " " + word;
                int width = this.textRenderer.getWidth(testLine);
                if (width > maxWidth) {
                    int lineWidth = this.textRenderer.getWidth(line.toString());
                    int centerX = x + (maxWidth - lineWidth) / 2;
                    context.drawText(this.textRenderer, line.toString(), centerX, curY, textColor, false);
                    curY += lineHeight;
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(testLine);
                }
            }
            if (line.length() > 0) {
                int lineWidth = this.textRenderer.getWidth(line.toString());
                int centerX = x + (maxWidth - lineWidth) / 2;
                context.drawText(this.textRenderer, line.toString(), centerX, curY, textColor, false);
                curY += lineHeight;
            }
            curY += lineHeight; // межабзац
        }
    }
}