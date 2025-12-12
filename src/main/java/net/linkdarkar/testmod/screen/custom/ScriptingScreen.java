package net.linkdarkar.testmod.screen.custom;

import net.linkdarkar.testmod.scripting.*;
import net.linkdarkar.testmod.scripting.enums.ComparisonOperator;
import net.linkdarkar.testmod.scripting.instructions.InstructionIF;
import net.linkdarkar.testmod.scripting.instructions.InstructionMath;
import net.linkdarkar.testmod.scripting.instructions.InstructionWHILE;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

public class ScriptingScreen extends Screen {
    public ScriptBuilder builder;

    private final int START_Y = 20;
    private final int LINE_HEIGHT = 25;
    private final int SCRIPT_X = 140;

    public ScriptingScreen() {
        super(Text.literal("opens scripting screen"));
        this.builder = new ScriptBuilder();
    }

    @Override
    protected void init() {
        super.init();

        // Add Control Buttons (Left Side)
        int btnY = 50;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("New Variable"), b -> {
            this.builder.DeclareVar("varName", 0);
            this.rebuildUI();
        }).dimensions(20, btnY, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("ADD IF"), b -> {
            this.builder.BeginIf("", ComparisonOperator.EQUALS, "");
            this.rebuildUI();
        }).dimensions(20, btnY + 25, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("ADD WHILE"), b -> {
            this.builder.BeginWhile("", ComparisonOperator.EQUALS, "");
            this.rebuildUI();
        }).dimensions(20, btnY + 50, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("END BLOCK"), b -> {
            this.builder.EndBlock();
            this.rebuildUI();
        }).dimensions(20, btnY + 75, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("SAVE"), b -> {
            // Save
            this.close();
        }).dimensions(20, btnY + 100, 100, 20).build());

        renderBlockRecursive(this.builder.GetScript(), START_Y, 0);
    }
    private void rebuildUI() {
        this.clearChildren();
        this.init();
    }

    private int renderBlockRecursive(ScriptBlock block, int currentY, int indent) {
        List<ScriptLine> lines = block.blockLines;

        for (ScriptLine line : lines) {
            int xOffset = indent * 20;

            if (line instanceof InstructionIF) {
                InstructionIF ifLine = (InstructionIF) line;
                createConditionWidgets(ifLine.condition, "IF", currentY, xOffset);
                currentY += LINE_HEIGHT;
                currentY = renderBlockRecursive(ifLine.trueBlock, currentY, indent + 1);

            } else if (line instanceof InstructionWHILE) {
                InstructionWHILE whileLine = (InstructionWHILE) line;
                createConditionWidgets(whileLine.condition, "WHILE", currentY, xOffset);
                currentY += LINE_HEIGHT;
                currentY = renderBlockRecursive(whileLine.loopBlock, currentY, indent + 1);

            } else if (line instanceof InstructionMath) {
                InstructionMath mathLine = (InstructionMath) line;
                createMathWidgets(mathLine, currentY, xOffset);
                currentY += LINE_HEIGHT;
            }
        }
        return currentY;
    }
    private void createConditionWidgets(ScriptCondition cond, String label, int y, int xOffset) {
        int baseX = SCRIPT_X + xOffset;

        if (cond == null) return;
        final ScriptCondition finalCond = cond;

        // Left Variable
        TextFieldWidget leftField = new TextFieldWidget(this.textRenderer, baseX + 40, y, 50, 20, Text.literal(""));
        leftField.setText(cond.lVar.GetOriginalValue());
        leftField.setChangedListener(text -> finalCond.lVar.UpdateValue(text));
        this.addDrawableChild(leftField);

        // Operator Button
        ButtonWidget opButton = ButtonWidget.builder(Text.literal(getOpSymbol(cond.op)), button -> {
            finalCond.op = nextOperator(finalCond.op);
            button.setMessage(Text.literal(getOpSymbol(finalCond.op)));
        }).dimensions(baseX + 95, y, 25, 20).build();
        this.addDrawableChild(opButton);

        // Right Variable
        TextFieldWidget rightField = new TextFieldWidget(this.textRenderer, baseX + 125, y, 50, 20, Text.literal(""));
        rightField.setText(cond.rVar.GetOriginalValue());
        rightField.setChangedListener(text -> finalCond.rVar.UpdateValue(text));
        this.addDrawableChild(rightField);
    }
    private void createMathWidgets(InstructionMath line, int y, int xOffset) {
        int baseX = SCRIPT_X + xOffset;

        TextFieldWidget targetField = new TextFieldWidget(this.textRenderer, baseX, y, 60, 20, Text.literal(""));
        targetField.setText(line.targetVarName);
        targetField.setChangedListener(text -> line.targetVarName = text);
        this.addDrawableChild(targetField);

        TextFieldWidget valueField = new TextFieldWidget(this.textRenderer, baseX + 80, y, 60, 20, Text.literal(""));
        valueField.setText(line.right.GetOriginalValue());
        valueField.setChangedListener(text -> line.right.UpdateValue(text));
        this.addDrawableChild(valueField);
    }
    private ComparisonOperator nextOperator(ComparisonOperator current) {
        int nextOrdinal = (current.ordinal() + 1) % ComparisonOperator.values().length;
        return ComparisonOperator.values()[nextOrdinal];
    }

    private String getOpSymbol(ComparisonOperator op) {
        switch (op) {
            case EQUALS: return "==";
            case DIFFERENT: return "!=";
            case LESS_THAN: return "<";
            case GREATER_THAN: return ">";
            default: return "?";
        }
    }

    private int getBlockHeight(ScriptBlock block) {
        int height = 0;
        for (ScriptLine line : block.blockLines) {
            height += LINE_HEIGHT;
            if (line instanceof InstructionIF) {
                height += getBlockHeight(((InstructionIF) line).trueBlock);
            } else if (line instanceof InstructionWHILE) {
                height += getBlockHeight(((InstructionWHILE) line).loopBlock);
            }
        }
        return height;
    }
    private int drawBlockBackgrounds(DrawContext context, ScriptBlock block, int currentY, int indent) {
        for (ScriptLine line : block.blockLines) {
            int xOffset = indent * 20;

            if (line instanceof InstructionIF) {
                InstructionIF ifLine = (InstructionIF) line;
                int innerHeight = getBlockHeight(ifLine.trueBlock);
                int startX = SCRIPT_X + xOffset;

                // Background Box: Extends to this.width (Screen edge)
                int bgColor = 0x22FF0000;
                context.fill(startX + 10, currentY + LINE_HEIGHT, this.width, currentY + LINE_HEIGHT + innerHeight, bgColor);

                currentY += LINE_HEIGHT;
                currentY = drawBlockBackgrounds(context, ifLine.trueBlock, currentY, indent + 1);

            } else if (line instanceof InstructionWHILE) {
                InstructionWHILE whileLine = (InstructionWHILE) line;
                int innerHeight = getBlockHeight(whileLine.loopBlock);
                int startX = SCRIPT_X + xOffset;

                // Background Box
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

            if (line instanceof InstructionIF) {
                InstructionIF ifLine = (InstructionIF) line;
                int innerHeight = getBlockHeight(ifLine.trueBlock);
                int startX = SCRIPT_X + xOffset;

                // Vertical Line (Red)
                int bracketColor = 0xFFFF0000;
                context.fill(startX + 5, currentY + LINE_HEIGHT, startX + 7, currentY + LINE_HEIGHT + innerHeight, bracketColor);

                currentY += LINE_HEIGHT;
                currentY = drawIndentationLines(context, ifLine.trueBlock, currentY, indent + 1);

            } else if (line instanceof InstructionWHILE) {
                InstructionWHILE whileLine = (InstructionWHILE) line;
                int innerHeight = getBlockHeight(whileLine.loopBlock);
                int startX = SCRIPT_X + xOffset;

                // Vertical Line (Green)
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

            if (line instanceof InstructionIF) {
                context.drawTextWithShadow(this.textRenderer, "IF", SCRIPT_X + xOffset, textY, 0xFF0000);
                currentY += LINE_HEIGHT;
                currentY = drawLabelsRecursive(context, ((InstructionIF) line).trueBlock, currentY, indent + 1);

            } else if (line instanceof InstructionWHILE) {
                context.drawTextWithShadow(this.textRenderer, "WHILE", SCRIPT_X + xOffset, textY, 0x66FF66);
                currentY += LINE_HEIGHT;
                currentY = drawLabelsRecursive(context, ((InstructionWHILE) line).loopBlock, currentY, indent + 1);

            } else if (line instanceof InstructionMath) {
                context.drawTextWithShadow(this.textRenderer, "=", SCRIPT_X + xOffset + 68, textY, 0xFFFFFF);
                currentY += LINE_HEIGHT;
            }
        }
        return currentY;
    }
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        // Draw Backgrounds
        // drawBlockBackgrounds(context, this.builder.GetScript(), START_Y, 0);

        // Draw Widgets (Buttons/Fields)
        super.render(context, mouseX, mouseY, delta);

        // Draw Lines
        drawIndentationLines(context, this.builder.GetScript(), START_Y, 0);

        // Draw Text Labels
        drawLabelsRecursive(context, this.builder.GetScript(), START_Y, 0);
    }

}
