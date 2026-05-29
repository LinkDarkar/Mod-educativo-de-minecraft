package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.scripting.ExecutionContext;
import net.linkdarkar.testmod.scripting.ExpressionEvaluator;
import net.linkdarkar.testmod.scripting.ScriptLine;
import net.linkdarkar.testmod.scripting.functionCaller.FunctionCaller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InstructionEntity_WalkForward extends ScriptLine {

    public String speedExp;

    public InstructionEntity_WalkForward() {
        this.color = 0x0080FF;
        this.speedExp = "1";
    }

    @Override
    public String GetLineAsPlainText() {
        return "Walk Forward";
    }

    @Override
    public String GetLineHandle()
    {
        return "Walk Forward";
    }

    @Override
    public List<String> Validate() {
        List<String> errors = new ArrayList<>();
        if (speedExp == null || speedExp.trim().isEmpty()) {
            errors.add("Speed cannot be empty.");
        } else {
            try {
                ExpressionEvaluator.evaluate(speedExp, new ExecutionContext(null));
            } catch (Exception e) {
                errors.add("Invalid speed syntax: " + e.getMessage());
            }
        }
        return errors;
    }

    @Override
    public Object Execute(ExecutionContext context) {
        try {
            Object result = ExpressionEvaluator.evaluate(speedExp, context);
            double speed = result instanceof Number ? ((Number) result).doubleValue() : 1.0;

            // Log action for simulation
            if (context.isSimulation) {
                context.recordedActions.add("ACTION_WALK_FORWARD:" + speed);
                return null;
            }

            TestMod.LOGGER.info("Executing Walk Forward Instruction...");

            if (context.executorEntity == null) return null;
            World world = context.executorEntity.getWorld();

            if (world instanceof ServerWorld serverWorld) {
                boolean success = FunctionCaller.walkForward(context.executorEntity, speed, 10);
                TestMod.LOGGER.info("Walk Forward command sent. Success: {}", success);
            }
            else {
                TestMod.LOGGER.warn("Script tried to run on Client Side. Ignoring.");
            }
        } catch (Exception e) {
            TestMod.LOGGER.error("Error executing Walk Forward: {}", e.getMessage());
        }
        return null;
    }


    private Entity findEntityByUUID(String uuidString) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return null;

        try {
            UUID uuid = UUID.fromString(uuidString);
            for (Entity entity : client.world.getEntities()) {
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

    // NBT stuff
    @Override
    protected String getTypeID() {
        return "E_WALK_FORWARD";
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.putString("speed", speedExp);
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.speedExp = nbt.getString("speed");
    }
}
