package net.linkdarkar.testmod.screen.custom;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.linkdarkar.testmod.scripting.*;
import net.linkdarkar.testmod.scripting.enums.ComparisonOperator;
import net.linkdarkar.testmod.scripting.instructions.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.linkdarkar.testmod.networking.ModNetworking.ExecuteOncePayload;
import net.linkdarkar.testmod.networking.ModNetworking.SetTickingPayload;
import net.minecraft.util.Formatting;

import java.util.*;

public class ScriptingScreen extends Screen {
    protected final UUID entityUuid;
    protected final UUID defaultEntityUuid;

    protected final LivingEntity entity;
    public ScriptBuilder builder;

    protected final int START_Y = 20;
    protected final int LINE_HEIGHT = 25;
    protected final int SCRIPT_X = 140;

    protected double scrollOffset = 0;
    protected double horizontalScrollOffset = 0;
    protected int contentHeight = 0;

    protected final List<PlacedWidget> scrollableWidgets = new ArrayList<>();

    protected record PlacedWidget(ClickableWidget widget, int originalX, int originalY, boolean pinX) {}

    protected Map<ScriptLine, List<String>> currentErrors = new HashMap<>();

    protected String verificationMessage = "";
    protected int verificationColor = 0xFFFFFF;

    public ScriptingScreen(LivingEntity entity) {
        super(Text.literal("opens scripting screen"));
        this.entity = entity;
        this.entityUuid = entity.getUuid();
        this.defaultEntityUuid = new UUID(~entity.getUuid().getMostSignificantBits(), entity.getUuid().getLeastSignificantBits());

        ScriptBuilder existing = ScriptActorManager.getInstance().getBuilder(entityUuid);
        if (existing != null) {
            this.builder = existing;
        } else {
            this.resetToDefaultScript();
        }
    }

