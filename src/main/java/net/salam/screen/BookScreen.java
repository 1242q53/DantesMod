package net.salam.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class BookScreen extends Screen {

    private int page = 0;
    private final String[] pages = {
            "Это первая страница книги. Здесь можно разместить текст.",
            "Это вторая страница книги. Можно добавить больше текста.",
            "Это третья страница книги. Продолжайте добавлять страницы."
    };

    protected BookScreen() {
        super(Text.of("Моя Книга"));
    }


    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, textRenderer, "Моя Книга", width / 2, 20, 0xFFFFFF);
        // Отрисовка текста страницы
        drawTextWithShadow(matrices, textRenderer, pages[page], 50, 50, 0xFFFFFF);
        // Кнопки навигации
        drawCenteredString(matrices, textRenderer, "<", width / 2 - 50, height - 30, 0xFFFFFF);
        drawCenteredString(matrices, textRenderer, ">", width / 2 + 50, height - 30, 0xFFFFFF);
    }

    private void renderBackground(MatrixStack matrices) {

    }

    private void drawCenteredString(MatrixStack matrices, TextRenderer textRenderer, String s, int i, int i1, int i2) {

    }

    private void drawTextWithShadow(MatrixStack matrices, TextRenderer textRenderer, String page, int i, int i1, int i2) {

    }

    private void drawCenteredText(MatrixStack matrices, TextRenderer textRenderer, String мояКнига, int i, int i1, int i2) {

    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Обработка кликов по кнопкам
        if (mouseX > width / 2 - 60 && mouseX < width / 2 - 40 && mouseY > height - 40 && mouseY < height - 20) {
            // Левый стрелка
            if (page > 0) page--;
            return true;
        } else if (mouseX > width / 2 + 40 && mouseX < width / 2 + 60 && mouseY > height - 40 && mouseY < height - 20) {
            // Правый стрелка
            if (page < pages.length - 1) page++;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}