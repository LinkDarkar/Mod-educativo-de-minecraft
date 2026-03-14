package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class InstructionPrint extends ScriptLine {
    public String message;

    public InstructionPrint(String msg) {
        this.message = msg;
        this.color = 0xFFFFFF;
    }

    public InstructionPrint() {
        this.message = "\"Value: \" + x";
        this.color = 0xFFFFFF;
    }

    @Override
    public String GetAsText() {
        return "PRINT " + message;
    }

    @Override
    public List<String> Validate() {
        List<String> errors = new ArrayList<>();
        if (message == null || message.trim().isEmpty()) {
            errors.add("Print message cannot be empty.");
        } else {
            try {
                ExpressionEvaluator.evaluate(message, new ExecutionContext(null));
            } catch (Exception e) {
                errors.add("Invalid print syntax: " + e.getMessage());
            }
        }
        return errors;
    }

    @Override
    public Object Execute(ExecutionContext context) {
        String finalMsg;

        try {
            Object result = ExpressionEvaluator.evaluate(message, context);

            if (result instanceof Double) {
                double d = (Double) result;
                if (d == (long) d) {
                    finalMsg = String.format("%d", (long) d);
                } else {
                    finalMsg = result.toString();
                }
            } else {
                finalMsg = result.toString();
            }
        } catch (Exception e) {
            finalMsg = "Error: " + e.getMessage();
        }

        if (context.isSimulation) {
            context.printedMessages.add(finalMsg);
            return null;
        }

        // if (MinecraftClient.getInstance().player != null) {
        //     MinecraftClient.getInstance().player.sendMessage(Text.literal(finalMsg), false);
        // }

        if (context.executorEntity != null)
        {
            World world = context.executorEntity.getWorld();

            if (!world.isClient)
            {
                Text textObj = Text.literal((finalMsg));

                for (PlayerEntity player : world.getPlayers())
                {
                    player.sendMessage(textObj, false);
                }
            }
        }

        return null;
    }

    @Override
    protected String getTypeID() {
        return "PRINT";
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.putString("msg", message);
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.message = nbt.getString("msg");
    }
}