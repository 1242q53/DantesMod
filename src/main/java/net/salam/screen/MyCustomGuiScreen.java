package net.salam.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.salam.item.ModItems;
import org.lwjgl.glfw.GLFW;

import java.awt.Font;
import java.io.InputStream;

public class MyCustomGuiScreen extends Screen {
    private Font customFont;
    private boolean fontLoaded = false;

    private int currentPage = 0;
    private final String[] pages = {};
    private int maxPage = 8; // ← было 6, теперь 8 (страница 8 = крафты)

    // Тексты страниц
    private String leftPageText = "";
    private String rightPageText = "";
    private String leftPageText2 = "";
    private String rightPageText2 = "";
    private String leftPageText3 = "";
    private String rightPageText3 = "";
    private String leftPageText4 = "";
    private String rightPageText4 = "";

    private final ItemStack[] items = new ItemStack[]{};

    private Identifier backgroundTexture;
    private boolean useAlternateBackground = false;

    // ── Крафт пистолета: ! * ! / * * * / ! * !  где *=iron_ingot, !=bullet ──
    private static final ItemStack[] PISTOL_GRID = {
            new ItemStack(ModItems.BULLET),  new ItemStack(Items.IRON_INGOT), new ItemStack(ModItems.BULLET),
            new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT),
            new ItemStack(ModItems.BULLET),  new ItemStack(Items.IRON_INGOT), new ItemStack(ModItems.BULLET),
    };

    // ── Крафт пули:  _#_ / #I# / _#_  где #=gunpowder, I=iron_nugget ──
    private static final ItemStack[] BULLET_GRID = {
            ItemStack.EMPTY,                 new ItemStack(Items.GUNPOWDER),   ItemStack.EMPTY,
            new ItemStack(Items.GUNPOWDER),  new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.GUNPOWDER),
            ItemStack.EMPTY,                 new ItemStack(Items.GUNPOWDER),   ItemStack.EMPTY,
    };

    public MyCustomGuiScreen() {
        super(Text.of("Моя книга"));
        backgroundTexture = new Identifier("tutorialmod", "textures/item/book_background.png");
        loadFont();
    }

    private void loadFont() {
        try {
            InputStream fontStream = getClass().getClassLoader().getResourceAsStream("tutorialmod/fonts/19510.ttf");
            if (fontStream != null) {
                customFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(48f);
                fontLoaded = true;
                System.out.println("OTF шрифт успешно загружен");
            } else {
                System.out.println("Файл шрифта не найден");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void init() {
        super.init();
        updatePageTexts();
        this.addDrawableChild(ButtonWidget.builder(Text.of("Далее"), button -> nextPage())
                .dimensions(this.width / 2 + 10, this.height - 40, 80, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.of("Назад"), button -> previousPage())
                .dimensions(this.width / 2 - 90, this.height - 40, 80, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        drawBookPage(context);
        for (var element : this.children()) {
            if (element instanceof net.minecraft.client.gui.Drawable drawable) {
                drawable.render(context, mouseX, mouseY, delta);
            }
        }
    }

    private void renderBackground(DrawContext context) {
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        context.drawTexture(backgroundTexture, 0, 0, this.width, this.height, 0, 0, 512, 256, 512, 256);
    }

    private void drawBookPage(DrawContext context) {
        int margin = 20;
        int pageWidth  = this.width  - margin * 2;
        int pageHeight = this.height - margin * 2 - 50;

        int leftX     = margin;
        int rectWidth = pageWidth / 2 - 10;
        int rectY     = margin;
        int rightX    = margin + rectWidth + 10;

        // Страница 8 — крафты и координаты
        if (currentPage == 8) {
            drawLastPage(context, leftX, rectY, rectWidth, rightX);
        } else {
            drawPageContentWithRightX(context, leftX, rectY, rectWidth, rightX);
        }

        if (fontLoaded && customFont != null) {
            drawCustomText(context, leftX + 10, rectY + 10, rectWidth - 20, 9);
        }
    }

    // ── Страница 8: координаты (лево) + два крафта (право) ────────────────
    private void drawLastPage(DrawContext context, int leftX, int rectY, int rectWidth, int rightX) {
        int textColor  = 0x000000;
        int titleColor = 0x8B0000;

        // ── ЛЕВАЯ ЧАСТЬ: координаты ────────────────────────────────────────
        int lx = leftX + 10;
        int ly = rectY + 20;

        String title = "Место дуэли";
        int titleW = this.textRenderer.getWidth(title);
        context.drawText(this.textRenderer, title,
                lx + (rectWidth - 20 - titleW) / 2, ly, titleColor, false);
        ly += 14;

        context.fill(lx, ly, lx + rectWidth - 20, ly + 1, 0x88000000);
        ly += 8;

        String[] lines = {
                "Чтобы встретить Дантеса,",
                "отправляйся на эти",
                "координаты:",
                "",
                "X: 10260",
                "Y: 88",
                "Z: -8061",
                "",
                "Возьми с собой пистолет",
                "и запас пуль.",
                "Удачи в дуэли!",
        };
        for (String line : lines) {
            int lw = this.textRenderer.getWidth(line);
            context.drawText(this.textRenderer, line,
                    lx + (rectWidth - 20 - lw) / 2, ly, textColor, false);
            ly += 10;
        }

        // ── ПРАВАЯ ЧАСТЬ: два крафта ───────────────────────────────────────
        int rx = rightX + 10;
        int ry = rectY + 15;

        String pistolTitle = "Крафт: Пистолет";
        int ptW = this.textRenderer.getWidth(pistolTitle);
        context.drawText(this.textRenderer, pistolTitle,
                rx + (rectWidth - 20 - ptW) / 2, ry, titleColor, false);
        ry += 12;

        int gridX = rx + (rectWidth - 20 - (3 * 18 + 2 + 14 + 16)) / 2;
        drawCraftingGrid(context, PISTOL_GRID, new ItemStack(ModItems.PISTOL), gridX, ry);
        ry += 3 * 18 + 10;

        context.fill(rx, ry, rx + rectWidth - 20, ry + 1, 0x88000000);
        ry += 8;

        String bulletTitle = "Крафт: Пуля (x4)";
        int btW = this.textRenderer.getWidth(bulletTitle);
        context.drawText(this.textRenderer, bulletTitle,
                rx + (rectWidth - 20 - btW) / 2, ry, titleColor, false);
        ry += 12;

        int gridX2 = rx + (rectWidth - 20 - (3 * 18 + 2 + 14 + 16)) / 2;
        drawCraftingGrid(context, BULLET_GRID, new ItemStack(ModItems.BULLET, 4), gridX2, ry);
    }

    private void drawCraftingGrid(DrawContext context, ItemStack[] grid, ItemStack result, int x, int y) {
        int cellSize = 18;
        int gridBg   = 0xFF8B8B8B;
        int border   = 0xFF373737;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int cx = x + col * cellSize;
                int cy = y + row * cellSize;

                context.fill(cx, cy, cx + cellSize - 1, cy + cellSize - 1, gridBg);
                context.fill(cx, cy, cx + cellSize - 1, cy + 1, border);
                context.fill(cx, cy + cellSize - 2, cx + cellSize - 1, cy + cellSize - 1, border);
                context.fill(cx, cy, cx + 1, cy + cellSize - 1, border);
                context.fill(cx + cellSize - 2, cy, cx + cellSize - 1, cy + cellSize - 1, border);

                ItemStack stack = grid[row * 3 + col];
                if (!stack.isEmpty()) {
                    context.drawItem(stack, cx + 1, cy + 1);
                }
            }
        }

        int arrowX = x + 3 * cellSize + 4;
        int arrowY = y + cellSize + 4;
        context.drawText(this.textRenderer, "->", arrowX, arrowY, 0xFF555555, false);

        int resX = arrowX + 16;
        int resY = y + cellSize - 1;
        context.fill(resX, resY, resX + cellSize + 3, resY + cellSize + 3, 0xFF6B6B6B);
        context.fill(resX + 1, resY + 1, resX + cellSize + 2, resY + cellSize + 2, 0xFF8B8B8B);
        context.drawItem(result, resX + 2, resY + 2);

        if (result.getCount() > 1) {
            context.drawText(this.textRenderer,
                    String.valueOf(result.getCount()),
                    resX + 12, resY + 12, 0xFFFFFF, true);
        }
    }

    private void drawPageContentWithRightX(DrawContext context, int leftX, int rectY, int rectWidth, int rightX) {
        if (currentPage == 0) {
            drawWrappedText(context, leftPageText,  leftX + 10, rectY + 125, rectWidth - 20, 9);
            drawWrappedText(context, rightPageText, rightX + 20, rectY + 15, rectWidth - 20, 8);
        } else if (currentPage == 2) {
            drawWrappedText(context, leftPageText2,  leftX + 10, rectY + 125, rectWidth - 20, 9);
            drawWrappedText(context, rightPageText2, rightX + 10, rectY + 80,  rectWidth - 20, 9);
        } else if (currentPage == 4) {
            drawWrappedText(context, leftPageText3,  leftX + 10, rectY + 25, rectWidth - 20, 8);
            drawWrappedText(context, rightPageText3, rightX + 20, rectY + 15, rectWidth - 20, 6);
        } else if (currentPage == 6) {
            // Страница 6 — оригинальные стихи (Ты и вы + Если жизнь тебя обманет)
            drawWrappedText(context, leftPageText4,  leftX + 10, rectY + 125, rectWidth - 20, 9);
            drawWrappedText(context, rightPageText4, rightX + 10, rectY + 125, rectWidth - 20, 9);
        }
    }

    private void drawWrappedText(DrawContext context, String text, int x, int y, int maxWidth, int lineHeight) {
        String[] paragraphs = text.split("\n");
        int curY = y;
        int textColor = 0x000000;

        for (String paragraph : paragraphs) {
            String[] words = paragraph.split(" ");
            StringBuilder line = new StringBuilder();

            for (String word : words) {
                String testLine = line.length() == 0 ? word : line + " " + word;
                int width = this.textRenderer.getWidth(testLine);
                if (width > maxWidth) {
                    int textWidth = this.textRenderer.getWidth(line.toString());
                    int centerX = x + (maxWidth - textWidth) / 2;
                    context.drawText(this.textRenderer, line.toString(), centerX, curY, textColor, false);
                    curY += lineHeight;
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(testLine);
                }
            }
            if (line.length() > 0) {
                int textWidth = this.textRenderer.getWidth(line.toString());
                int centerX = x + (maxWidth - textWidth) / 2;
                context.drawText(this.textRenderer, line.toString(), centerX, curY, textColor, false);
                curY += lineHeight;
            }
            curY += lineHeight;
        }
    }

    private void drawCustomText(DrawContext context, int x, int y, int maxWidth, int lineHeight) {
        String[] lines = leftPageText.split("\n");
        int curY = y;
        int textColor = 0xFFFFFF;
        for (String line : lines) {
            int width = this.textRenderer.getWidth(line);
            int centerX = x + (maxWidth - width) / 2;
            context.drawText(this.textRenderer, line, centerX, curY, textColor, false);
            curY += lineHeight;
        }
    }

    private void nextPage() {
        if (currentPage + 2 <= maxPage) {
            currentPage += 2;
            updatePageTexts();
        }
    }

    private void updatePageTexts() {
        if (currentPage == 0) {
            leftPageText = "Уж небо осенью дышало…\n" +
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
            rightPageText = "Я помню чудное мгновенье:\n" +
                    "Передо мной явилась ты,\n" +
                    "Как мимолетное виденье,\n" +
                    "Как гений чистой красоты.\n" +
                    "В томленьях грусти безнадежной,\n" +
                    "В тревогах шумной суеты,\n" +
                    "Звучал мне долго голос нежный\n" +
                    "И снились милые черты.\n" +
                    "Шли годы. Бурь порыв мятежный\n" +
                    "Рассеял прежние мечты,\n" +
                    "И я забыл твой голос нежный,\n" +
                    "Твои небесные черты.\n" +
                    "В глуши, во мраке заточенья\n" +
                    "Тянулись тихо дни мои\n" +
                    "Без божества, без вдохновенья,\n" +
                    "Без слез, без жизни, без любви.\n" +
                    "Душе настало пробужденье:\n" +
                    "И вот опять явилась ты,\n" +
                    "Как мимолетное виденье,\n" +
                    "Как гений чистой красоты.\n" +
                    "И сердце бьется в упоенье,\n" +
                    "И для него воскресли вновь\n" +
                    "И божество, и вдохновенье,\n" +
                    "И жизнь, и слезы, и любовь.";
        } else if (currentPage == 2) {
            leftPageText2 = "Я вас любил: любовь еще, быть может…\n" +
                    "Я вас любил: любовь еще, быть может,\n" +
                    "В душе моей угасла не совсем;\n" +
                    "Но пусть она вас больше не тревожит;\n" +
                    "Я не хочу печалить вас ничем.\n" +
                    "Я вас любил безмолвно, безнадежно,\n" +
                    "То робостью, то ревностью томим;\n" +
                    "Я вас любил так искренно, так нежно,\n" +
                    "Как дай вам Бог любимой быть другим.";
            rightPageText2 = "Узник\n" +
                    "Сижу за решеткой в темнице сырой.\n" +
                    "Вскормленный в неволе орел молодой,\n" +
                    "Мой грустный товарищ, махая крылом,\n" +
                    "Кровавую пищу клюет под окном,\n" +
                    "Клюет, и бросает, и смотрит в окно,\n" +
                    "Как будто со мною задумал одно.\n" +
                    "Зовет меня взглядом и криком своим\n" +
                    "И вымолвить хочет: «Давай улетим!\n" +
                    "Мы вольные птицы; пора, брат, пора!\n" +
                    "Туда, где за тучей белеет гора,\n" +
                    "Туда, где синеют морские края,\n" +
                    "Туда, где гуляем лишь ветер… да я!..»";
        } else if (currentPage == 4) {
            leftPageText3 = "К Чаадаеву\n" +
                    "Любви, надежды, тихой славы\n" +
                    "Недолго нежил нас обман,\n" +
                    "Исчезли юные забавы,\n" +
                    "Как сон, как утренний туман;\n" +
                    "Но в нас горит еще желанье,\n" +
                    "Под гнетом власти роковой\n" +
                    "Нетерпеливою душой\n" +
                    "Отчизны внемлем призыванье.\n" +
                    "Мы ждем с томленьем упованья\n" +
                    "Минуты вольности святой,\n" +
                    "Как ждет любовник молодой\n" +
                    "Минуты верного свиданья.\n" +
                    "Пока свободою горим,\n" +
                    "Пока сердца для чести живы,\n" +
                    "Мой друг, отчизне посвятим\n" +
                    "Души прекрасные порывы!\n" +
                    "Товарищ, верь: взойдет она,\n" +
                    "Звезда пленительного счастья,\n" +
                    "Россия вспрянет ото сна,\n" +
                    "И на обломках самовластья\n" +
                    "Напишут наши имена!";
            rightPageText3 = "Пророк\n" +
                    "Духовной жаждою томим,\n" +
                    "В пустыне мрачной я влачился, —\n" +
                    "И шестикрылый серафим\n" +
                    "На перепутье мне явился.\n" +
                    "Перстами легкими как сон\n" +
                    "Моих зениц коснулся он.\n" +
                    "Отверзлись вещие зеницы,\n" +
                    "Как у испуганной орлицы.\n" +
                    "Моих ушей коснулся он, —\n" +
                    "И их наполнил шум и звон:\n" +
                    "И внял я неба содроганье,\n" +
                    "И горний ангелов полет,\n" +
                    "И гад морских подводный ход,\n" +
                    "И дольней лозы прозябанье.\n" +
                    "И он к устам моим приник,\n" +
                    "И вырвал грешный мой язык,\n" +
                    "И празднословный и лукавый,\n" +
                    "И жало мудрыя змеи\n" +
                    "В уста замершие мои\n" +
                    "Вложил десницею кровавой.\n" +
                    "И он мне грудь рассек мечом,\n" +
                    "И сердце трепетное вынул,\n" +
                    "И угль, пылающий огнем,\n" +
                    "Во грудь отверстую водвинул.\n" +
                    "Как труп в пустыне я лежал,\n" +
                    "И бога глас ко мне воззвал:\n" +
                    "«Восстань, пророк, и виждь, и внемли,\n" +
                    "Исполнись волею моей,\n" +
                    "И, обходя моря и земли,\n" +
                    "Глаголом жги сердца людей».";
        } else if (currentPage == 6) {
            leftPageText4 = "Ты и вы\n" +
                    "Пустое вы сердечным ты\n" +
                    "Она, обмолвясь, заменила\n" +
                    "И все счастливые мечты\n" +
                    "В душе влюбленной возбудила.\n" +
                    "Пред ней задумчиво стою,\n" +
                    "Свести очей с нее нет силы;\n" +
                    "И говорю ей: как вы милы!\n" +
                    "И мыслю: как тебя люблю!";
            rightPageText4 = "«Если жизнь тебя обманет…»\n" +
                    " \n" +
                    "Если жизнь тебя обманет,\n" +
                    "Не печалься, не сердись!\n" +
                    "В день уныния смирись:\n" +
                    "День веселья, верь, настанет.\n" +
                    " \n" +
                    " \n" +
                    "Сердце в будущем живет;\n" +
                    "Настоящее уныло:\n" +
                    "Всё мгновенно, всё пройдет;\n" +
                    "Что пройдет, то будет мило.";
        }
        // currentPage == 8 рисуется отдельно в drawLastPage()
    }

    private void previousPage() {
        if (currentPage - 2 >= 0) {
            currentPage -= 2;
            updatePageTexts();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_H) {
            toggleBackground();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void toggleBackground() {
        useAlternateBackground = !useAlternateBackground;
        if (useAlternateBackground) {
            backgroundTexture = new Identifier("tutorialmod", "textures/item/book_background_alt.png");
        } else {
            backgroundTexture = new Identifier("tutorialmod", "textures/item/book_background.png");
        }
    }
}