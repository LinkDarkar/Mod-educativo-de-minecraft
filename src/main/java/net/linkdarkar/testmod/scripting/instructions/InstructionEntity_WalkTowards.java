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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InstructionEntity_WalkTowards extends ScriptLine {
    public String xExp = "0", yExp = "0", zExp = "0", speedExp = "1";

    public InstructionEntity_WalkTowards()
    {
        this.color = 0x0080FF;
    }

    @Override
    public String GetLineAsPlainText()
    {
        return "Walk Forward";
    }

    @Override
    public String GetLineHandle()
    {
        return "Walk Towards";
    }

    @Override
    public List<String> Validate() {
        List<String> errors = new ArrayList<>();
        String[] fields = {xExp, yExp, zExp, speedExp};
        String[] names = {"X", "Y", "Z", "Speed"};

        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == null || fields[i].trim().isEmpty()) {
                errors.add(names[i] + " cannot be empty.");
            } else {
                try {
                    ExpressionEvaluator.evaluate(fields[i], new ExecutionContext(null));
                } catch (Exception e) {
                    errors.add("Invalid " + names[i] + " syntax: " + e.getMessage());
                }
            }
        }
        return errors;
    }

    @Override
    public Object Execute(ExecutionContext context) {
        try {
            double x = ((Number) ExpressionEvaluator.evaluate(xExp, context)).doubleValue();
            double y = ((Number) ExpressionEvaluator.evaluate(yExp, context)).doubleValue();
            double z = ((Number) ExpressionEvaluator.evaluate(zExp, context)).doubleValue();
            double speed = ((Number) ExpressionEvaluator.evaluate(speedExp, context)).doubleValue();

            // Log action for simulation
            if (context.isSimulation) {
                context.recordedActions.add("ACTION_WALK_TOWARDS:" + x + "," + y + "," + z + "@" + speed);
                return null;
            }

            TestMod.LOGGER.info("Executing Walk Towards Instruction...");

            if (context.executorEntity == null) return null;
            World world = context.executorEntity.getWorld();

            if (world instanceof ServerWorld serverWorld) {
                boolean success = FunctionCaller.walkTowards(context.executorEntity, 1, new Vec3d(x, y, z));

                System.out.println("Walk towards "+ new Vec3d(x, y, z));

                TestMod.LOGGER.info("Walk Towards command sent. Success: {}", success);
            }
            else {
                TestMod.LOGGER.warn("Script tried to run on Client Side. Ignoring.");
            }
        } catch (Exception e) {
            TestMod.LOGGER.error("Error executing Walk Towards: {}", e.getMessage());
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
        return "E_WALK_TOWARDS";
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.putString("x", xExp);
        nbt.putString("y", yExp);
        nbt.putString("z", zExp);
        nbt.putString("speed", speedExp);
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.xExp = nbt.getString("x");
        this.yExp = nbt.getString("y");
        this.zExp = nbt.getString("z");
        this.speedExp = nbt.getString("speed");
    }
}
