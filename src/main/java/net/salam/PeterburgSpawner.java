package net.salam;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtTagSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.ChunkSection;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PeterburgSpawner {

    // ── Настройки скорости ────────────────────────────────────────────────────
    private static final int CHUNKS_PER_TICK = 16;
    private static final int CLEAR_PER_TICK  = 64;
    // ── Состояние ─────────────────────────────────────────────────────────────
    private static ConcurrentLinkedQueue<ChunkBatch> chunkQueue  = null;
    private static final AtomicBoolean decodeReady  = new AtomicBoolean(false);
    private static final AtomicBoolean decodeFailed = new AtomicBoolean(false);

    private static int totalChunks    = 0;
    private static int chunksPlaced   = 0;
    private static int totalBlocks    = 0;
    private static int blocksPlaced   = 0;
    private static int structureHeight = 0;

    private static BlockPos    origin;
    private static ServerWorld targetWorld;
    // Фаза 1 — предзагрузка чанков
    private static final AtomicBoolean pregenerationDone  = new AtomicBoolean(false);
    private static int totalPregenerationChunks = 0;
    private static final AtomicInteger pregeneratedChunks = new AtomicInteger(0);

    // Фаза 2 — зачистка территории
    private static ConcurrentLinkedQueue<ChunkPos> clearQueue = null;
    private static final AtomicBoolean clearingDone = new AtomicBoolean(false);
    private static int totalClearChunks = 0;
    private static int clearedChunks    = 0;

    // ── Маркерный файл ────────────────────────────────────────────────────────
    /**
     * Файл-маркер в директории мира — надёжная защита от повторного спавна.
     * Работает независимо от ModPersistentState.
     */
    private static Path getMarkerPath(net.minecraft.server.MinecraftServer server) {
        // В 1.20.3 WorldSavePath не имеет нужных констант.
        // Строим путь через runDirectory + название мира.
        String levelName = server.getSaveProperties().getLevelName();
        return server.getRunDirectory().toPath()
                .resolve(levelName)
                .resolve("peterburg_spawned.marker");
    }

    // ── Регистрация ──────────────────────────────────────────────────────────

    public static void register() {

        // Команда /peterburg reset — сбрасывает оба флага (и файл, и persistent state)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        CommandManager.literal("peterburg")
                                .requires(src -> src.hasPermissionLevel(4))
                                .then(CommandManager.literal("reset")
                                        .executes(ctx -> {
                                            var server = ctx.getSource().getServer();
                                            ModPersistentState state = ModPersistentState.getServerState(server);
                                            state.peterburgSpawned = false;
                                            state.markDirty();
                                            try { Files.deleteIfExists(getMarkerPath(server)); }
                                            catch (IOException ignored) {}
                                            ctx.getSource().sendFeedback(
                                                    () -> Text.literal("[Salam] Флаг сброшен. Перезапусти сервер для повторного спавна."),
                                                    true
                                            );
                                            return 1;
                                        })
                                )
                )
        );

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // ── Двойная проверка: persistent state + файл-маркер ─────────────
            // Даже если один из них потеряется — второй защитит от повторного спавна.
            ModPersistentState state = ModPersistentState.getServerState(server);
            if (state.peterburgSpawned) return;

            if (Files.exists(getMarkerPath(server))) {
                // Файл есть, но флаг потерялся — синхронизируем
                state.peterburgSpawned = true;
                state.markDirty();
                System.out.println("[Salam] Маркерный файл найден — структура уже создана, пропускаем.");
                return;
            }

            ServerWorld world = server.getWorld(World.OVERWORLD);
            if (world == null) return;

            world.getChunk(10000 >> 4, -10000 >> 4);
            int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, 10000, -10000);
            if (surfaceY <= 0) surfaceY = 64;
            origin      = new BlockPos(10000, surfaceY, -10000);
            targetWorld = world;

            chunkQueue = new ConcurrentLinkedQueue<>();

            int structureChunksX = (562  + 15) / 16;
            int structureChunksZ = (2314 + 15) / 16;
            int originChunkX = origin.getX() >> 4;
            int originChunkZ = origin.getZ() >> 4;
            totalPregenerationChunks = structureChunksX * structureChunksZ;

            System.out.printf("[Salam] Ставим тикеты на %d чанков...%n", totalPregenerationChunks);

            for (int cx = 0; cx < structureChunksX; cx++) {
                for (int cz = 0; cz < structureChunksZ; cz++) {
                    ChunkPos cp = new ChunkPos(originChunkX + cx, originChunkZ + cz);
                    world.getChunkManager().addTicket(
                            net.minecraft.server.world.ChunkTicketType.FORCED, cp, 0, cp);
                }
            }
            System.out.printf("[Salam] Тикеты выставлены, ждём генерации %d чанков...%n",
                    totalPregenerationChunks);
        });

        // ── Главный тик-обработчик: 5 фаз ────────────────────────────────────
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (targetWorld == null) return;
            if (decodeFailed.get()) { cleanup(); return; }

            ModPersistentState state = ModPersistentState.getServerState(server);
            if (state.peterburgSpawned) return;

            // ══ ФАЗА 1: Ждём загрузки чанков ══════════════════════════════════
            if (!pregenerationDone.get()) {
                int originCX = origin.getX() >> 4;
                int originCZ = origin.getZ() >> 4;
                int sCX = (562  + 15) / 16;
                int sCZ = (2314 + 15) / 16;
                int loaded = 0;
                for (int cx = 0; cx < sCX; cx++)
                    for (int cz = 0; cz < sCZ; cz++)
                        if (targetWorld.getChunkManager().isChunkLoaded(originCX + cx, originCZ + cz))
                            loaded++;
                pregeneratedChunks.set(loaded);

                int pct = loaded * 100 / totalPregenerationChunks;
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList())
                    p.sendMessage(Text.literal("§7[Salam] §bГенерация чанков: §e" + pct + "%"), true);

                if (loaded >= totalPregenerationChunks) {
                    pregenerationDone.set(true);
                    System.out.println("[Salam] Все чанки готовы! Начинаем зачистку территории...");

                    clearQueue = new ConcurrentLinkedQueue<>();
                    for (int cx = 0; cx < sCX; cx++)
                        for (int cz = 0; cz < sCZ; cz++)
                            clearQueue.offer(new ChunkPos(originCX + cx, originCZ + cz));
                    totalClearChunks = clearQueue.size();

                    // Декодировка параллельно с зачисткой
                    Thread decoder = new Thread(() -> {
                        try { decodeSchemAsync(); }
                        catch (Exception e) {
                            System.err.println("[Salam] Ошибка декодировки: " + e.getMessage());
                            e.printStackTrace();
                            decodeFailed.set(true);
                        }
                    }, "salam-schem-decoder");
                    decoder.setDaemon(true);
                    decoder.start();
                }
                return;
            }

            // ══ ФАЗА 2: Полная зачистка территории ════════════════════════════
            if (!clearingDone.get()) {
                int clearedThisTick = 0;
                while (clearedThisTick < CLEAR_PER_TICK && clearQueue != null) {
                    ChunkPos cp = clearQueue.poll();
                    if (cp == null) {
                        clearingDone.set(true);
                        System.out.println("[Salam] Зачистка завершена! Ставим блоки...");
                        break;
                    }
                    clearChunk(cp.x, cp.z);
                    clearedChunks++;
                    clearedThisTick++;
                }
                int pct = totalClearChunks > 0 ? clearedChunks * 100 / totalClearChunks : 0;
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList())
                    p.sendMessage(Text.literal("§7[Salam] §cСнос рельефа: §e" + pct + "%"), true);
                return;
            }

            // ══ ФАЗА 3: Расстановка блоков ════════════════════════════════════

            // Ждём пока декодировка завершится
            if (!decodeReady.get()) {
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList())
                    p.sendMessage(Text.literal("§7[Salam] §eДекодировка структуры..."), true);
                return;
            }

            // Расставляем чанки из очереди
            if (chunkQueue != null && !chunkQueue.isEmpty()) {
                int processedThisTick = 0;
                while (processedThisTick < CHUNKS_PER_TICK) {
                    ChunkBatch batch = chunkQueue.poll();
                    if (batch == null) break;
                    placeChunkBatch(batch);
                    chunksPlaced++;
                    processedThisTick++;
                }
                if (totalChunks > 0) {
                    int pct = (chunksPlaced * 100) / totalChunks;
                    if (pct % 5 == 0 && processedThisTick > 0) {
                        System.out.printf("[Salam] Расстановка: %d%% (%d/%d чанков, %d блоков)%n",
                                pct, chunksPlaced, totalChunks, blocksPlaced);
                        Text title = Text.literal("§6Загрузка Петербурга...");
                        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                            p.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 25, 5));
                            p.networkHandler.sendPacket(new TitleS2CPacket(title));
                            p.sendMessage(Text.literal("§7[Salam] §e" + pct + "% — ещё немного..."), true);
                        }
                    }
                }
                return; // Ещё есть чанки
            }

            // ══ ГОТОВО ════════════════════════════════════════════════════════
            System.out.printf("[Salam] Готово! %d блоков, origin Y=%d%n", blocksPlaced, origin.getY());

            // Устанавливаем оба флага
            state.peterburgSpawned = true;
            state.markDirty();
            try {
                Files.createFile(getMarkerPath(server));
            } catch (IOException ignored) {}

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                player.sendMessage(
                        Text.literal("§a[Salam] Структура готова! Теперь можно строить."), false);
            }

            cleanup();
        });
    }

    // ── Фаза 2: Полная вертикальная зачистка чанка ───────────────────────────
    /**
     * Очищает чанк от самого низа мира до самого верха.
     * Это гарантирует что ни ванильный рельеф, ни пещеры, ни деревья,
     * ни другие структуры не залезут под/на нашу постройку.
     */
    private static void clearChunk(int worldChunkX, int worldChunkZ) {
        WorldChunk chunk = targetWorld.getChunk(worldChunkX, worldChunkZ);

        int startY    = origin.getY();
        int worldTopY = targetWorld.getTopY();

        // Удаляем блок-энтити начиная с Y структуры
        new ArrayList<>(chunk.getBlockEntities().keySet())
                .stream()
                .filter(pos -> pos.getY() >= startY)
                .forEach(pos -> chunk.removeBlockEntity(pos));

        BlockState AIR = net.minecraft.block.Blocks.AIR.getDefaultState();
        ChunkSection[] sections = chunk.getSectionArray();

        // Итерируем по каждому абсолютному Y от startY до потолка.
        // Это надёжнее чем работа с секциями напрямую — нет зависимости от
        // sectionIndexToCoord который ведёт себя по-разному в разных версиях.
        for (int ay = startY; ay < worldTopY; ay++) {
            int si = chunk.getSectionIndex(ay);
            if (si < 0 || si >= sections.length) continue;
            int ly = ay & 15; // локальный Y внутри секции (0–15)
            ChunkSection section = sections[si];
            for (int lx = 0; lx < 16; lx++)
                for (int lz = 0; lz < 16; lz++)
                    section.setBlockState(lx, ly, lz, AIR);
        }
        chunk.setNeedsSaving(true);
    }

    // ── Фаза 3: Вставка одного чанка из схемы ────────────────────────────────

    private static void placeChunkBatch(ChunkBatch batch) {
        int worldChunkX = (origin.getX() >> 4) + batch.chunkX;
        int worldChunkZ = (origin.getZ() >> 4) + batch.chunkZ;
        WorldChunk chunk = targetWorld.getChunk(worldChunkX, worldChunkZ);

        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        ChunkSection[] sections = chunk.getSectionArray();

        short[]      localX = batch.localX;
        short[]      y      = batch.y;
        short[]      localZ = batch.localZ;
        BlockState[] states = batch.states;

        for (int i = 0; i < batch.size; i++) {
            int ay = oy + y[i];
            int si = chunk.getSectionIndex(ay);
            if (si < 0 || si >= sections.length) continue;
            int lx = (ox + batch.chunkX * 16 + localX[i]) & 15;
            int lz = (oz + batch.chunkZ * 16 + localZ[i]) & 15;
            sections[si].setBlockState(lx, ay & 15, lz, states[i]);
            blocksPlaced++;
        }

        chunk.setNeedsSaving(true);
    }

    // ── Фоновая декодировка схемы ─────────────────────────────────────────────

    private static void decodeSchemAsync() throws Exception {
        try (InputStream is = PeterburgSpawner.class
                .getResourceAsStream("/data/salam/peterburg.schem")) {

            if (is == null) {
                System.err.println("[Salam] peterburg.schem не найден!");
                decodeFailed.set(true);
                return;
            }

            System.out.println("[Salam] Читаем peterburg.schem...");
            NbtCompound root  = NbtIo.readCompressed(is, NbtTagSizeTracker.ofUnlimitedBytes());
            NbtCompound schem = root.getCompound("Schematic");

            int width  = Short.toUnsignedInt(schem.getShort("Width"));
            int height = Short.toUnsignedInt(schem.getShort("Height"));
            int length = Short.toUnsignedInt(schem.getShort("Length"));
            totalBlocks   = width * height * length;
            structureHeight = height;

            System.out.printf("[Salam] Размер: %dx%dx%d = %d блоков%n",
                    width, height, length, totalBlocks);

            NbtCompound blocks     = schem.getCompound("Blocks");
            NbtCompound paletteNbt = blocks.getCompound("Palette");
            BlockState[] palette   = new BlockState[paletteNbt.getSize()];

            for (String key : paletteNbt.getKeys()) {
                int idx = paletteNbt.getInt(key);
                palette[idx] = parseBlockState(key);
            }

            byte[] blockData = blocks.getByteArray("Data");
            int    dataIndex = 0;

            HashMap<Long, TempBatch> tempMap = new HashMap<>(4096);

            int chunkCountX = (width  + 15) / 16;
            int chunkCountZ = (length + 15) / 16;
            totalChunks = chunkCountX * chunkCountZ;

            System.out.printf("[Salam] Декодируем %d блоков в %d чанков...%n",
                    totalBlocks, totalChunks);

            for (int blocksDone = 0; blocksDone < totalBlocks; blocksDone++) {
                int paletteIdx = 0, shift = 0;
                while (dataIndex < blockData.length) {
                    int b = blockData[dataIndex++] & 0xFF;
                    paletteIdx |= (b & 0x7F) << shift;
                    shift += 7;
                    if ((b & 0x80) == 0) break;
                }

                BlockState bs = (paletteIdx < palette.length && palette[paletteIdx] != null)
                        ? palette[paletteIdx]
                        : net.minecraft.block.Blocks.AIR.getDefaultState();

                if (bs.isAir()) continue; // Пропускаем воздух

                // Sponge v3: x + z*width + y*width*length
                int x =  blocksDone % width;
                int z = (blocksDone % (width * length)) / width;
                int y =  blocksDone / (width * length);

                int cx = x / 16, cz = z / 16;
                int lx = x % 16, lz = z % 16;

                long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
                TempBatch tb = tempMap.computeIfAbsent(key, k -> {
                    TempBatch t = new TempBatch(cx, cz);
                    t.structureWidth  = width;
                    t.structureLength = length;
                    return t;
                });
                tb.add((short) lx, (short) y, (short) lz, bs);
            }

            for (TempBatch tb : tempMap.values()) {
                chunkQueue.offer(tb.toChunkBatch());
            }
            tempMap.clear();

            System.out.println("[Salam] Декодировка завершена, ставим блоки...");
            decodeReady.set(true);
        }
    }

    // ── Очистка состояния ─────────────────────────────────────────────────────

    private static void cleanup() {
        chunkQueue  = null;
        clearQueue  = null;
        targetWorld = null;
    }

    // ── Вспомогательные классы ────────────────────────────────────────────────

    private static class TempBatch {
        final int chunkX, chunkZ;
        int structureWidth, structureLength;
        short[]      lxArr = new short[1024];
        short[]      yArr  = new short[1024];
        short[]      lzArr = new short[1024];
        BlockState[] bsArr = new BlockState[1024];
        int size = 0;

        TempBatch(int cx, int cz) { chunkX = cx; chunkZ = cz; }

        void add(short lx, short y, short lz, BlockState bs) {
            if (size >= lxArr.length) grow();
            lxArr[size] = lx; yArr[size] = y; lzArr[size] = lz; bsArr[size] = bs;
            size++;
        }

        void grow() {
            int n = lxArr.length * 2;
            lxArr = Arrays.copyOf(lxArr, n); yArr  = Arrays.copyOf(yArr,  n);
            lzArr = Arrays.copyOf(lzArr, n); bsArr = Arrays.copyOf(bsArr, n);
        }

        ChunkBatch toChunkBatch() {
            return new ChunkBatch(chunkX, chunkZ,
                    Arrays.copyOf(lxArr, size), Arrays.copyOf(yArr,  size),
                    Arrays.copyOf(lzArr, size), Arrays.copyOf(bsArr, size),
                    size, structureWidth, structureLength);
        }
    }

    private record ChunkBatch(
            int chunkX, int chunkZ,
            short[] localX, short[] y, short[] localZ,
            BlockState[] states, int size,
            int structureWidth, int structureLength) {}

    // ── Парсинг BlockState ────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState parseBlockState(String str) {
        try {
            String name  = str.contains("[") ? str.substring(0, str.indexOf('[')) : str;
            Block  block = Registries.BLOCK.get(new Identifier(name));
            BlockState bs = block.getDefaultState();

            if (str.contains("[")) {
                String props = str.substring(str.indexOf('[') + 1, str.length() - 1);
                for (String prop : props.split(",")) {
                    String[] kv = prop.split("=");
                    if (kv.length != 2) continue;
                    Property property = bs.getBlock().getStateManager().getProperty(kv[0].trim());
                    if (property == null) continue;
                    Optional<?> val = property.parse(kv[1].trim());
                    if (val.isPresent())
                        bs = bs.with((Property) property, (Comparable) val.get());
                }
            }
            return bs;
        } catch (Exception e) {
            return net.minecraft.block.Blocks.AIR.getDefaultState();
        }
    }
}