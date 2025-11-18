package net.linkdarkar.testmod.entity.custom;

import net.linkdarkar.testmod.vn.VisualNovelDialogueScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class CustomNPCEntity extends MobEntity {
    public CustomNPCEntity (EntityType<? extends CustomNPCEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (this.getWorld().isClient()) {
            MinecraftClient.getInstance().setScreen(new VisualNovelDialogueScreen());
        }

        return ActionResult.SUCCESS;
    }
}
