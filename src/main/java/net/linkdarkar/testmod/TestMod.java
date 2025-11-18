package net.linkdarkar.testmod;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.linkdarkar.testmod.block.ModBlocks;
import net.linkdarkar.testmod.entity.ModEntities;
import net.linkdarkar.testmod.entity.custom.CustomNPCEntity;
import net.linkdarkar.testmod.item.ModItemGroups;
import net.linkdarkar.testmod.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMod implements ModInitializer {
	public static final String MOD_ID = "testmod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");
		ModItemGroups.registerItemGroups();
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
        ModEntities.registerModEntities();


        FabricDefaultAttributeRegistry.register(ModEntities.NPC_REIMU_TEST, CustomNPCEntity.createMobAttributes());
	}
}