package net.linkdarkar.testmod.entity;

import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.entity.custom.CustomNPCEntity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<CustomNPCEntity> NPC_REIMU_TEST = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(TestMod.MOD_ID, "npc_reimu_test"),
            EntityType.Builder.<CustomNPCEntity>create(CustomNPCEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.8f).build());

    public static void registerModEntities() {
        TestMod.LOGGER.info("Registering Mod Entities for "+ TestMod.MOD_ID);
    }
}
