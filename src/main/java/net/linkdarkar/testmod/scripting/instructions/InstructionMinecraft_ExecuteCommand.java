package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.scripting.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class InstructionMinecraft_ExecuteCommand extends ScriptLine {
    public String commandExpression;

    public InstructionMinecraft_ExecuteCommand() {
        this.commandExpression = "\"say Hello World\"";
        this.color = 0xFFAA00;
    }

    public InstructionMinecraft_ExecuteCommand(String cmd) {
        this.commandExpression = cmd;
        this.color = 0xFFAA00;
    }

    @Override
    public String GetLineAsPlainText() {
        return "Command: " + commandExpression;
    }

    @Override
    public String GetLineHandle()
    {
        return "MC /cmd";
    }

    @Override
    public List<String> Validate() {
        List<String> errors = new ArrayList<>();
        if (commandExpression == null || commandExpression.trim().isEmpty()) {
            errors.add("Command expression cannot be empty.");
        } else {
            try {
                ExpressionEvaluator.evaluate(commandExpression, new ExecutionContext(null));
            } catch (Exception e) {
                errors.add("Invalid expression syntax: " + e.getMessage());
            }
        }
        return errors;
    }

    @Override
    public Object Execute(ExecutionContext context) {
        if (context.executorEntity == null) return null;
        World world = context.executorEntity.getWorld();

        if (world instanceof ServerWorld serverWorld) {
            String finalCommand = "";
            try {
                Object result = ExpressionEvaluator.evaluate(commandExpression, context);
                finalCommand = result.toString();

                if (finalCommand.startsWith("/")) {
                    finalCommand = finalCommand.substring(1);
                }
            } catch (Exception e) {
                TestMod.LOGGER.error("Failed to evaluate command expression: {}", e.getMessage());
                return null;
            }

            MinecraftServer server = serverWorld.getServer();
            ServerCommandSource source = context.executorEntity.getCommandSource();

//            // Elevate permissions to level 4 (Server OP) or the script will only be able to run commands the executing player has permissions to run
//            source = source.withLevel(4);

            server.getCommandManager().executeWithPrefix(source, finalCommand);
        }
        return null;
    }

    @Override
    protected String getTypeID() {
        return "MC_COMMAND";
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.putString("cmd", commandExpression);
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.commandExpression = nbt.getString("cmd");
    }
}