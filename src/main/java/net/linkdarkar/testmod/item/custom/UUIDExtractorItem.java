package net.linkdarkar.testmod.item.custom;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.linkdarkar.testmod.util.ClientInteractionUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

public class UUIDExtractorItem extends Item {

    // A static block to register the event interceptor once when the item is loaded into the game.
    // This intercepts the interaction BEFORE the entity (like Horses or NPCs) consumes it.
    static {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isEmpty() && stack.getItem() instanceof UUIDExtractorItem extractor && entity instanceof LivingEntity living) {
                // Force the item logic to execute and override the entity's normal interaction
                return extractor.useOnEntity(stack, player, living, hand);
            }
            return ActionResult.PASS;
        });
    }

    public UUIDExtractorItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        String uuidString = entity.getUuidAsString();

        if (user.getWorld().isClient()) {
            ClientInteractionUtil.copyToClipboard("\""+uuidString+"\"");
            user.sendMessage(Text.literal("Copied UUID to clipboard: " + uuidString).formatted(Formatting.GREEN), true);
        }

        ItemStack targetItem;
        if (stack.getCount() > 1) {
            targetItem = stack.split(1);
            targetItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal(uuidString));
            if (!user.getInventory().insertStack(targetItem)) {
                user.dropItem(targetItem, false);
            }
        } else {
            targetItem = stack;
            targetItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal(uuidString));
            user.setStackInHand(hand, targetItem);
        }

        return ActionResult.SUCCESS;
    }
}