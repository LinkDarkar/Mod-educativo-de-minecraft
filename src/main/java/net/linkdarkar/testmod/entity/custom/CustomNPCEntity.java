package net.linkdarkar.testmod.entity.custom;

import net.linkdarkar.testmod.vn.VisualNovelDialogueScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class CustomNPCEntity extends MobEntity {
    private static final TrackedData<String> DIALOGUE_PATH =
            DataTracker.registerData(CustomNPCEntity.class, TrackedDataHandlerRegistry.STRING);

    public CustomNPCEntity (EntityType<? extends CustomNPCEntity> type, World world) {
        super(type, world);

    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DIALOGUE_PATH, "default.json");
    }

    public String getDialoguePath() {
        return this.dataTracker.get(DIALOGUE_PATH);
    }

    public void setDialoguePath(String path) {
        this.dataTracker.set(DIALOGUE_PATH, path);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("DialoguePath")) {
            this.setDialoguePath(nbt.getString("DialoguePath"));
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("DialoguePath", this.getDialoguePath());
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (this.getWorld().isClient()) {
            String dialogue = this.getDialoguePath();
            MinecraftClient.getInstance().setScreen(new VisualNovelDialogueScreen(dialogue));
        }

        return ActionResult.SUCCESS;
    }
}
