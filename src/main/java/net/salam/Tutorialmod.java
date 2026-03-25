package net.salam;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.salam.entity.ModEntities;
import net.salam.entity.custom.DantesEntity;
import net.salam.item.ModItemGroups;
import net.salam.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.salam.entity.ModEntities.registerModEntities;

public class Tutorialmod implements ModInitializer {
	public static final String MOD_ID = "tutorialmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItemGroups.registerItemGroups();
		ModItems.registerModItems();

		FabricDefaultAttributeRegistry.register(ModEntities.DANTES, DantesEntity.createDantesAttributes());
		registerModEntities();
		PeterburgSpawner.register();
	}
}