package net.linkdarkar.testmod.screen.custom;

import net.linkdarkar.testmod.item.custom.questionHandler.QuestionLoader;
import net.linkdarkar.testmod.scripting.ScriptBlock;
import net.linkdarkar.testmod.scripting.ScriptLine;
import net.linkdarkar.testmod.scripting.instructions.InstructionIF;
import net.linkdarkar.testmod.scripting.instructions.InstructionWHILE;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ScriptingScreen extends Screen {
    public ScriptBlock scriptBlock;

    public ScriptingScreen() {
        super(Text.literal("opens scripting screen"));
        this.scriptBlock = new ScriptBlock();
    }

    @Override
    protected void init() {
        super.init();
        try {
            this.addDrawableChild(
                    ButtonWidget.builder(
                                    Text.literal("IF"),
                                    button -> {
                                        this.onAddInstructionClicked(new InstructionIF());
                                    }
                            )
                            .dimensions(20, 160, 100, 20)
                            .build()
            );
            this.addDrawableChild(
                    ButtonWidget.builder(
                                    Text.literal("WHILE"),
                                    button -> {
                                        this.onAddInstructionClicked(new InstructionWHILE());
                                    }
                            )
                            .dimensions(20, 190, 100, 20)
                            .build()
            );
            this.addDrawableChild(
                    ButtonWidget.builder(
                                    Text.literal("EXECUTE"),
                                    button -> {
                                        this.onExecuteClicked();
                                    }
                            )
                            .dimensions(20, 220, 100, 20)
                            .build()
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void onExecuteClicked() {
        this.scriptBlock.ExecuteLines();
        this.close();
    }

    // TODO change to different ones for each instructions and shit
    // Actually make it receive a new InstructionSOMETHING()
    private void onAddInstructionClicked(ScriptLine scriptLine) {
        assert this.client != null;
        if (client.player == null) {
            return;
        }

        this.scriptBlock.AddInstruction(scriptLine);
    }

    @Override
    public void render (DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        for (int i = 0; i < this.scriptBlock.blockLines.size(); i += 1) {
            context.drawTextWithShadow(
                    this.textRenderer,
                    this.scriptBlock.blockLines.get(i).GetAsText(),
                    180,
                    10 + (i * 12),
                    this.scriptBlock.blockLines.get(i).color
            );
        }
    }
}
