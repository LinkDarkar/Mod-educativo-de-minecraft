package net.linkdarkar.testmod.item.custom;

import net.linkdarkar.testmod.mixin.ItemStackAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.datafixer.fix.ItemCustomNameToComponentFix;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.UUID;

public class UUIDExtractorItem extends Item {
    public UUID extractedUUID;
    public String extractedUUIDAsString;

    public UUIDExtractorItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (user.getWorld().isClient()) {
            return ActionResult.SUCCESS;
        }

        ItemStack targetItem;

        // añadir try catch en caso de uuid no existir
        try {
            this.extractedUUID = entity.getUuid();
            this.extractedUUIDAsString = entity.getUuidAsString();
        }
        catch (Exception e) {
            System.out.println("fayo we");
        }

        if (stack.getCount() > 1) {
            targetItem = stack.split(1);
            targetItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal(entity.getUuidAsString()));
            if (!user.getInventory().insertStack(targetItem)) {
                user.dropItem(targetItem, false);
            }
        }
        else {
            targetItem = stack;
            targetItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal(entity.getUuidAsString()));
            user.setStackInHand(hand, targetItem);
        }

        return ActionResult.CONSUME;
    }
}
