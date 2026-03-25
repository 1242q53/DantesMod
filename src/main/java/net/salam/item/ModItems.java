package net.salam.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.salam.Tutorialmod;
import net.salam.block.DantesSpawnBlock;
import net.salam.entity.ModEntities;

public class ModItems {
    public static final Item MY_CUSTOM_BOOK = registerItem("my_custom_book", new MyCustomBookItem(new FabricItemSettings()));
    public static final Item LISTIK1 = registerItem("listik1", new ListikItem(new FabricItemSettings()));
    public static final Item LISTIK2 = registerItem("listik2", new ListikItem2(new FabricItemSettings()));
    public static final Item LISTIK3 = registerItem("listik3", new ListikItem3(new FabricItemSettings()));
    public static final Item LISTIK4 = registerItem("listik4", new ListikItem4(new FabricItemSettings()));
    public static final Item LISTIK5 = registerItem("listik5", new ListikItem5(new FabricItemSettings()));
    public static final Item LISTIK6 = registerItem("listik6", new ListikItem6(new FabricItemSettings()));
    public static final Item LISTIK7 = registerItem("listik7", new ListikItem7(new FabricItemSettings()));
    public static final Item LISTIK8 = registerItem("listik8", new ListikItem8(new FabricItemSettings()));
    public static final Item PISTOL = registerItem("pistol", new GunItem(new FabricItemSettings().maxCount(1)));
    public static final Item BULLET = registerItem("bullet", new BulletItem(new FabricItemSettings().maxCount(16)));
    public static final Block DANTES_SPAWN_BLOCK = registerBlock("dantes_spawn_block",
            new DantesSpawnBlock(FabricBlockSettings.copyOf(Blocks.OBSIDIAN).strength(4.0f)));

    private static Block registerBlock(String name, Block block) {
        // Регистрируем BlockItem чтобы блок был в инвентаре
        Registry.register(Registries.ITEM, new Identifier(Tutorialmod.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings()));
        // Регистрируем сам блок
        return Registry.register(Registries.BLOCK, new Identifier(Tutorialmod.MOD_ID, name), block);
    }

    public static final Item DANTES_SPAWN_EGG = registerItem("dantes_spawn_egg", new SpawnEggItem(ModEntities.DANTES, 0xa86518,0x3b260f, new FabricItemSettings()));

    private static void addItemsToIngridientItemGroup(FabricItemGroupEntries entries) {

        entries.add(MY_CUSTOM_BOOK);
        entries.add(LISTIK1);
        entries.add(LISTIK2);
        entries.add(LISTIK3);
        entries.add(LISTIK4);
        entries.add(LISTIK5);
        entries.add(LISTIK6);
        entries.add(LISTIK7);
        entries.add(LISTIK8);
        entries.add(DANTES_SPAWN_EGG);
        entries.add(PISTOL);
        entries.add(BULLET);
        entries.add(DANTES_SPAWN_BLOCK);

    }
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(Tutorialmod.MOD_ID, name), item);
    }

    public static void registerModItems() {
        Tutorialmod.LOGGER.info("Registering Mod Items for " + Tutorialmod.MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(ModItems::addItemsToIngridientItemGroup);
    }
}
