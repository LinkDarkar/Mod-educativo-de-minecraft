package net.linkdarkar.testmod.screen.custom;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.linkdarkar.testmod.networking.ModNetworking;
import net.linkdarkar.testmod.scripting.*;
import net.linkdarkar.testmod.scripting.enums.ComparisonOperator;
import net.linkdarkar.testmod.scripting.instructions.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.linkdarkar.testmod.networking.ModNetworking.ExecuteOncePayload;
import net.linkdarkar.testmod.networking.ModNetworking.SetTickingPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ScriptingScreen extends Screen {
    private final UUID entityUuid;
    private final LivingEntity entity;
    public ScriptBuilder builder;

    private final int START_Y = 20;
    private final int LINE_HEIGHT = 25;
    private final int SCRIPT_X = 140;

    private double scrollOffset = 0;
    private int contentHeight = 0;

    private final List<PlacedWidget> scrollableWidgets = new ArrayList<>();

    private record PlacedWidget(ClickableWidget widget, int originalY) {}

    public ScriptingScreen(LivingEntity entity) {
        super(Text.literal("opens scripting screen"));
        this.entity = entity;
        this.entityUuid = entity.getUuid();

        ScriptBuilder existing = ScriptActorManager.getInstance().getBuilder(entityUuid);
        if (existing != null) {
            this.builder = existing;
        } else {
            this.builder = new ScriptBuilder(entity);
            ScriptActorManager.getInstance().saveBuilder(this.entityUuid, this.builder);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.scrollableWidgets.clear();

        // Add Control Buttons (Left Side)
        int btnY = 50;
        int btnWidth = 60;
        int leftOffset = 10;

        // NEW VAR
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add VAR"), b -> {
            this.builder.AddMath();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY, btnWidth, 20).build());

        // ADD IF
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add IF"), b -> {
            this.builder.AddIf();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY + 25, btnWidth, 20).build());

        // ADD WHILE
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add WHILE"), b -> {
            this.builder.AddWhile();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY + 50, btnWidth, 20).build());

        // ADD PRINT (to chat)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("PRINT"), b -> {
            this.builder.AddPrint();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY + 75, btnWidth, 20).build());

        // ADD FOLLOW_ENTITY
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Follow Entity"), b -> {
            this.builder.AddFollowEntity();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY + 100, btnWidth, 20).build());

        // ADD PLACE_BLOCK
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Place Block"), b -> {
            this.builder.AddPlaceBlock();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY + 125, btnWidth, 20).build());

        // EXECUTE ONCE
        this.addDrawableChild(ButtonWidget.builder(Text.literal("EXECUTE ONCE"), b -> {
            NbtCompound scriptNbt = this.builder.toNbt();

            // New way: Send the object directly
            ClientPlayNetworking.send(new ExecuteOncePayload(this.entityUuid, scriptNbt));

            this.close();
        }).dimensions(leftOffset, btnY + 150, btnWidth, 20).build());

        // START LOOP
        this.addDrawableChild(ButtonWidget.builder(Text.literal("START LOOP"), b -> {
            NbtCompound scriptNbt = this.builder.toNbt();

            // New way: Send the object directly
            ClientPlayNetworking.send(new SetTickingPayload(this.entityUuid, true, scriptNbt));

            this.close();
        }).dimensions(leftOffset, btnY + 175, btnWidth, 20).build());

        // STOP LOOP
        this.addDrawableChild(ButtonWidget.builder(Text.literal("STOP LOOP"), b -> {
            // New way: Send the object directly (sending empty NBT since we are stopping)
            ClientPlayNetworking.send(new SetTickingPayload(this.entityUuid, false, new NbtCompound()));

            this.close();
        }).dimensions(leftOffset, btnY + 200, btnWidth, 20).build());

        // UUID stuff
        // ------------------------------

        List<String> entityUUIDs = scanInventoryForEntityUUIDs();

        int listX = this.width - 110;
        int listY = 40;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("UUIDs in Inventory"), b -> {})
                .dimensions(listX, 20, 100, 20).build()).active = false; // Inactive button acts as a label

        for (String uuidStr : entityUUIDs) {
            String labelText;
            Entity entity = findEntityByUUID(uuidStr);

            if (entity != null) {
                String type = entity.getType().getName().getString(); // e.g. "Sheep"

                if (entity.hasCustomName()) {
                    labelText = Objects.requireNonNull(entity.getCustomName()).getString() + " (" + type + ")";
                } else {
                    labelText = type + " " + uuidStr.substring(0, 4) + "..";
                }
            } else {
                labelText = "Unknown " + uuidStr.substring(0, 8) + "..";
            }

            // Create the button with the new label
            this.addDrawableChild(ButtonWidget.builder(Text.literal(labelText), b -> {
                insertUUID(uuidStr);
            }).dimensions(listX, listY, 100, 20).build());

            listY += 25;
        }

        int finalY = generateWidgetsRecursive(this.builder.GetScript(), START_Y, 0);

        this.contentHeight = finalY - START_Y;

        updateScroll();
    }
    private void rebuildUI() {
        this.clearChildren();
        this.init();
    }

    private ScriptLine findLineAt(int mouseY, ScriptBlock block, int currentY) {
        for (ScriptLine line : block.blockLines) {
            if (mouseY >= currentY && mouseY < currentY + LINE_HEIGHT) {
                return line;
            }
            currentY += LINE_HEIGHT;

            if (line instanceof InstructionIF) {
                ScriptLine foundInChild = findLineAt(mouseY, ((InstructionIF) line).trueBlock, currentY);
                if (foundInChild != null) return foundInChild;
                currentY += getBlockHeight(((InstructionIF) line).trueBlock);
            }
            else if (line instanceof InstructionWHILE) {
                ScriptLine foundInChild = findLineAt(mouseY, ((InstructionWHILE) line).loopBlock, currentY);
                if (foundInChild != null) return foundInChild;
                currentY += getBlockHeight(((InstructionWHILE) line).loopBlock);
            }
        }
        return null;
    }

    private ScriptBlock findParentOfLine(ScriptBlock scope, ScriptLine target) {
        if (scope.blockLines.contains(target)) return scope;

        for (ScriptLine line : scope.blockLines) {
            ScriptBlock child = null;
            if (line instanceof InstructionIF) child = ((InstructionIF) line).trueBlock;
            if (line instanceof InstructionWHILE) child = ((InstructionWHILE) line).loopBlock;

            if (child != null) {
                ScriptBlock res = findParentOfLine(child, target);
                if (res != null) return res;
            }
        }
        return null;
    }

    private int generateWidgetsRecursive(ScriptBlock block, int currentY, int indent) {
        int contentStartX = SCRIPT_X + (indent * 20);

        for (ScriptLine line : block.blockLines) {


            if (builder.selectedLine == line) {
                createSelectionButtons(currentY);
            }

            if (line instanceof InstructionIF ifLine) {
                createConditionWidgets(ifLine.condition, currentY, contentStartX);
                currentY += LINE_HEIGHT;
                currentY = generateWidgetsRecursive(ifLine.trueBlock, currentY, indent + 1);

            } else if (line instanceof InstructionWHILE whileLine) {
                createConditionWidgets(whileLine.condition, currentY, contentStartX);
                currentY += LINE_HEIGHT;
                currentY = generateWidgetsRecursive(whileLine.loopBlock, currentY, indent + 1);

            } else if (line instanceof InstructionMath mathLine) {
                createMathWidgets(mathLine, currentY, contentStartX);
                currentY += LINE_HEIGHT;
            } else if (line instanceof InstructionPrint printLine) {
                createPrintWidgets(printLine, currentY, contentStartX);
                currentY += LINE_HEIGHT;
            } else if (line instanceof InstructionEntity_FollowEntity followLine) {
                createFollowEntityWidgets(followLine, currentY, contentStartX);
                currentY += LINE_HEIGHT;
            } else if (line instanceof InstructionBlock_Place placeLine) {
                createPlaceBlockWidgets(placeLine, currentY, contentStartX);
                currentY += LINE_HEIGHT;
            }
        }
        return currentY;
    }

    private void createPrintWidgets(InstructionPrint line, int y, int startX) {
        TextFieldWidget msgField = new TextFieldWidget(this.textRenderer, startX + 40, y, 150, 20, Text.literal(""));
        msgField.setText(line.message);
        msgField.setChangedListener(text -> line.message = text);
        addScrollableChild(msgField, y);
    }

    private void createMathWidgets(InstructionMath line, int y, int startX) {
        // [Target] = [Expression]

        // Target Variable
        TextFieldWidget targetField = new TextFieldWidget(this.textRenderer, startX, y, 50, 20, Text.literal(""));
        targetField.setText(line.targetVarName);
        targetField.setChangedListener(text -> line.targetVarName = text);
        addScrollableChild(targetField, y);

        // Expression Field
        // Positioned after the "=" sign (approx +60px)
        TextFieldWidget exprField = new TextFieldWidget(this.textRenderer, startX + 65, y, 120, 20, Text.literal(""));
        exprField.setText(line.expression);
        exprField.setChangedListener(text -> line.expression = text);
        addScrollableChild(exprField, y);
    }

    private void createPlaceBlockWidgets(InstructionBlock_Place line, int y, int startX) {
        int fieldWidth = 35;
        int gap = 5;

        int currentX = startX + 40;

        // X Field
        TextFieldWidget xField = new TextFieldWidget(this.textRenderer, currentX, y, fieldWidth, 20, Text.literal(""));
        xField.setText(line.xExp);
        xField.setChangedListener(text -> line.xExp = text);
        addScrollableChild(xField, y);

        currentX += fieldWidth + gap;

        // Y Field
        TextFieldWidget yField = new TextFieldWidget(this.textRenderer, currentX, y, fieldWidth, 20, Text.literal(""));
        yField.setText(line.yExp);
        yField.setChangedListener(text -> line.yExp = text);
        addScrollableChild(yField, y);

        currentX += fieldWidth + gap;

        // Z Field
        TextFieldWidget zField = new TextFieldWidget(this.textRenderer, currentX, y, fieldWidth, 20, Text.literal(""));
        zField.setText(line.zExp);
        zField.setChangedListener(text -> line.zExp = text);
        addScrollableChild(zField, y);
    }

    private <T extends ClickableWidget> void addScrollableChild(T widget, int originalY) {
        this.addDrawableChild(widget);
        this.scrollableWidgets.add(new PlacedWidget(widget, originalY));
    }

    private void createSelectionButtons(int y) {
        int navX = 80;
        int navW = 20;

        // Left [←]
        addScrollableChild(ButtonWidget.builder(Text.literal("←"), b -> {
            builder.MoveLeft(); rebuildUI();
        }).dimensions(navX, y, navW, 20).build(), y);

        // Right [→]
        addScrollableChild(ButtonWidget.builder(Text.literal("→"), b -> {
            builder.MoveRight(); rebuildUI();
        }).dimensions(navX + 25, y, navW, 20).build(), y);

        int rightEdge = this.width - 10;

        // Delete [×]
        addScrollableChild(ButtonWidget.builder(Text.literal("×"), b -> {
            builder.DeleteSelected(); rebuildUI();
        }).dimensions(rightEdge - 20, y, 20, 20).build(), y);

        // Vertical Movement
        int moveX = rightEdge - 45;

        // Up [↑]
        addScrollableChild(ButtonWidget.builder(Text.literal("↑"), b -> {
            builder.MoveUp(); rebuildUI();
        }).dimensions(moveX, y, 20, 10).build(), y);

        // Down [↓]
        addScrollableChild(ButtonWidget.builder(Text.literal("↓"), b -> {
            builder.MoveDown(); rebuildUI();
        }).dimensions(moveX, y + 10, 20, 10).build(), y + 10);
    }

    private void createConditionWidgets(ScriptCondition cond, int y, int startX) {
        if (cond == null) return;
        final ScriptCondition finalCond = cond;

        // Left Expression Field (Slightly wider for math: 60px)
        TextFieldWidget leftField = new TextFieldWidget(this.textRenderer, startX + 40, y, 60, 20, Text.literal(""));
        leftField.setText(cond.leftExpression);
        leftField.setChangedListener(text -> finalCond.leftExpression = text);
        addScrollableChild(leftField, y);

        // Operator Button
        // Positioned after LeftField (40 + 60 + 5 padding = 105)
        ButtonWidget opButton = ButtonWidget.builder(Text.literal(getOpSymbol(cond.op)), button -> {
            finalCond.op = nextOperator(finalCond.op);
            button.setMessage(Text.literal(getOpSymbol(finalCond.op)));
        }).dimensions(startX + 105, y, 25, 20).build();
        addScrollableChild(opButton, y);

        // Right Expression Field
        // Positioned after OpButton (105 + 25 + 5 padding = 135)
        TextFieldWidget rightField = new TextFieldWidget(this.textRenderer, startX + 135, y, 60, 20, Text.literal(""));
        rightField.setText(cond.rightExpression);
        rightField.setChangedListener(text -> finalCond.rightExpression = text);
        addScrollableChild(rightField, y);
    }
    private void createMathSimpleWidgets(InstructionMathSimple line, int y, int startX) {
        TextFieldWidget targetField = new TextFieldWidget(this.textRenderer, startX, y, 60, 20, Text.literal(""));
        targetField.setText(line.targetVarName);
        targetField.setChangedListener(text -> line.targetVarName = text);
        addScrollableChild(targetField, y);

        TextFieldWidget valueField = new TextFieldWidget(this.textRenderer, startX + 80, y, 60, 20, Text.literal(""));
        valueField.setText(line.right.GetOriginalValue());
        valueField.setChangedListener(text -> line.right.UpdateValue(text));
        addScrollableChild(valueField, y);
    }
    private void createFollowEntityWidgets(InstructionEntity_FollowEntity line, int y, int startX) {
        int labelWidth = 85;

        TextFieldWidget targetField = new TextFieldWidget(this.textRenderer, startX + labelWidth, y, 100, 20, Text.literal(""));
        targetField.setText(line.targetUUID);
        targetField.setChangedListener(text -> line.targetUUID = text);
        addScrollableChild(targetField, y);
    }
    private ComparisonOperator nextOperator(ComparisonOperator current) {
        int nextOrdinal = (current.ordinal() + 1) % ComparisonOperator.values().length;
        return ComparisonOperator.values()[nextOrdinal];
    }
    private String getOpSymbol(ComparisonOperator op) {
        return switch (op) {
            case EQUALS -> "==";
            case DIFFERENT -> "!=";
            case LESS_THAN -> "<";
            case GREATER_THAN -> ">";
            default -> "?";
        };
    }

    private int getBlockHeight(ScriptBlock block) {
        int height = 0;
        for (ScriptLine line : block.blockLines) {
            height += LINE_HEIGHT;
            if (line instanceof InstructionIF) height += getBlockHeight(((InstructionIF) line).trueBlock);
            else if (line instanceof InstructionWHILE) height += getBlockHeight(((InstructionWHILE) line).loopBlock);
        }
        return height;
    }
    private int drawBlockBackgrounds(DrawContext context, ScriptBlock block, int currentY, int indent) {
        for (ScriptLine line : block.blockLines) {
            int xOffset = indent * 20;

            if (line instanceof InstructionIF ifLine) {
                int innerHeight = getBlockHeight(ifLine.trueBlock);
                int startX = SCRIPT_X + xOffset;

                int bgColor = 0x22FF0000;
                context.fill(startX + 10, currentY + LINE_HEIGHT, this.width, currentY + LINE_HEIGHT + innerHeight, bgColor);

                currentY += LINE_HEIGHT;
                currentY = drawBlockBackgrounds(context, ifLine.trueBlock, currentY, indent + 1);

            } else if (line instanceof InstructionWHILE whileLine) {
                int innerHeight = getBlockHeight(whileLine.loopBlock);
                int startX = SCRIPT_X + xOffset;

                int bgColor = 0x2200FF00;
                context.fill(startX + 10, currentY + LINE_HEIGHT, this.width, currentY + LINE_HEIGHT + innerHeight, bgColor);

                currentY += LINE_HEIGHT;
                currentY = drawBlockBackgrounds(context, whileLine.loopBlock, currentY, indent + 1);

            } else {
                currentY += LINE_HEIGHT;
            }
        }
        return currentY;
    }
    private int drawIndentationLines(DrawContext context, ScriptBlock block, int currentY, int indent) {
        for (ScriptLine line : block.blockLines) {
            int xOffset = indent * 20;

            if (line instanceof InstructionIF ifLine) {
                int innerHeight = getBlockHeight(ifLine.trueBlock);
                int startX = SCRIPT_X + xOffset;

                int bracketColor = 0xFFFF0000;
                context.fill(startX + 5, currentY + LINE_HEIGHT, startX + 7, currentY + LINE_HEIGHT + innerHeight, bracketColor);

                currentY += LINE_HEIGHT;
                currentY = drawIndentationLines(context, ifLine.trueBlock, currentY, indent + 1);

            } else if (line instanceof InstructionWHILE whileLine) {
                int innerHeight = getBlockHeight(whileLine.loopBlock);
                int startX = SCRIPT_X + xOffset;

                int bracketColor = 0xFF00FF00;
                context.fill(startX + 5, currentY + LINE_HEIGHT, startX + 7, currentY + LINE_HEIGHT + innerHeight, bracketColor);

                currentY += LINE_HEIGHT;
                currentY = drawIndentationLines(context, whileLine.loopBlock, currentY, indent + 1);

            } else {
                currentY += LINE_HEIGHT;
            }
        }
        return currentY;
    }

    private int drawLabelsRecursive(DrawContext context, ScriptBlock block, int currentY, int indent) {
        for (ScriptLine line : block.blockLines) {
            int xOffset = indent * 20;
            int textY = currentY + 6;

            // Safety check to ensure we don't render text off-screen
            if (0 < textY && textY < this.height) {
                if (line instanceof InstructionIF) {
                    context.drawTextWithShadow(this.textRenderer, "IF", SCRIPT_X + xOffset, textY, 0xFF0000);
                } else if (line instanceof InstructionWHILE) {
                    context.drawTextWithShadow(this.textRenderer, "WHILE", SCRIPT_X + xOffset, textY, 0x66FF66);
                } else if (line instanceof InstructionMath) {
                    // I HAD TO ADJUST THIS VALUE MANUALLY 1 PIXEL AND RECOMPILE THE THING 6+ TIMES
                    int extraPadding = 55;
                    context.drawTextWithShadow(this.textRenderer, "=", SCRIPT_X + xOffset + extraPadding, textY, 0xFFFFFF);
                } else if (line instanceof InstructionPrint) {
                    context.drawTextWithShadow(this.textRenderer, "PRINT", SCRIPT_X + xOffset, textY, 0xAAAAAA);
                } else if (line instanceof InstructionEntity_FollowEntity) {
                    context.drawTextWithShadow(this.textRenderer, "FollowEntity", SCRIPT_X + xOffset, textY, 0x55FFFF);
                } else if (line instanceof InstructionBlock_Place) {
                    context.drawTextWithShadow(this.textRenderer, "Place", SCRIPT_X + xOffset, textY, 0x55FFFF);
                }
            }

            currentY += LINE_HEIGHT;

            if (line instanceof InstructionIF) {
                currentY = drawLabelsRecursive(context, ((InstructionIF) line).trueBlock, currentY, indent + 1);
            } else if (line instanceof InstructionWHILE) {
                currentY = drawLabelsRecursive(context, ((InstructionWHILE) line).loopBlock, currentY, indent + 1);
            }
        }
        return currentY;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        // Left click
        if (button == 0) {
            double virtualY = mouseY + scrollOffset;

            ScriptLine clicked = findLineAt((int)virtualY, this.builder.GetScript(), START_Y);
            if (clicked != null) {
                ScriptBlock parent = findParentOfLine(this.builder.GetScript(), clicked);
                if (parent != null) {
                    this.builder.Select(parent, clicked);
                    this.rebuildUI();
                    return true;
                }
            } else {
                // Clicked empty space
                // this.builder.Deselect();
                // this.rebuildUI();
            }
        }
        return false;
    }
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int drawY = (int) (START_Y - scrollOffset);

        // Draw Backgrounds
        // drawBlockBackgrounds(context, this.builder.GetScript(), drawY, 0);

        // Draw HIGHLIGHT for Selected Line
        drawSelectionHighlight(context, this.builder.GetScript(), drawY);

        // Draw Widgets (Buttons/Fields)
        super.render(context, mouseX, mouseY, delta);

        // Draw Indentation Lines
        drawIndentationLines(context, this.builder.GetScript(), drawY, 0);

        // Draw Text Labels
        drawLabelsRecursive(context, this.builder.GetScript(), drawY, 0);
    }
    private int drawSelectionHighlight(DrawContext context, ScriptBlock block, int currentY) {
        for (ScriptLine line : block.blockLines) {
            if (line == builder.selectedLine) {
                if (currentY > 0 && currentY < this.height) {
                    int highlightColor = 0x55FFFFFF;
                    context.fill(SCRIPT_X - 10, currentY, this.width - 20, currentY + LINE_HEIGHT, highlightColor);
                }
            }

            currentY += LINE_HEIGHT;

            if (line instanceof InstructionIF) {
                currentY = drawSelectionHighlight(context, ((InstructionIF) line).trueBlock, currentY);
            } else if (line instanceof InstructionWHILE) {
                currentY = drawSelectionHighlight(context, ((InstructionWHILE) line).loopBlock, currentY);
            }
        }
        return currentY;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Adjust scroll offset

        // Scroll speed
        this.scrollOffset -= verticalAmount * 20;

        // Clamp scroll
        // Max scroll: Content Height - Viewport Height (approx) + Padding
        double maxScroll = Math.max(0, this.contentHeight - (this.height - 40));
        if (this.scrollOffset < 0) this.scrollOffset = 0;
        if (maxScroll < this.scrollOffset) this.scrollOffset = maxScroll;

        updateScroll();
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    private void updateScroll() {
        for (PlacedWidget pw : scrollableWidgets) {
            int newY = (int) (pw.originalY - scrollOffset);
            pw.widget.setY(newY);

            // Visibility Culling: hide if off-screen to prevent clicking/rendering outside bounds
            // Assuming top bar is ~20px and bottom is valid
            pw.widget.visible = 10 < newY && newY < this.height - 10;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        assert this.client != null;
        assert this.client.player != null;

        // TODO asegurarse de que el usuario no esté escribiendo en una caja de texto para abrir el inventario
        if (this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            ScreenReturnHandler.returnScreen = this;
            this.client.setScreen(new InventoryScreen(this.client.player));
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        ScriptActorManager.getInstance().saveBuilder(entityUuid, this.builder);

        super.close();
    }

    // UUID AND INVENTORY STUFF

    private Entity findEntityByUUID(String uuidString) {
        if (this.client == null || this.client.world == null) return null;

        try {
            UUID uuid = UUID.fromString(uuidString);
            for (Entity entity : this.client.world.getEntities()) {
                if (entity.getUuid().equals(uuid)) {
                    return entity;
                }
            }
        } catch (IllegalArgumentException e) {
            // Invalid UUID string
            return null;
        }
        // Entity not found or not loaded on client
        return null;
    }

    private List<String> scanInventoryForEntityUUIDs() {
        List<String> foundUUIDs = new ArrayList<>();
        if (this.client == null || this.client.player == null) return foundUUIDs;

        for (ItemStack stack : this.client.player.getInventory().main) {
            if (stack.isEmpty()) continue;

            String name = stack.getName().getString();

            // Check if the name is a valid UUID
            try {
                UUID.fromString(name);
                // If no exception was thrown, it's valid
                if (!foundUUIDs.contains(name)) {
                    foundUUIDs.add(name);
                }
            } catch (IllegalArgumentException e) {
                // Not a UUID, ignore this item
            }
        }
        return foundUUIDs;
    }

    // puts the clicked entity into the function
    private void insertUUID(String uuid) {
        String formattedUUID = "\"" + uuid + "\"";

        // Scenario 1: User has a specific Text Field focused
        if (this.getFocused() instanceof TextFieldWidget tf) {
            tf.write(formattedUUID);
            return;
        }

        // Scenario 2: User has a line selected in the script
        if (builder.selectedLine != null) {
            if (builder.selectedLine instanceof InstructionEntity_FollowEntity followInstr) {
                followInstr.targetUUID = formattedUUID;
                this.rebuildUI();
            }
            // TODO: Other future instructions
            else if (builder.selectedLine instanceof InstructionMath mathInstr) {
                mathInstr.expression = formattedUUID;
                this.rebuildUI();
            }
        }
    }
}
