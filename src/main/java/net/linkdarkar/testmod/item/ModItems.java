package net.linkdarkar.testmod.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.item.custom.ChiselItem;
import net.linkdarkar.testmod.item.custom.QuestionsPopupChiselItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item PINK_GARNET = registerItem("pink_garnet", new Item (new Item.Settings()));
    public static final Item RAW_PINK_GARNET = registerItem("raw_pink_garnet", new Item (new Item.Settings()));
    public static final Item CHISEL = registerItem("chisel", new ChiselItem (new Item.Settings().maxDamage(128)));
    public static final Item QUESTIONS_POPUP_CHISEL = registerItem("questions_popup_chisel", new QuestionsPopupChiselItem(new Item.Settings().maxDamage(128)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(TestMod.MOD_ID, name), item);
    }

    // first of, we need a method to call this class so everything in here gets initialized
    public static void registerModItems() {
        TestMod.LOGGER.info("Registering Mod Items for " + TestMod.MOD_ID);

        // ItemGroups.INGREDIENTS will put the item in the ingredients tab in the creative mode
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(fabricItemGroupEntries -> {
            fabricItemGroupEntries.add(PINK_GARNET);
            fabricItemGroupEntries.add(RAW_PINK_GARNET);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register( fabricItemGroupEntries -> {
            fabricItemGroupEntries.add(CHISEL);
            fabricItemGroupEntries.add(QUESTIONS_POPUP_CHISEL);
        });
    }
}
