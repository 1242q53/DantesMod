package net.salam.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.salam.Tutorialmod;

public class ModItemGroups {
    public static final ItemGroup Listik = Registry.register(Registries.ITEM_GROUP,
            new Identifier(Tutorialmod.MOD_ID, "listiks"),
            FabricItemGroup.builder().displayName(Text.translatable("itemgroup.listiks"))
                    .icon(() -> new ItemStack(ModItems.LISTIK1)).entries((displayContext, entries) -> {

                        entries.add(ModItems.LISTIK1);
                        entries.add(ModItems.LISTIK2);
                        entries.add(ModItems.LISTIK3);
                        entries.add(ModItems.LISTIK4);
                        entries.add(ModItems.LISTIK5);
                        entries.add(ModItems.LISTIK6);
                        entries.add(ModItems.LISTIK7);
                        entries.add(ModItems.LISTIK8);


                    }).build());

    public static void registerItemGroups() {
        Tutorialmod.LOGGER.info("Registring Item Groups for " + Tutorialmod.MOD_ID);
    }
}
