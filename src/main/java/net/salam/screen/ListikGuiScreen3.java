package net.salam.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.io.InputStream;

public class ListikGuiScreen3 extends Screen {
    private boolean fontLoaded = false;
    private Font customFont; // шрифт

    // Текст листка
    private String pageText = "Я вас любил: любовь еще, быть может…\n" +
            "Я вас любил: любовь еще, быть может,\n" +
            "В душе моей угасла не совсем;\n" +
            "Но пусть она вас больше не тревожит;\n" +
            "Я не хочу печалить вас ничем.\n" +
            "Я вас любил безмолвно, безнадежно,\n" +
            "То робостью, то ревностью томим;\n" +
            "Я вас любил так искренно, так нежно,\n" +
            "Как дай вам Бог любимой быть другим.";

    // Текущая текстура фона
    private Identifier backgroundTexture;
    private boolean useAlternateBackground = false;

    public ListikGuiScreen3() {
        super(Text.of("Листок"));
        backgroundTexture = new Identifier("tutorialmod", "textures/item/listik3.png");
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