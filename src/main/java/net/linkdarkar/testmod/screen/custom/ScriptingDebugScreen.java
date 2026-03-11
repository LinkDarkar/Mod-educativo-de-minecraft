package net.linkdarkar.testmod.screen.custom;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.linkdarkar.testmod.networking.ModNetworking.ExecuteOncePayload;
import net.linkdarkar.testmod.networking.ModNetworking.SetTickingPayload;
import net.linkdarkar.testmod.scripting.*;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ScriptingDebugScreen extends ScriptingScreen {

    private final UUID correctEntityUuid;
    private boolean editingDefault = false;

    public ScriptingDebugScreen(LivingEntity entity) {
        super(entity);

        this.correctEntityUuid = new UUID(entity.getUuid().getMostSignificantBits(), ~entity.getUuid().getLeastSignificantBits());

        this.loadCurrentBuilder();
    }

    @Override
    protected void init() {
        // We do NOT call super.init() here because we want to completely replace some of the buttons
        this.clearChildren();
        this.scrollableWidgets.clear();

        // Left Buttons
        int btnY = 50;
        int btnWidth = 60;
        int leftOffset = 10;

        int toggleWidth = 30;
        int toggleOffset = leftOffset + btnWidth + 5;
        ScriptingConfigManager.ScriptingConfig config = ScriptingConfigManager.getInstance().getConfig(this.entityUuid);

        this.addDrawableChild(ButtonWidget.builder(Text.literal(this.editingDefault ? "Mode: DEFAULT" : "Mode: CORRECT"), b -> {
            UUID currentUuid = this.editingDefault ? this.defaultEntityUuid : this.correctEntityUuid;
            ScriptActorManager.getInstance().saveBuilder(currentUuid, this.builder);

            this.editingDefault = !this.editingDefault;
            this.loadCurrentBuilder();
            this.rebuildUI();
        }).dimensions(leftOffset, 15, btnWidth * 2, 20).build());

        // NEW VAR
        this.addDrawableChild(ButtonWidget.builder(Text.literal("VAR"), b -> {
            this.builder.AddMath();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY, btnWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowVar ? "ON" : "OFF"), b -> {
            config.allowVar = !config.allowVar;
            this.rebuildUI();
        }).dimensions(toggleOffset, btnY, toggleWidth, 20).build());

        // ADD IF
        this.addDrawableChild(ButtonWidget.builder(Text.literal("IF"), b -> {
            this.builder.AddIf();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY + 25, btnWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowIf ? "ON" : "OFF"), b -> {
            config.allowIf = !config.allowIf;
            this.rebuildUI();
        }).dimensions(toggleOffset, btnY + 25, toggleWidth, 20).build());

        // ADD WHILE
        this.addDrawableChild(ButtonWidget.builder(Text.literal("WHILE"), b -> {
            this.builder.AddWhile();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY + 50, btnWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowWhile ? "ON" : "OFF"), b -> {
            config.allowWhile = !config.allowWhile;
            this.rebuildUI();
        }).dimensions(toggleOffset, btnY + 50, toggleWidth, 20).build());

        // ADD PRINT (to chat)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("PRINT"), b -> {
            this.builder.AddPrint();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY + 75, btnWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowPrint ? "ON" : "OFF"), b -> {
            config.allowPrint = !config.allowPrint;
            this.rebuildUI();
        }).dimensions(toggleOffset, btnY + 75, toggleWidth, 20).build());

        // ADD FOLLOW_ENTITY
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Follow"), b -> {
            this.builder.AddFollowEntity();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY + 100, btnWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowFollow ? "ON" : "OFF"), b -> {
            config.allowFollow = !config.allowFollow;
            this.rebuildUI();
        }).dimensions(toggleOffset, btnY + 100, toggleWidth, 20).build());

        // ADD PLACE_BLOCK
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Place"), b -> {
            this.builder.AddPlaceBlock();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY + 125, btnWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowPlace ? "ON" : "OFF"), b -> {
            config.allowPlace = !config.allowPlace;
            this.rebuildUI();
        }).dimensions(toggleOffset, btnY + 125, toggleWidth, 20).build());

        // EXECUTE ONCE
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Exe 1"), b -> {
            NbtCompound scriptNbt = this.builder.toNbt();
            ClientPlayNetworking.send(new ExecuteOncePayload(this.entityUuid, scriptNbt));
            this.close();
        }).dimensions(leftOffset, btnY + 150, btnWidth, 20).build());

        // START LOOP
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Start"), b -> {
            NbtCompound scriptNbt = this.builder.toNbt();
            ClientPlayNetworking.send(new SetTickingPayload(this.entityUuid, true, scriptNbt));
            this.close();
        }).dimensions(leftOffset, btnY + 175, btnWidth, 20).build());

        // STOP LOOP
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Stop"), b -> {
            ClientPlayNetworking.send(new SetTickingPayload(this.entityUuid, false, new NbtCompound()));
            this.close();
        }).dimensions(leftOffset, btnY + 200, btnWidth, 20).build());

        // UUID stuff
        // ------------------------------
        List<String> entityUUIDs = this.scanInventoryForEntityUUIDs();

        int listX = this.width - 110;
        int listY = 40;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("UUIDs in Inventory"), b -> {})
                .dimensions(listX, 20, 100, 20).build()).active = false;

        for (String uuidStr : entityUUIDs) {
            String labelText;
            Entity foundEntity = this.findEntityByUUID(uuidStr);

            if (foundEntity != null) {
                String type = foundEntity.getType().getName().getString();

                if (foundEntity.hasCustomName()) {
                    labelText = Objects.requireNonNull(foundEntity.getCustomName()).getString() + " (" + type + ")";
                } else {
                    labelText = type + " " + uuidStr.substring(0, 4) + "..";
                }
            } else {
                labelText = "Unknown " + uuidStr.substring(0, 8) + "..";
            }

            this.addDrawableChild(ButtonWidget.builder(Text.literal(labelText), b -> {
                this.insertUUID(uuidStr);
            }).dimensions(listX, listY, 100, 20).build());

            listY += 25;
        }

        int finalY = this.generateWidgetsRecursive(this.builder.GetScript(), START_Y, 0);

        this.contentHeight = finalY - START_Y;

        this.updateScroll();
    }

    private void loadCurrentBuilder() {
        UUID targetUuid = this.editingDefault ? this.defaultEntityUuid : this.correctEntityUuid;
        ScriptBuilder existing = ScriptActorManager.getInstance().getBuilder(targetUuid);
        if (existing != null) {
            this.builder = existing;
        } else {
            this.builder = new ScriptBuilder(this.entity);
            ScriptActorManager.getInstance().saveBuilder(targetUuid, this.builder);
        }
    }

    @Override
    public void close() {
        UUID currentUuid = this.editingDefault ? this.defaultEntityUuid : this.correctEntityUuid;
        ScriptActorManager.getInstance().saveBuilder(currentUuid, this.builder);

        if (this.client != null && this.client.player != null) {
            this.client.setScreen(null);
        }
    }
}