    @Override
    protected void init() {
        super.init();
        this.scrollableWidgets.clear();

        this.currentErrors = this.builder.GetScriptErrors();

        // Add Control Buttons (Left Side)
        int btnY = 20;
        int btnWidth = 80;
        int leftOffset = 10;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset Script"), b -> {
            this.resetToDefaultScript();
            this.rebuildUI();
        }).dimensions(leftOffset, btnY, btnWidth, 20).build());
        btnY += 25;

        ScriptingConfigManager.ScriptingConfig config = ScriptingConfigManager.getInstance().getConfig(this.entityUuid);

        // NEW VAR
        if (config.allowVar) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Add VAR"), b -> {
                this.builder.AddMath();
                this.rebuildUI();
            }).dimensions(leftOffset, btnY, btnWidth, 20).build());
            btnY += 25;
        }

        // ADD VAR = FUNCTION
        if (config.allowVar) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Add VARF"), b -> {
                this.builder.AddVarAssign();
                this.rebuildUI();
            }).dimensions(leftOffset, btnY, btnWidth, 20).build());
            btnY += 25;
        }

        // ADD IF
        if (config.allowIf) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Add IF"), b -> {
                this.builder.AddIf();
                this.rebuildUI();
            }).dimensions(leftOffset, btnY, btnWidth, 20).build());
            btnY += 25;
        }

        // ADD ELSE
        if (config.allowElse) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Add ELSE"), b -> {
                this.builder.AddElse();
                this.rebuildUI();
            }).dimensions(leftOffset, btnY, btnWidth, 20).build());
            btnY += 25;
        }

        // ADD WHILE
        if (config.allowWhile) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Add WHILE"), b -> {
                this.builder.AddWhile();
                this.rebuildUI();
            }).dimensions(leftOffset, btnY, btnWidth, 20).build());
            btnY += 25;
        }

        // ADD PRINT (to chat)
        if (config.allowPrint) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("PRINT"), b -> {
                this.builder.AddPrint();
                this.rebuildUI();
            }).dimensions(leftOffset, btnY, btnWidth, 20).build());
            btnY += 25;
        }

        // ADD FOLLOW_ENTITY
        if (config.allowFollow) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Follow Entity"), b -> {
                this.builder.AddFollowEntity();
                this.rebuildUI();
            }).dimensions(leftOffset, btnY, btnWidth, 20).build());
            btnY += 25;
        }

        // ADD PLACE_BLOCK
        if (config.allowPlace) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Place Block"), b -> {
                this.builder.AddPlaceBlock();
                this.rebuildUI();
            }).dimensions(leftOffset, btnY, btnWidth, 20).build());
            btnY += 25;
        }

        // ADD COMMAND
        if (config.allowCommand) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Add Command"), b -> {
                this.builder.AddCommand();
                this.rebuildUI();
            }).dimensions(leftOffset, btnY, btnWidth, 20).build());
            btnY += 25;
        }

        int execY = this.height - 80;

        // VERIFY CODE
        this.addDrawableChild(ButtonWidget.builder(Text.literal("VERIFY"), b -> {
            UUID targetCorrectUuid = new UUID(this.entityUuid.getMostSignificantBits(), ~this.entityUuid.getLeastSignificantBits());
            ScriptBuilder correctBuilder = ScriptActorManager.getInstance().getBuilder(targetCorrectUuid);

            if (correctBuilder == null || correctBuilder.GetScript().blockLines.isEmpty()) {
                this.verificationMessage = "No correct answer defined by teacher!";
                this.verificationColor = 0xFF5555;
                return;
            }

            ScriptVerifier.VerificationResult result = ScriptVerifier.verify(this.builder.GetScript(), correctBuilder.GetScript(), this.entity);
            this.verificationMessage = result.message();
            this.verificationColor = result.isCorrect() ? 0x55FF55 : 0xFF5555;

            if (this.client != null && this.client.getNetworkHandler() != null) {
                ScriptingConfigManager.EntityActions actions = ScriptingConfigManager.getInstance().getActions(this.entityUuid);

                processAction(actions.anyExecute);

                if (result.isCorrect()) {
                    processAction(actions.executeCorrect);
                } else {
                    processAction(actions.executeWrong);
                }
            }

        }).dimensions(leftOffset, execY - 25, btnWidth, 20).build());

        // EXECUTE ONCE
        this.addDrawableChild(ButtonWidget.builder(Text.literal("EXECUTE ONCE"), b -> {
            NbtCompound scriptNbt = this.builder.toNbt();
            ClientPlayNetworking.send(new ExecuteOncePayload(this.entityUuid, scriptNbt));
            this.close();
        }).dimensions(leftOffset, execY, btnWidth, 20).build());

        // START LOOP
        this.addDrawableChild(ButtonWidget.builder(Text.literal("START LOOP"), b -> {
            NbtCompound scriptNbt = this.builder.toNbt();
            ClientPlayNetworking.send(new SetTickingPayload(this.entityUuid, true, scriptNbt));
            this.close();
        }).dimensions(leftOffset, execY + 25, btnWidth, 20).build());

        // STOP LOOP
        this.addDrawableChild(ButtonWidget.builder(Text.literal("STOP LOOP"), b -> {
            ClientPlayNetworking.send(new SetTickingPayload(this.entityUuid, false, new NbtCompound()));
            this.close();
        }).dimensions(leftOffset, execY + 50, btnWidth, 20).build());

        // UUID stuff
        // ------------------------------

        List<String> entityUUIDs = scanInventoryForEntityUUIDs();

        int listX = this.width - 110;
        int listY = 40;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("UUIDs in Inventory"), b -> {})
                .dimensions(listX, 20, 100, 20).build()).active = false;

        for (String uuidStr : entityUUIDs) {
            String labelText;
            Entity foundEntity = findEntityByUUID(uuidStr);

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
                insertUUID(uuidStr);
            }).dimensions(listX, listY, 100, 20).build());

            listY += 25;
        }

        int finalY = generateWidgetsRecursive(this.builder.GetScript(), START_Y, 0);

        this.contentHeight = finalY - START_Y;

        updateScroll();
    }

    protected void rebuildUI() {
        this.clearChildren();
        this.init();
    }

    protected ScriptLine findLineAt(int mouseY, ScriptBlock block, int currentY) {
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
            else if (line instanceof InstructionELSE) {
                ScriptLine foundInChild = findLineAt(mouseY, ((InstructionELSE) line).elseBlock, currentY);
                if (foundInChild != null) return foundInChild;
                currentY += getBlockHeight(((InstructionELSE) line).elseBlock);
            }
            else if (line instanceof InstructionWHILE) {
                ScriptLine foundInChild = findLineAt(mouseY, ((InstructionWHILE) line).loopBlock, currentY);
                if (foundInChild != null) return foundInChild;
                currentY += getBlockHeight(((InstructionWHILE) line).loopBlock);
            }
            else if (line instanceof InstructionVarAssign) {
                ScriptLine foundInChild = findLineAt(mouseY, ((InstructionVarAssign) line).valueBlock, currentY);
                if (foundInChild != null) return foundInChild;
                currentY += getBlockHeight(((InstructionVarAssign) line).valueBlock);
            }
        }
        return null;
    }

    protected ScriptBlock findParentOfLine(ScriptBlock scope, ScriptLine target) {
        if (scope.blockLines.contains(target)) return scope;

        for (ScriptLine line : scope.blockLines) {
            ScriptBlock child = null;
            if (line instanceof InstructionIF) child = ((InstructionIF) line).trueBlock;
            else if (line instanceof InstructionELSE) child = ((InstructionELSE) line).elseBlock;
            else if (line instanceof InstructionWHILE) child = ((InstructionWHILE) line).loopBlock;
            else if (line instanceof InstructionVarAssign) child = ((InstructionVarAssign) line).valueBlock;

            if (child != null) {
                ScriptBlock res = findParentOfLine(child, target);
                if (res != null) return res;
            }
        }
        return null;
    }

    protected int generateWidgetsRecursive(ScriptBlock block, int currentY, int indent) {
        int contentStartX = SCRIPT_X + (indent * 20);

        for (ScriptLine line : block.blockLines) {

            if (builder.selectedLine == line) {
                createSelectionButtons(contentStartX, currentY);
            }

            if (line instanceof InstructionIF ifLine) {
                createConditionWidgets(ifLine.condition, currentY, contentStartX);
                currentY += LINE_HEIGHT;
                currentY = generateWidgetsRecursive(ifLine.trueBlock, currentY, indent + 1);
            } else if (line instanceof InstructionELSE elseLine) {
                currentY += LINE_HEIGHT;
                currentY = generateWidgetsRecursive(elseLine.elseBlock, currentY, indent + 1);
            } else if (line instanceof InstructionWHILE whileLine) {
                createConditionWidgets(whileLine.condition, currentY, contentStartX);
                currentY += LINE_HEIGHT;
                currentY = generateWidgetsRecursive(whileLine.loopBlock, currentY, indent + 1);
            } else if (line instanceof InstructionMath mathLine) {
                createMathWidgets(mathLine, currentY, contentStartX);
                currentY += LINE_HEIGHT;
            } else if (line instanceof InstructionVarAssign varLine) {
                createVarAssignWidgets(varLine, currentY, contentStartX);
                currentY += LINE_HEIGHT;
                currentY = generateWidgetsRecursive(varLine.valueBlock, currentY, indent + 1);
            } else if (line instanceof InstructionPrint printLine) {
                createPrintWidgets(printLine, currentY, contentStartX);
                currentY += LINE_HEIGHT;
            } else if (line instanceof InstructionEntity_FollowEntity followLine) {
                createFollowEntityWidgets(followLine, currentY, contentStartX);
                currentY += LINE_HEIGHT;
            } else if (line instanceof InstructionBlock_Place placeLine) {
                createPlaceBlockWidgets(placeLine, currentY, contentStartX);
                currentY += LINE_HEIGHT;
            } else if (line instanceof InstructionMinecraft_ExecuteCommand cmdLine) {
                createCommandWidgets(cmdLine, currentY, contentStartX);
                currentY += LINE_HEIGHT;
            }
        }
        return currentY;
    }

    protected void createPrintWidgets(InstructionPrint line, int y, int startX) {
        TextFieldWidget msgField = new TextFieldWidget(this.textRenderer, startX + 45, y, 250, 20, Text.literal(""));
//        EditBoxWidget
        msgField.setMaxLength(256);
        msgField.setText(line.message);
        msgField.setChangedListener(text -> line.message = text);
        addScrollableChild(msgField, startX + 45, y);
    }

    protected void createMathWidgets(InstructionMath line, int y, int startX) {
        TextFieldWidget targetField = new TextFieldWidget(this.textRenderer, startX, y, 80, 20, Text.literal(""));
        targetField.setMaxLength(256);
        targetField.setText(line.targetVarName);
        targetField.setChangedListener(text -> line.targetVarName = text);
        addScrollableChild(targetField, startX, y);

        TextFieldWidget exprField = new TextFieldWidget(this.textRenderer, startX + 95, y, 200, 20, Text.literal(""));
        exprField.setMaxLength(256);
        exprField.setText(line.expression);
        exprField.setChangedListener(text -> line.expression = text);
        addScrollableChild(exprField, startX + 95, y);
    }

    protected void createVarAssignWidgets(InstructionVarAssign line, int y, int startX) {
        TextFieldWidget targetField = new TextFieldWidget(this.textRenderer, startX, y, 120, 20, Text.literal(""));
        targetField.setMaxLength(256);
        targetField.setText(line.targetVarName);
        targetField.setChangedListener(text -> line.targetVarName = text);
        addScrollableChild(targetField, startX, y);
    }

    protected void createPlaceBlockWidgets(InstructionBlock_Place line, int y, int startX) {
        int fieldWidth = 60;
        int gap = 5;

        int currentX = startX + 35;

        // X Field
        TextFieldWidget xField = new TextFieldWidget(this.textRenderer, currentX, y, fieldWidth, 20, Text.literal(""));
        xField.setMaxLength(256);
        xField.setText(line.xExp);
        xField.setChangedListener(text -> line.xExp = text);
        addScrollableChild(xField, currentX, y);

        currentX += fieldWidth + gap;

        // Y Field
        TextFieldWidget yField = new TextFieldWidget(this.textRenderer, currentX, y, fieldWidth, 20, Text.literal(""));
        yField.setMaxLength(256);
        yField.setText(line.yExp);
        yField.setChangedListener(text -> line.yExp = text);
        addScrollableChild(yField, currentX, y);

        currentX += fieldWidth + gap;

        // Z Field
        TextFieldWidget zField = new TextFieldWidget(this.textRenderer, currentX, y, fieldWidth, 20, Text.literal(""));
        zField.setMaxLength(256);
        zField.setText(line.zExp);
        zField.setChangedListener(text -> line.zExp = text);
        addScrollableChild(zField, currentX, y);
    }

    protected <T extends ClickableWidget> void addScrollableChild(T widget, int originalX, int originalY) {
        this.addSelectableChild(widget);
        this.scrollableWidgets.add(new PlacedWidget(widget, originalX, originalY, false));
    }

    protected <T extends ClickableWidget> void addScrollableChildPinned(T widget, int originalX, int originalY) {
        this.addSelectableChild(widget);
        this.scrollableWidgets.add(new PlacedWidget(widget, originalX, originalY, true));
    }

    protected void createSelectionButtons(int startX, int y) {
        int rightColBoundary = this.width - 110;

        // Vertical Movement (Placed left of UUID list)
        int moveX = rightColBoundary - 25;
        addScrollableChildPinned(ButtonWidget.builder(Text.literal("↑"), b -> {
            builder.MoveUp(); rebuildUI();
        }).dimensions(moveX, y, 20, 10).build(), moveX, y);

        addScrollableChildPinned(ButtonWidget.builder(Text.literal("↓"), b -> {
            builder.MoveDown(); rebuildUI();
        }).dimensions(moveX, y + 10, 20, 10).build(), moveX, y + 10);

        // Delete [×] (Placed left of movement arrows)
        int delX = moveX - 25;
        addScrollableChildPinned(ButtonWidget.builder(Text.literal("×"), b -> {
            builder.DeleteSelected(); rebuildUI();
        }).dimensions(delX, y, 20, 20).build(), delX, y);

        // Nesting Controls [<] [>] (Placed left of delete)
        int nestX = delX - 45;
        addScrollableChildPinned(ButtonWidget.builder(Text.literal("<"), b -> {
            builder.MoveLeft(); rebuildUI();
        }).dimensions(nestX, y, 20, 20).build(), nestX, y);

        addScrollableChildPinned(ButtonWidget.builder(Text.literal(">"), b -> {
            builder.MoveRight(); rebuildUI();
        }).dimensions(nestX + 22, y, 20, 20).build(), nestX + 22, y);
    }

    protected void createConditionWidgets(ScriptCondition cond, int y, int startX) {
        if (cond == null) return;
        final ScriptCondition finalCond = cond;


        // Left Expression Field
        TextFieldWidget leftField = new TextFieldWidget(this.textRenderer, startX + 45, y, 100, 20, Text.literal(""));
        leftField.setMaxLength(256);
        leftField.setText(cond.leftExpression);
        leftField.setChangedListener(text -> finalCond.leftExpression = text);
        addScrollableChild(leftField, startX + 45, y);

        // Operator Button
        ButtonWidget opButton = ButtonWidget.builder(Text.literal(getOpSymbol(cond.op)), button -> {
            finalCond.op = nextOperator(finalCond.op);
            button.setMessage(Text.literal(getOpSymbol(finalCond.op)));
        }).dimensions(startX + 150, y, 25, 20).build();
        addScrollableChild(opButton, startX + 150, y);

        // Right Expression Field
        TextFieldWidget rightField = new TextFieldWidget(this.textRenderer, startX + 180, y, 100, 20, Text.literal(""));
        rightField.setMaxLength(256);
        rightField.setText(cond.rightExpression);
        rightField.setChangedListener(text -> finalCond.rightExpression = text);
        addScrollableChild(rightField, startX + 180, y);
    }
    protected void createCommandWidgets(InstructionMinecraft_ExecuteCommand line, int y, int startX) {
        TextFieldWidget cmdField = new TextFieldWidget(this.textRenderer, startX + 45, y, 250, 20, Text.literal(""));
        cmdField.setMaxLength(256);
        cmdField.setText(line.commandExpression);
        cmdField.setChangedListener(text -> line.commandExpression = text);
        addScrollableChild(cmdField, startX + 45, y);
    }
    protected void processAction(ScriptingConfigManager.ActionEventData actionData) {
        if (actionData.maxExecutions == 0 || actionData.currentExecutions < actionData.maxExecutions) {
            if (actionData.commands != null && !actionData.commands.trim().isEmpty()) {
                String[] commandsToRun = actionData.commands.split("\\r?\\n");
                for (String cmd : commandsToRun) {
                    cmd = cmd.trim();
                    if (!cmd.isEmpty()) {
                        if (this.client != null && this.client.getNetworkHandler() != null) {
                            this.client.getNetworkHandler().sendChatCommand(cmd);
                        }
                    }
                }
            }
            actionData.currentExecutions++;
        }
    }
    protected void createFollowEntityWidgets(InstructionEntity_FollowEntity line, int y, int startX) {
        TextFieldWidget targetField = new TextFieldWidget(this.textRenderer, startX + 45, y, 200, 20, Text.literal(""));
        targetField.setMaxLength(256);
        targetField.setText(line.targetUUID);
        targetField.setChangedListener(text -> line.targetUUID = text);
        addScrollableChild(targetField, startX + 45, y);
    }

    protected ComparisonOperator nextOperator(ComparisonOperator current) {
        int nextOrdinal = (current.ordinal() + 1) % ComparisonOperator.values().length;
        return ComparisonOperator.values()[nextOrdinal];
    }

    protected String getOpSymbol(ComparisonOperator op) {
        return switch (op) {
            case EQUALS -> "==";
            case DIFFERENT -> "!=";
            case LESS_THAN -> "<";
            case GREATER_THAN -> ">";
            default -> "?";
        };
    }

    protected int getBlockHeight(ScriptBlock block) {
        int height = 0;
        for (ScriptLine line : block.blockLines) {
            height += LINE_HEIGHT;
            if (line instanceof InstructionIF) height += getBlockHeight(((InstructionIF) line).trueBlock);
            else if (line instanceof InstructionELSE) height += getBlockHeight(((InstructionELSE) line).elseBlock);
            else if (line instanceof InstructionWHILE) height += getBlockHeight(((InstructionWHILE) line).loopBlock);
            else if (line instanceof InstructionVarAssign) height += getBlockHeight(((InstructionVarAssign) line).valueBlock);
        }
        return height;
    }

    protected int drawBlockBackgrounds(DrawContext context, ScriptBlock block, int currentY, int indent) {
        for (ScriptLine line : block.blockLines) {
            int xOffset = indent * 20;

            switch (line) {
                case InstructionIF ifLine -> {
                    int innerHeight = getBlockHeight(ifLine.trueBlock);
                    int startX = SCRIPT_X + xOffset;

                    int bgColor = 0x22FF0000;
                    context.fill(startX + 10, currentY + LINE_HEIGHT, this.width, currentY + LINE_HEIGHT + innerHeight, bgColor);

                    currentY += LINE_HEIGHT;
                    currentY = drawBlockBackgrounds(context, ifLine.trueBlock, currentY, indent + 1);
                }
                case InstructionELSE elseLine -> {
                    int innerHeight = getBlockHeight(elseLine.elseBlock);
                    int startX = SCRIPT_X + xOffset;
                    int bgColor = 0x22FFAA00; // Orange tint

                    context.fill(startX + 10, currentY + LINE_HEIGHT, this.width, currentY + LINE_HEIGHT + innerHeight, bgColor);
                    currentY += LINE_HEIGHT;
                    currentY = drawBlockBackgrounds(context, elseLine.elseBlock, currentY, indent + 1);
                }
                case InstructionWHILE whileLine -> {
                    int innerHeight = getBlockHeight(whileLine.loopBlock);
                    int startX = SCRIPT_X + xOffset;

                    int bgColor = 0x2200FF00;
                    context.fill(startX + 10, currentY + LINE_HEIGHT, this.width, currentY + LINE_HEIGHT + innerHeight, bgColor);

                    currentY += LINE_HEIGHT;
                    currentY = drawBlockBackgrounds(context, whileLine.loopBlock, currentY, indent + 1);
                }
                case InstructionVarAssign varLine -> {
                    int innerHeight = getBlockHeight(varLine.valueBlock);
                    int startX = SCRIPT_X + xOffset;

                    int bgColor = 0x22FFFF00;
                    context.fill(startX + 10, currentY + LINE_HEIGHT, this.width, currentY + LINE_HEIGHT + innerHeight, bgColor);

                    currentY += LINE_HEIGHT;
                    currentY = drawBlockBackgrounds(context, varLine.valueBlock, currentY, indent + 1);
                }
                case null, default -> currentY += LINE_HEIGHT;
            }
        }
        return currentY;
    }

    protected int drawIndentationLines(DrawContext context, ScriptBlock block, int currentY, int indent) {
        for (ScriptLine line : block.blockLines) {
            int xOffset = indent * 20;

            switch (line) {
                case InstructionIF ifLine -> {
                    int innerHeight = getBlockHeight(ifLine.trueBlock);
                    int actualX = SCRIPT_X + xOffset - (int) horizontalScrollOffset;

                    int bracketColor = 0xFFFF0000;
                    context.fill(actualX + 5, currentY + LINE_HEIGHT, actualX + 7, currentY + LINE_HEIGHT + innerHeight, bracketColor);

                    currentY += LINE_HEIGHT;
                    currentY = drawIndentationLines(context, ifLine.trueBlock, currentY, indent + 1);
                }
                case InstructionELSE elseLine -> {
                    int innerHeight = getBlockHeight(elseLine.elseBlock);
                    int actualX = SCRIPT_X + xOffset - (int) horizontalScrollOffset;
                    int bracketColor = 0xFFFFAA00;
                    context.fill(actualX + 5, currentY + LINE_HEIGHT, actualX + 7, currentY + LINE_HEIGHT + innerHeight, bracketColor);
                    currentY += LINE_HEIGHT;
                    currentY = drawIndentationLines(context, elseLine.elseBlock, currentY, indent + 1);
                }
                case InstructionWHILE whileLine -> {
                    int innerHeight = getBlockHeight(whileLine.loopBlock);
                    int actualX = SCRIPT_X + xOffset - (int) horizontalScrollOffset;

                    int bracketColor = 0xFF00FF00;
                    context.fill(actualX + 5, currentY + LINE_HEIGHT, actualX + 7, currentY + LINE_HEIGHT + innerHeight, bracketColor);

                    currentY += LINE_HEIGHT;
                    currentY = drawIndentationLines(context, whileLine.loopBlock, currentY, indent + 1);

                }
                case InstructionVarAssign varLine -> {
                    int innerHeight = getBlockHeight(varLine.valueBlock);
                    int actualX = SCRIPT_X + xOffset - (int) horizontalScrollOffset;

                    int bracketColor = 0xFFFFFF00;
                    context.fill(actualX + 5, currentY + LINE_HEIGHT, actualX + 7, currentY + LINE_HEIGHT + innerHeight, bracketColor);

                    currentY += LINE_HEIGHT;
                    currentY = drawIndentationLines(context, varLine.valueBlock, currentY, indent + 1);
                }
                case null, default -> currentY += LINE_HEIGHT;
            }
        }
        return currentY;
    }

    protected int drawLineNumbersRecursive(DrawContext context, ScriptBlock block, int currentY, int[] lineNum) {
        for (ScriptLine line : block.blockLines) {
            int textY = currentY + 6;

            int currentNum = lineNum[0]++;

            if (0 < textY && textY < this.height) {
                int numColor = (line == builder.selectedLine) ? (currentErrors.containsKey(line) ? 0xFF5555 : 0xFFFFFF) : (currentErrors.containsKey(line) ? 0xB4403E : 0x555555);

                int fixedLineNumX = SCRIPT_X - 20;

                context.drawTextWithShadow(this.textRenderer, String.valueOf(currentNum), fixedLineNumX, textY, numColor);
            }

            currentY += LINE_HEIGHT;

            switch (line) {
                case InstructionIF instructionIF ->
                        currentY = drawLineNumbersRecursive(context, instructionIF.trueBlock, currentY, lineNum);
                case InstructionELSE instructionELSE ->
                        currentY = drawLineNumbersRecursive(context, instructionELSE.elseBlock, currentY, lineNum);
                case InstructionWHILE instructionWHILE ->
                        currentY = drawLineNumbersRecursive(context, instructionWHILE.loopBlock, currentY, lineNum);
                case InstructionVarAssign instructionVarAssign ->
                        currentY = drawLineNumbersRecursive(context, instructionVarAssign.valueBlock, currentY, lineNum);
                case null, default -> {
                }
            }
        }
        return currentY;
    }

    protected int drawScriptTextRecursive(DrawContext context, ScriptBlock block, int currentY, int indent) {
        for (ScriptLine line : block.blockLines) {
            int xOffset = indent * 20;
            int actualX = SCRIPT_X + xOffset - (int)horizontalScrollOffset;
            int textY = currentY + 6;

            if (0 < textY && textY < this.height) {
                if (line instanceof InstructionIF) {
                    context.drawTextWithShadow(this.textRenderer, "IF", actualX, textY, 0xFF0000);
                } else if (line instanceof InstructionELSE) {
                    context.drawTextWithShadow(this.textRenderer, "ELSE", actualX, textY, 0xFFFFAA00);
                } else if (line instanceof InstructionWHILE) {
                    context.drawTextWithShadow(this.textRenderer, "WHILE", actualX, textY, 0x66FF66);
                } else if (line instanceof InstructionMath) {
                    context.drawTextWithShadow(this.textRenderer, "=", actualX + 85, textY, 0xFFFFFF);
                } else if (line instanceof InstructionPrint) {
                    context.drawTextWithShadow(this.textRenderer, "PRINT", actualX, textY, 0xAAAAAA);
                } else if (line instanceof InstructionEntity_FollowEntity) {
                    context.drawTextWithShadow(this.textRenderer, "Follow", actualX, textY, 0x55FFFF);
                } else if (line instanceof InstructionBlock_Place) {
                    context.drawTextWithShadow(this.textRenderer, "Place", actualX, textY, 0x55FFFF);
                } else if (line instanceof InstructionVarAssign) {
                    context.drawTextWithShadow(this.textRenderer, "VARF", actualX, textY, 0xFFFF55);
                } else if (line instanceof InstructionMinecraft_ExecuteCommand) {
                    context.drawTextWithShadow(this.textRenderer, "/cmd", actualX, textY, 0xFFAA00);
                }
            }

            currentY += LINE_HEIGHT;

            if (line instanceof InstructionIF) {
                currentY = drawScriptTextRecursive(context, ((InstructionIF) line).trueBlock, currentY, indent + 1);
            } else if (line instanceof InstructionELSE) {
                currentY = drawScriptTextRecursive(context, ((InstructionELSE) line).elseBlock, currentY, indent + 1);
            } else if (line instanceof InstructionWHILE) {
                currentY = drawScriptTextRecursive(context, ((InstructionWHILE) line).loopBlock, currentY, indent + 1);
            } else if (line instanceof InstructionVarAssign) {
                currentY = drawScriptTextRecursive(context, ((InstructionVarAssign) line).valueBlock, currentY, indent + 1);
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
                this.builder.Deselect();
                this.rebuildUI();
            }
        }
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int drawY = (int) (START_Y - scrollOffset);
        int rightBound = this.width - 110;

        drawLineNumbersRecursive(context, this.builder.GetScript(), drawY, new int[]{1});

        context.enableScissor(SCRIPT_X, 0, rightBound, this.height);

        // Draw HIGHLIGHT for Selected Line
        drawSelectionHighlight(context, this.builder.GetScript(), drawY, 0);

        // Draw Indentation Lines
        drawIndentationLines(context, this.builder.GetScript(), drawY, 0);

        // Draw Script Instruction Text (IF, WHILE, etc)
        drawScriptTextRecursive(context, this.builder.GetScript(), drawY, 0);

        for (PlacedWidget pw : scrollableWidgets) {
            if (pw.widget.visible) {
                pw.widget.render(context, mouseX, mouseY, delta);
            }
        }

        context.disableScissor();

        if (!this.verificationMessage.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, this.verificationMessage, SCRIPT_X, this.height - 20, this.verificationColor);
        }

        if (SCRIPT_X <= mouseX && mouseX <= rightBound && START_Y <= mouseY) {
            double virtualY = mouseY + scrollOffset;
            ScriptLine hoveredLine = findLineAt((int) virtualY, this.builder.GetScript(), START_Y);

            if (hoveredLine != null && currentErrors.containsKey(hoveredLine)) {
                List<String> errors = currentErrors.get(hoveredLine);
                List<Text> tooltipTexts = new ArrayList<>();

                for (String error : errors) {
                    tooltipTexts.add(Text.literal(error).formatted(Formatting.RED));
                }

                context.drawTooltip(this.textRenderer, tooltipTexts, mouseX, mouseY);
            }
        }
    }

    protected int drawSelectionHighlight(DrawContext context, ScriptBlock block, int currentY, int indent) {
        for (ScriptLine line : block.blockLines) {
            if (line == builder.selectedLine) {
                if (0 < currentY && currentY < this.height) {
                    int highlightColor = currentErrors.containsKey(line) ? 0xFF998F : 0x55FFFFFF;

                    int rightBound = this.width - 110;
                    context.fill(SCRIPT_X, currentY, rightBound, currentY + LINE_HEIGHT, highlightColor);
                }
            }

            currentY += LINE_HEIGHT;

            if (line instanceof InstructionIF) {
                currentY = drawSelectionHighlight(context, ((InstructionIF) line).trueBlock, currentY, indent + 1);
            } else if (line instanceof InstructionELSE) {
                currentY = drawSelectionHighlight(context, ((InstructionELSE) line).elseBlock, currentY, indent + 1);
            } else if (line instanceof InstructionWHILE) {
                currentY = drawSelectionHighlight(context, ((InstructionWHILE) line).loopBlock, currentY, indent + 1);
            } else if (line instanceof InstructionVarAssign) {
                currentY = drawSelectionHighlight(context, ((InstructionVarAssign) line).valueBlock, currentY, indent + 1);
            }
        }
        return currentY;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Adjust scroll offset
        double scrollSpeed = verticalAmount * 20;
        if (Screen.hasShiftDown()) {
            this.horizontalScrollOffset -= scrollSpeed;
            if (this.horizontalScrollOffset < 0) this.horizontalScrollOffset = 0;
            if (500 < this.horizontalScrollOffset) this.horizontalScrollOffset = 500;
        } else {
            this.scrollOffset -= scrollSpeed;
            double maxScroll = Math.max(0, this.contentHeight - (this.height - 40));
            if (this.scrollOffset < 0) this.scrollOffset = 0;
            if (maxScroll < this.scrollOffset) this.scrollOffset = maxScroll;
        }

        updateScroll();
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    protected void updateScroll() {
        for (PlacedWidget pw : scrollableWidgets) {
            int newY = (int) (pw.originalY - scrollOffset);

            int newX = pw.pinX ? pw.originalX : (int) (pw.originalX - horizontalScrollOffset);

            pw.widget.setY(newY);
            pw.widget.setX(newX);

            boolean visibleVertically = 10 < newY && newY < this.height - 10;

            pw.widget.visible = visibleVertically;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    protected void resetToDefaultScript() {
        this.builder = new ScriptBuilder(this.entity);
        ScriptBuilder defaultBuilder = ScriptActorManager.getInstance().getBuilder(this.defaultEntityUuid);

        if (defaultBuilder != null) {
            NbtCompound defaultNbt = defaultBuilder.toNbt();
            this.builder = ScriptBuilder.fromNbt(defaultNbt);
        }

        ScriptActorManager.getInstance().saveBuilder(this.entityUuid, this.builder);
    }

    @Override
    public void close() {
        ScriptActorManager.getInstance().saveBuilder(entityUuid, this.builder);
        super.close();
    }

    // UUID AND INVENTORY STUFF

    protected Entity findEntityByUUID(String uuidString) {
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

    protected List<String> scanInventoryForEntityUUIDs() {
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
    protected void insertUUID(String uuid) {
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