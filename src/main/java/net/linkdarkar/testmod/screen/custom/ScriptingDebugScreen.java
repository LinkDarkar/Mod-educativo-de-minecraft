package net.linkdarkar.testmod.screen.custom;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.linkdarkar.testmod.networking.ModNetworking.ExecuteOncePayload;
import net.linkdarkar.testmod.networking.ModNetworking.SetTickingPayload;
import net.linkdarkar.testmod.scripting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ScriptingDebugScreen extends ScriptingScreen {

    private final UUID correctEntityUuid;
    private boolean editingDefault = false;

    private enum Tab
    {
        SCRIPT,
        ACTIONS
    }
    private Tab currentTab = Tab.SCRIPT;

    private final ScriptingConfigManager.EntityActions entityActions;

    public ScriptingDebugScreen(LivingEntity entity) {
        super(entity);

        this.correctEntityUuid = new UUID(entity.getUuid().getMostSignificantBits(), ~entity.getUuid().getLeastSignificantBits());

        this.entityActions = ScriptingConfigManager.getInstance().getActions(this.entityUuid);

        this.loadCurrentBuilder();
    }

    @Override
    protected void init() {
        // We do NOT call super.init() here because we want to completely replace some of the buttons
        this.clearChildren();
        this.scrollableWidgets.clear();

        int leftOffset = 10;
        int btnWidth = 60;

        // Export Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("EXPORT CMD"), b -> {
            exportNpcToClipboard();
        }).dimensions(leftOffset, this.height - 30, btnWidth * 2, 20).build());

        // Tab Switcher Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Tab: " + currentTab.name()), b -> {
            this.currentTab = (this.currentTab == Tab.SCRIPT) ? Tab.ACTIONS : Tab.SCRIPT;
            this.rebuildUI();
        }).dimensions(leftOffset, 10, btnWidth * 2, 20).build());

        switch (this.currentTab) {
            case SCRIPT:
                // Mode Switcher Button
                this.addDrawableChild(ButtonWidget.builder(Text.literal(this.editingDefault ? "Mode: DEFAULT" : "Mode: CORRECT"), b -> {
                    UUID currentUuid = this.editingDefault ? this.defaultEntityUuid : this.correctEntityUuid;
                    ScriptActorManager.getInstance().saveBuilder(currentUuid, this.builder);

                    this.editingDefault = !this.editingDefault;
                    this.loadCurrentBuilder();
                    this.rebuildUI();
                }).dimensions(leftOffset, 35, btnWidth * 2, 20).build());

                initScriptTab(leftOffset, btnWidth);
                break;
            case ACTIONS:
                initActionsTab();
                break;
        }
    }

    private void initScriptTab(int leftOffset, int btnWidth) {
        int btnY = 60;
        int toggleWidth = 30;
        int toggleOffset = leftOffset + btnWidth + 5;
        ScriptingConfigManager.ScriptingConfig config = ScriptingConfigManager.getInstance().getConfig(this.entityUuid);

        // VAR
        this.addDrawableChild(ButtonWidget.builder(Text.literal("VAR"), b -> {
            this.builder.AddMath();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY, btnWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowVar ? "ON" : "OFF"), b -> {
            config.allowVar = !config.allowVar;
            this.rebuildUI();
        }).dimensions(toggleOffset, btnY, toggleWidth, 20).build());
        btnY += 25;

        // VARF
        this.addDrawableChild(ButtonWidget.builder(Text.literal("VARF"), b -> {
            this.builder.AddVarAssign();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY, btnWidth, 20).build());
        btnY += 25;

        // IF
        this.addDrawableChild(ButtonWidget.builder(Text.literal("IF"), b -> {
            this.builder.AddIf();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY, btnWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowIf ? "ON" : "OFF"), b -> {
            config.allowIf = !config.allowIf;
            this.rebuildUI();
        }).dimensions(toggleOffset, btnY, toggleWidth, 20).build());
        btnY += 25;

        // ELSE
        this.addDrawableChild(ButtonWidget.builder(Text.literal("ELSE"), b -> {
            this.builder.AddElse();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY, btnWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowElse ? "ON" : "OFF"), b -> {
            config.allowElse = !config.allowElse;
            this.rebuildUI();
        }).dimensions(toggleOffset, btnY, toggleWidth, 20).build());
        btnY += 25;

        // WHILE
        this.addDrawableChild(ButtonWidget.builder(Text.literal("WHILE"), b -> {
            this.builder.AddWhile();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY, btnWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowWhile ? "ON" : "OFF"), b -> {
            config.allowWhile = !config.allowWhile;
            this.rebuildUI();
        }).dimensions(toggleOffset, btnY, toggleWidth, 20).build());
        btnY += 25;

        // PRINT
        this.addDrawableChild(ButtonWidget.builder(Text.literal("PRINT"), b -> {
            this.builder.AddPrint();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY, btnWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowPrint ? "ON" : "OFF"), b -> {
            config.allowPrint = !config.allowPrint;
            this.rebuildUI();
        }).dimensions(toggleOffset, btnY, toggleWidth, 20).build());
        btnY += 25;

        // Follow Entity
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Follow"), b -> {
            this.builder.AddFollowEntity();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY, btnWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowFollow ? "ON" : "OFF"), b -> {
            config.allowFollow = !config.allowFollow;
            this.rebuildUI();
        }).dimensions(toggleOffset, btnY, toggleWidth, 20).build());
        btnY += 25;

        // Place Block
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Place"), b -> {
            this.builder.AddPlaceBlock();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY, btnWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowPlace ? "ON" : "OFF"), b -> {
            config.allowPlace = !config.allowPlace;
            this.rebuildUI();
        }).dimensions(toggleOffset, btnY, toggleWidth, 20).build());
        btnY += 25;

        // Command
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Command"), b -> {
            this.builder.AddCommand();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY, btnWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowCommand ? "ON" : "OFF"), b -> {
            config.allowCommand = !config.allowCommand;
            this.rebuildUI();
        }).dimensions(toggleOffset, btnY, toggleWidth, 20).build());
        btnY += 25;

        // Exec controls
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Exe 1"), b -> {
            NbtCompound scriptNbt = this.builder.toNbt();
            ClientPlayNetworking.send(new ExecuteOncePayload(this.entityUuid, scriptNbt));
            this.close();
        }).dimensions(leftOffset, btnY, btnWidth, 20).build());
        btnY += 25;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Start"), b -> {
            NbtCompound scriptNbt = this.builder.toNbt();
            ClientPlayNetworking.send(new SetTickingPayload(this.entityUuid, true, scriptNbt));
            this.close();
        }).dimensions(leftOffset, btnY, btnWidth, 20).build());
        btnY += 25;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Stop"), b -> {
            ClientPlayNetworking.send(new SetTickingPayload(this.entityUuid, false, new NbtCompound()));
            this.close();
        }).dimensions(leftOffset, btnY, btnWidth, 20).build());

        // UUID stuff
        List<String> entityUUIDs = this.scanInventoryForEntityUUIDs();
        int listX = this.width - 110;
        int listY = 40;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("UUIDs in Inventory"), b -> {

        }).dimensions(listX, 20, 100, 20).build()).active = false;

        for (String uuidStr : entityUUIDs) {
            String labelText;
            Entity foundEntity = this.findEntityByUUID(uuidStr);

            if (foundEntity != null) {
                String type = foundEntity.getType().getName().getString();
                labelText = foundEntity.hasCustomName() ? Objects.requireNonNull(foundEntity.getCustomName()).getString() + " (" + type + ")" : type + " " + uuidStr.substring(0, 4) + "..";
            } else {
                labelText = "Unknown " + uuidStr.substring(0, 8) + "..";
            }

            this.addDrawableChild(ButtonWidget.builder(Text.literal(labelText), b -> this.insertUUID(uuidStr))
                    .dimensions(listX, listY, 100, 20).build());
            listY += 25;
        }

        int finalY = this.generateWidgetsRecursive(this.builder.GetScript(), START_Y, 0);
        this.contentHeight = finalY - START_Y;
        this.updateScroll();
    }

    private void initActionsTab() {
        int startY = 70;
        int gapY = 90;

        // Draw the UI rows for each event
        createActionRow("Any Execute", entityActions.anyExecute, startY);
        createActionRow("Correct Code", entityActions.executeCorrect, startY + gapY);
        createActionRow("Wrong Code", entityActions.executeWrong, startY + gapY * 2);
    }

    private void createActionRow(String label, ScriptingConfigManager.ActionEventData data, int y) {
        int labelX = SCRIPT_X;

        // Multi-line Commands Input
        EditBoxWidget cmdBox = new EditBoxWidget(this.textRenderer, labelX, y + 15, 200, 60, Text.empty(), Text.literal("Commands"));
        cmdBox.setText(data.commands);
        cmdBox.setChangeListener(text -> data.commands = text);
        this.addDrawableChild(cmdBox);

        // Max Executions Input
        TextFieldWidget maxField = new TextFieldWidget(this.textRenderer, labelX + 210, y + 15, 40, 20, Text.literal(""));
        maxField.setText(String.valueOf(data.maxExecutions));
        maxField.setChangedListener(text -> {
            try { data.maxExecutions = Integer.parseInt(text); }
            catch (NumberFormatException ignored) {}
        });
        this.addDrawableChild(maxField);

        // Reset Counter Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset Cnt: " + data.currentExecutions), b -> {
            data.currentExecutions = 0;
            this.rebuildUI();
        }).dimensions(labelX + 260, y + 15, 80, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        switch (this.currentTab) {
            // Render the script highlighting/text specifically for the Script tab
            case SCRIPT -> {
                int drawY = (int) (START_Y - scrollOffset);
                int rightBound = this.width - 110;

                drawLineNumbersRecursive(context, this.builder.GetScript(), drawY, new int[]{1});
                context.enableScissor(SCRIPT_X, 0, rightBound, this.height);
                drawSelectionHighlight(context, this.builder.GetScript(), drawY, 0);
                drawIndentationLines(context, this.builder.GetScript(), drawY, 0);
                drawScriptTextRecursive(context, this.builder.GetScript(), drawY, 0);
                context.disableScissor();
            }
            // Render labels for the Actions tab
            case null, default -> {
                int startY = 70;
                int gapY = 90;

                context.drawTextWithShadow(this.textRenderer, "Event: Any Execute", SCRIPT_X, startY, 0xFFFFFF);
                context.drawTextWithShadow(this.textRenderer, "Event: Execute Correct", SCRIPT_X, startY + gapY, 0x55FF55);
                context.drawTextWithShadow(this.textRenderer, "Event: Execute Wrong", SCRIPT_X, startY + gapY * 2, 0xFF5555);

                context.drawTextWithShadow(this.textRenderer, "Cmds (; separated)", SCRIPT_X, startY - 10, 0xAAAAAA);
                context.drawTextWithShadow(this.textRenderer, "Max", SCRIPT_X + 210, startY - 10, 0xAAAAAA);
            }
        }
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

    private void exportNpcToClipboard() {
        NbtCompound root = new NbtCompound();

        ScriptBuilder current = ScriptActorManager.getInstance().getBuilder(this.entityUuid);
        ScriptBuilder correct = ScriptActorManager.getInstance().getBuilder(this.correctEntityUuid);
        ScriptBuilder def = ScriptActorManager.getInstance().getBuilder(this.defaultEntityUuid);

        if (current != null) root.put("script_current", current.toNbt());
        if (correct != null) root.put("script_correct", correct.toNbt());
        if (def != null) root.put("script_default", def.toNbt());

        root.put("meta", ScriptingConfigManager.getInstance().exportAllToNbt(this.entityUuid));

        String nbtString = root.toString();
        String command = "/scriptsummon " + this.entity.getType().getUntranslatedName() + " " + nbtString;

        MinecraftClient.getInstance().keyboard.setClipboard(command);
        if (this.client != null && this.client.player != null) {
            this.client.player.sendMessage(Text.literal("Command copied to clipboard!").formatted(Formatting.GREEN), true);
        }
    }
}