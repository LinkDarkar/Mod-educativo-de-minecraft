package net.linkdarkar.testmod.item.custom;

import net.linkdarkar.testmod.screen.custom.QuestionsPopupChiselScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;


public class QuestionsPopupChiselItem extends Item {
    public QuestionsPopupChiselItem (Settings settings) {
        super(settings);
    }

    public TypedActionResult<ItemStack> use (World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);

        if (customData != null && world.isClient()) {
            NbtCompound nbt = customData.copyNbt();
            if (nbt.contains("QuestionsPath")) {
                System.out.println("PATH: " + nbt.getString("QuestionsPath"));
                MinecraftClient.getInstance().setScreen(new QuestionsPopupChiselScreen(nbt.getString("QuestionsPath")));
            }
//            MinecraftClient.getInstance().setScreen(new QuestionsPopupChiselScreen());
        }

        return TypedActionResult.success(stack);
    }
}
