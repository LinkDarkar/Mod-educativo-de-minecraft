package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class InstructionPrint extends ScriptLine {
    public String message;

    public InstructionPrint(String msg) {
        this.message = msg;
        this.color = 0xFFFFFF;
    }

    public InstructionPrint() {
        this.message = "Value: {x}";
        this.color = 0xFFFFFF;
    }

    @Override
    public String GetAsText() {
        return "PRINT " + this.message;
    }

    @Override
    public void Execute(ExecutionContext context) {
        String finalMsg = this.message;

        // Simple interpolation: replaces {varName} with value
        for (String key : context.variables.keySet()) {
            String placeholder = "{" + key + "}";
            if (finalMsg.contains(placeholder)) {
                finalMsg = finalMsg.replace(placeholder, context.variables.get(key).toString());
            }
        }

        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal(finalMsg), false);
        }
    }
}