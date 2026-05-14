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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ScriptingDebugScreen extends ScriptingScreen {

    private final UUID correctEntityUuid;
    private boolean editingDefault = false;

    private enum TabDebug {
        SCRIPT("Script Editor"),
        ACTIONS("Event Actions"),
        VARIABLES("Persistent Variables"),
        TEST_CASES("Test Cases"),
        CHECKPOINTS("Checkpoints");

        private final String displayName;
        TabDebug(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
    private TabDebug currentTab = TabDebug.SCRIPT;

    private final ScriptingConfigManager.EntityActions entityActions;

    private int currentTestCaseIndex = 0;

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
        int btnHeight = 16;
        int btnSpacing = 4;

        // Export Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("EXPORT CMD"), b -> {
            exportNpcToClipboard();
        }).dimensions(leftOffset, this.height - btnHeight - 5, btnWidth, btnHeight).build());

        // Tab Switcher Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Tab: " + currentTab.name()), b -> {
            if (this.currentTab == TabDebug.SCRIPT) this.currentTab = TabDebug.ACTIONS;
            else if (this.currentTab == TabDebug.ACTIONS) this.currentTab = TabDebug.VARIABLES;
            else if (this.currentTab == TabDebug.VARIABLES) this.currentTab = TabDebug.TEST_CASES;
            else if (this.currentTab == TabDebug.TEST_CASES) this.currentTab = TabDebug.CHECKPOINTS;
            else this.currentTab = TabDebug.SCRIPT;

            if (this.currentTab == TabDebug.SCRIPT) this.currentView = Tab.SCRIPT;
            else if (this.currentTab == TabDebug.VARIABLES) this.currentView = Tab.VARIABLES;
            else this.currentView = Tab.OTHER;

            this.rebuildUI();
        }).dimensions(leftOffset, 10, btnWidth + 35, btnHeight).build());

        switch (this.currentTab) {
            case SCRIPT:
                // Mode Switcher Button
                this.addDrawableChild(ButtonWidget.builder(Text.literal(this.editingDefault ? "Mode: DEFAULT" : "Mode: CORRECT"), b -> {
                    UUID currentUuid = this.editingDefault ? this.defaultEntityUuid : this.correctEntityUuid;
                    ScriptActorManager.getInstance().saveBuilder(currentUuid, this.builder);

                    this.editingDefault = !this.editingDefault;
                    this.loadCurrentBuilder();
                    this.rebuildUI();
                }).dimensions(leftOffset, 10 + btnHeight + btnSpacing, btnWidth + 35, btnHeight).build());

                initScriptDebugTab(leftOffset, btnWidth, btnHeight, btnSpacing);
                break;
            case ACTIONS:
                initActionsTab();
                break;
            case VARIABLES:
                initVariablesTab();
                break;
            case TEST_CASES:
                initTestCasesTab();
                break;
            case CHECKPOINTS:
                initCheckpointsTab();
                break;
        }
    }

    protected void initScriptDebugTab(int leftOffset, int btnWidth, int btnHeight, int btnSpacing) {
        int toggleWidth = 30;
        int toggleOffset = leftOffset + btnWidth + 5;

        // Start Y position below the Tab and Mode buttons
        int btnY = 10 + (btnHeight + btnSpacing) * 2 + 10;

        ScriptingConfigManager.ScriptingConfig config = ScriptingConfigManager.getInstance().getConfig(this.entityUuid);

        // Category Switcher
        int catBtnTotalWidth = btnWidth + toggleWidth + 5;
        int arrowW = 12;
        int labelW = catBtnTotalWidth - (arrowW * 2);

        // Left Arrow
        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            InstructionCategory[] cats = InstructionCategory.values();
            this.currentCategory = cats[(this.currentCategory.ordinal() - 1 + cats.length) % cats.length];
            this.rebuildUI();
        }).dimensions(leftOffset, btnY, arrowW, btnHeight + 4).build());

        // Middle Category Label (Inactive Button)
        ButtonWidget catLabel = ButtonWidget.builder(Text.literal(currentCategory.name()), b -> {})
                .dimensions(leftOffset + arrowW, btnY, labelW, btnHeight + 4).build();
        catLabel.active = false;
        this.addDrawableChild(catLabel);

        // Right Arrow
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            InstructionCategory[] cats = InstructionCategory.values();
            this.currentCategory = cats[(this.currentCategory.ordinal() + 1) % cats.length];
            this.rebuildUI();
        }).dimensions(leftOffset + arrowW + labelW, btnY, arrowW, btnHeight + 4).build());
        btnY += btnHeight + btnSpacing + 4;

        switch (this.currentCategory) {
            case BASIC -> {
                // VAR
                this.addDrawableChild(ButtonWidget.builder(Text.literal("VAR"), b -> {
                    this.builder.AddMath();
                    this.rebuildUI();
                }).dimensions(leftOffset, btnY, btnWidth, btnHeight).build());
                this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowVar ? "ON" : "OFF"), b -> {
                    config.allowVar = !config.allowVar;
                    ScriptingConfigManager.getInstance().markDirty();
                    this.rebuildUI();
                }).dimensions(toggleOffset, btnY, toggleWidth, btnHeight).build());
                btnY += btnHeight + btnSpacing;

                // VARF
                this.addDrawableChild(ButtonWidget.builder(Text.literal("VARF"), b -> {
                    this.builder.AddVarAssign();
                    this.rebuildUI();
                }).dimensions(leftOffset, btnY, btnWidth, btnHeight).build());
                btnY += btnHeight + btnSpacing; // VAR and VARF share the config toggle visually

                // PRINT
                this.addDrawableChild(ButtonWidget.builder(Text.literal("PRINT"), b -> {
                    this.builder.AddPrint();
                    this.rebuildUI();
                }).dimensions(leftOffset, btnY, btnWidth, btnHeight).build());
                this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowPrint ? "ON" : "OFF"), b -> {
                    config.allowPrint = !config.allowPrint;
                    ScriptingConfigManager.getInstance().markDirty();
                    this.rebuildUI();
                }).dimensions(toggleOffset, btnY, toggleWidth, btnHeight).build());
                btnY += btnHeight + btnSpacing;

                // IF
                this.addDrawableChild(ButtonWidget.builder(Text.literal("IF"), b -> {
                    this.builder.AddIf();
                    this.rebuildUI();
                }).dimensions(leftOffset, btnY, btnWidth, btnHeight).build());
                this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowIf ? "ON" : "OFF"), b -> {
                    config.allowIf = !config.allowIf;
                    ScriptingConfigManager.getInstance().markDirty();
                    this.rebuildUI();
                }).dimensions(toggleOffset, btnY, toggleWidth, btnHeight).build());
                btnY += btnHeight + btnSpacing;

                // ELSE
                this.addDrawableChild(ButtonWidget.builder(Text.literal("ELSE"), b -> {
                    this.builder.AddElse();
                    this.rebuildUI();
                }).dimensions(leftOffset, btnY, btnWidth, btnHeight).build());
                this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowElse ? "ON" : "OFF"), b -> {
                    config.allowElse = !config.allowElse;
                    ScriptingConfigManager.getInstance().markDirty();
                    this.rebuildUI();
                }).dimensions(toggleOffset, btnY, toggleWidth, btnHeight).build());
                btnY += btnHeight + btnSpacing;

                // WHILE
                this.addDrawableChild(ButtonWidget.builder(Text.literal("WHILE"), b -> {
                    this.builder.AddWhile();
                    this.rebuildUI();
                }).dimensions(leftOffset, btnY, btnWidth, btnHeight).build());
                this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowWhile ? "ON" : "OFF"), b -> {
                    config.allowWhile = !config.allowWhile;
                    ScriptingConfigManager.getInstance().markDirty();
                    this.rebuildUI();
                }).dimensions(toggleOffset, btnY, toggleWidth, btnHeight).build());
                btnY += btnHeight + btnSpacing;
            }
            case ENTITY -> {

                // Look At
                this.addDrawableChild(ButtonWidget.builder(Text.literal("Look At"), b -> {
                    this.builder.AddLookAtEntity();
                    this.rebuildUI();
                }).dimensions(leftOffset, btnY, btnWidth, btnHeight).build());
                this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowLookAt ? "ON" : "OFF"), b -> {
                    config.allowLookAt = !config.allowLookAt;
                    ScriptingConfigManager.getInstance().markDirty();
                    this.rebuildUI();
                }).dimensions(toggleOffset, btnY, toggleWidth, btnHeight).build());
                btnY += btnHeight + btnSpacing;

                // Follow Entity
                this.addDrawableChild(ButtonWidget.builder(Text.literal("Follow"), b -> {
                    this.builder.AddFollowEntity();
                    this.rebuildUI();
                }).dimensions(leftOffset, btnY, btnWidth, btnHeight).build());
                this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowFollow ? "ON" : "OFF"), b -> {
                    config.allowFollow = !config.allowFollow;
                    ScriptingConfigManager.getInstance().markDirty();
                    this.rebuildUI();
                }).dimensions(toggleOffset, btnY, toggleWidth, btnHeight).build());
                btnY += btnHeight + btnSpacing;

                // Walk Towards
                this.addDrawableChild(ButtonWidget.builder(Text.literal("WalkTo"), b -> {
                    this.builder.AddWalkTowards();
                    this.rebuildUI();
                }).dimensions(leftOffset, btnY, btnWidth, btnHeight).build());
                this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowWalkForward ? "ON" : "OFF"), b -> {
                    config.allowWalkForward = !config.allowWalkForward;
                    ScriptingConfigManager.getInstance().markDirty();
                    this.rebuildUI();
                }).dimensions(toggleOffset, btnY, toggleWidth, btnHeight).build());
                btnY += btnHeight + btnSpacing;

                // Walk Forward
                this.addDrawableChild(ButtonWidget.builder(Text.literal("WalkFwd"), b -> {
                    this.builder.AddWalkForward();
                    this.rebuildUI();
                }).dimensions(leftOffset, btnY, btnWidth, btnHeight).build());
                btnY += btnHeight + btnSpacing; // Shares the config visual switch with Walk Towards

            }
            case MINECRAFT -> {
                // Distance Check
                this.addDrawableChild(ButtonWidget.builder(Text.literal("Dist to Ent"), b -> {
                    this.builder.AddDistanceFromEntity();
                    this.rebuildUI();
                }).dimensions(leftOffset, btnY, btnWidth, btnHeight).build());
                this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowDistanceCheck ? "ON" : "OFF"), b -> {
                    config.allowDistanceCheck = !config.allowDistanceCheck;
                    ScriptingConfigManager.getInstance().markDirty();
                    this.rebuildUI();
                }).dimensions(toggleOffset, btnY, toggleWidth, btnHeight).build());
                btnY += btnHeight + btnSpacing;
                this.addDrawableChild(ButtonWidget.builder(Text.literal("Dist to Pos"), b -> {
                    this.builder.AddDistanceFromPosition();
                    this.rebuildUI();
                }).dimensions(leftOffset, btnY, btnWidth, btnHeight).build());
                btnY += btnHeight + btnSpacing;

                // Place Block
                this.addDrawableChild(ButtonWidget.builder(Text.literal("Place"), b -> {
                    this.builder.AddPlaceBlock();
                    this.rebuildUI();
                }).dimensions(leftOffset, btnY, btnWidth, btnHeight).build());
                this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowPlace ? "ON" : "OFF"), b -> {
                    config.allowPlace = !config.allowPlace;
                    ScriptingConfigManager.getInstance().markDirty();
                    this.rebuildUI();
                }).dimensions(toggleOffset, btnY, toggleWidth, btnHeight).build());
                btnY += btnHeight + btnSpacing;

                // Command
                this.addDrawableChild(ButtonWidget.builder(Text.literal("Command"), b -> {
                    this.builder.AddCommand();
                    this.rebuildUI();
                }).dimensions(leftOffset, btnY, btnWidth, btnHeight).build());
                this.addDrawableChild(ButtonWidget.builder(Text.literal(config.allowCommand ? "ON" : "OFF"), b -> {
                    config.allowCommand = !config.allowCommand;
                    ScriptingConfigManager.getInstance().markDirty();
                    this.rebuildUI();
                }).dimensions(toggleOffset, btnY, toggleWidth, btnHeight).build());
                btnY += btnHeight + btnSpacing;
            }
            case null, default -> {
            }
        }

        // ################### BOTTOM BUTTONS
        // Start above the EXPORT CMD button
        int execY = this.height - btnHeight - 5 - btnHeight - btnSpacing;

        // STOP
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Stop"), b -> {
            ClientPlayNetworking.send(new SetTickingPayload(this.entityUuid, false, new NbtCompound()));
            this.close();
        }).dimensions(leftOffset, execY, btnWidth, btnHeight).build());
        execY -= btnHeight; // Stacking upwards

        // START
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Start"), b -> {
            NbtCompound scriptNbt = this.builder.toNbt();
            ClientPlayNetworking.send(new SetTickingPayload(this.entityUuid, true, scriptNbt));
            this.close();
        }).dimensions(leftOffset, execY, btnWidth, btnHeight).build());
        execY -= btnHeight;

        // EXE 1
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Exe 1"), b -> {
            NbtCompound scriptNbt = this.builder.toNbt();
            ClientPlayNetworking.send(new ExecuteOncePayload(this.entityUuid, scriptNbt));
            this.close();
        }).dimensions(leftOffset, execY, btnWidth, btnHeight).build());
        execY -= btnHeight;

        // UUID stuff
        List<String> entityUUIDs = this.scanInventoryForEntityUUIDs();
        int listX = this.width - 110;
        int listY = 40;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("UUIDs in Inventory"), b -> {

        }).dimensions(listX, 20, 100, btnHeight).build()).active = false;

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
                    .dimensions(listX, listY, 100, btnHeight).build());
            listY += btnHeight + btnSpacing;
        }

        // Reset to Default (Bottom Right)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset to Default"), b -> {
            this.resetToDefaultScript();
            this.rebuildUI();
        }).dimensions(this.width - 90, this.height - btnHeight - 5, 80, btnHeight).build());

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

    private void initTestCasesTab() {
        ScriptingConfigManager.ScriptingConfig config = ScriptingConfigManager.getInstance().getConfig(this.entityUuid);
        if (config.testCases.isEmpty()) {
            config.testCases.add(new ScriptingConfigManager.TestCase());
        }
        if (config.testCases.size() <= currentTestCaseIndex) currentTestCaseIndex = Math.max(0, config.testCases.size() - 1);

        int listY = 70;

        // Pagination for Test Cases
        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            if (0 < currentTestCaseIndex) currentTestCaseIndex--;
            this.rebuildUI();
        }).dimensions(SCRIPT_X, listY, 20, 20).build());

        ButtonWidget caseLabel = ButtonWidget.builder(Text.literal("Case " + (currentTestCaseIndex + 1) + "/" + config.testCases.size()), b -> {}).dimensions(SCRIPT_X + 20, listY, 60, 20).build();
        caseLabel.active = false;
        this.addDrawableChild(caseLabel);

        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            if (currentTestCaseIndex < config.testCases.size() - 1) currentTestCaseIndex++;
            this.rebuildUI();
        }).dimensions(SCRIPT_X + 80, listY, 20, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("+ Case"), b -> {
            config.testCases.add(new ScriptingConfigManager.TestCase());
            currentTestCaseIndex = config.testCases.size() - 1;
            ScriptingConfigManager.getInstance().markDirty();
            this.rebuildUI();
        }).dimensions(SCRIPT_X + 110, listY, 50, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("- Case"), b -> {
            if (1 < config.testCases.size()) {
                config.testCases.remove(currentTestCaseIndex);
                if (config.testCases.size() <= currentTestCaseIndex) currentTestCaseIndex--;
                ScriptingConfigManager.getInstance().markDirty();
                this.rebuildUI();
            }
        }).dimensions(SCRIPT_X + 165, listY, 50, 20).build());

        listY += 30;

        ScriptingConfigManager.TestCase currentCase = config.testCases.get(currentTestCaseIndex);

        for (int i = 0; i < currentCase.variables.size(); i++) {
            ScriptingConfigManager.PersistentVariable pv = currentCase.variables.get(i);
            int finalI = i;

            TextFieldWidget nameField = new TextFieldWidget(this.textRenderer, SCRIPT_X, listY, 80, 20, Text.literal(""));
            nameField.setMaxLength(1024);
            nameField.setText(pv.name);
            nameField.setChangedListener(text -> { pv.name = text; ScriptingConfigManager.getInstance().markDirty(); });
            addScrollableChild(nameField, SCRIPT_X, listY);

            TextFieldWidget valField = new TextFieldWidget(this.textRenderer, SCRIPT_X + 95, listY, 150, 20, Text.literal(""));
            valField.setMaxLength(1024);
            valField.setText(pv.value);
            valField.setChangedListener(text -> { pv.value = text; ScriptingConfigManager.getInstance().markDirty(); });
            addScrollableChild(valField, SCRIPT_X + 95, listY);

            addScrollableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                currentCase.variables.remove(finalI);
                ScriptingConfigManager.getInstance().markDirty();
                this.rebuildUI();
            }).dimensions(SCRIPT_X + 250, listY, 20, 20).build(), SCRIPT_X + 250, listY);

            listY += LINE_HEIGHT;
        }

        addScrollableChild(ButtonWidget.builder(Text.literal("+ Add Start Var"), b -> {
            currentCase.variables.add(new ScriptingConfigManager.PersistentVariable());
            ScriptingConfigManager.getInstance().markDirty();
            this.rebuildUI();
        }).dimensions(SCRIPT_X, listY, 100, 20).build(), SCRIPT_X, listY);

        this.contentHeight = listY - START_Y + 40;
    }

    private void initCheckpointsTab() {
        int listY = 70;
        ScriptingConfigManager.ScriptingConfig config = ScriptingConfigManager.getInstance().getConfig(this.entityUuid);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("+ Add Checkpoint"), b -> {
            config.checkpoints.add(new ScriptingConfigManager.CheckpointData());
            ScriptingConfigManager.getInstance().markDirty();
            this.rebuildUI();
        }).dimensions(SCRIPT_X, 40, 120, 20).build());

        for (int i = 0; i < config.checkpoints.size(); i++) {
            ScriptingConfigManager.CheckpointData cp = config.checkpoints.get(i);
            int finalI = i;
            int currentX = SCRIPT_X;
            int fieldW = 50;

            TextFieldWidget xF = new TextFieldWidget(this.textRenderer, currentX, listY, fieldW, 20, Text.literal(""));
            xF.setText(String.valueOf(cp.x));
            xF.setChangedListener(text -> { try { cp.x = Double.parseDouble(text); ScriptingConfigManager.getInstance().markDirty(); } catch(Exception ignored){} });
            this.addDrawableChild(xF);
            currentX += fieldW + 5;

            TextFieldWidget yF = new TextFieldWidget(this.textRenderer, currentX, listY, fieldW, 20, Text.literal(""));
            yF.setText(String.valueOf(cp.y));
            yF.setChangedListener(text -> { try { cp.y = Double.parseDouble(text); ScriptingConfigManager.getInstance().markDirty(); } catch(Exception ignored){} });
            this.addDrawableChild(yF);
            currentX += fieldW + 5;

            TextFieldWidget zF = new TextFieldWidget(this.textRenderer, currentX, listY, fieldW, 20, Text.literal(""));
            zF.setText(String.valueOf(cp.z));
            zF.setChangedListener(text -> { try { cp.z = Double.parseDouble(text); ScriptingConfigManager.getInstance().markDirty(); } catch(Exception ignored){} });
            this.addDrawableChild(zF);
            currentX += fieldW + 10;

            this.addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                config.checkpoints.remove(finalI);
                ScriptingConfigManager.getInstance().markDirty();
                this.rebuildUI();
            }).dimensions(currentX, listY, 20, 20).build());

            listY += 25;
        }
        this.contentHeight = listY - START_Y + 40;
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
        // This will automatically handle SCRIPT and VARIABLES
        super.render(context, mouseX, mouseY, delta);

        // We only need to add the extra rendering for ACTIONS and CHECKPOINTS
        switch (this.currentTab) {
            case ACTIONS -> {
                int startY = 70;
                int gapY = 90;

                context.drawTextWithShadow(this.textRenderer, "Event: Any Execute", SCRIPT_X, startY, 0xFFFFFF);
                context.drawTextWithShadow(this.textRenderer, "Event: Execute Correct", SCRIPT_X, startY + gapY, 0x55FF55);
                context.drawTextWithShadow(this.textRenderer, "Event: Execute Wrong", SCRIPT_X, startY + gapY * 2, 0xFF5555);

                context.drawTextWithShadow(this.textRenderer, "Cmds (; separated)", SCRIPT_X, startY - 10, 0xAAAAAA);
                context.drawTextWithShadow(this.textRenderer, "Max", SCRIPT_X + 210, startY - 10, 0xAAAAAA);
            }
            case CHECKPOINTS -> {
                context.drawTextWithShadow(this.textRenderer, "X", SCRIPT_X + 20, 60, 0xAAAAAA);
                context.drawTextWithShadow(this.textRenderer, "Y", SCRIPT_X + 75, 60, 0xAAAAAA);
                context.drawTextWithShadow(this.textRenderer, "Z", SCRIPT_X + 130, 60, 0xAAAAAA);

                for (PlacedWidget pw : scrollableWidgets) {
                    if (pw.widget().visible) pw.widget().render(context, mouseX, mouseY, delta);
                }
            }
            case TEST_CASES -> {
                int drawY = (int) (START_Y - scrollOffset);
                int rightBound = this.width - 110;
                context.enableScissor(SCRIPT_X, 0, rightBound, this.height);

                ScriptingConfigManager.ScriptingConfig config = ScriptingConfigManager.getInstance().getConfig(this.entityUuid);

                if (!config.testCases.isEmpty() && currentTestCaseIndex < config.testCases.size()) {
                    int listY = drawY + 80;
                    ScriptingConfigManager.TestCase currentCase = config.testCases.get(currentTestCaseIndex);

                    for (ScriptingConfigManager.PersistentVariable pv : currentCase.variables) {
                        context.drawTextWithShadow(this.textRenderer, "=", SCRIPT_X + 85, listY + 6, 0xFFFFFF);
                        listY += LINE_HEIGHT;
                    }
                }

                for (PlacedWidget pw : scrollableWidgets) {
                    if (pw.widget().visible) pw.widget().render(context, mouseX, mouseY, delta);
                }
                context.disableScissor();
            }
            default -> {}
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