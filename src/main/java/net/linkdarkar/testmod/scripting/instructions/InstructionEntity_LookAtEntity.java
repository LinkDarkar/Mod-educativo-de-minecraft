package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.scripting.ExecutionContext;
import net.linkdarkar.testmod.scripting.ExpressionEvaluator;
import net.linkdarkar.testmod.scripting.ScriptLine;
import net.linkdarkar.testmod.scripting.functionCaller.FunctionCaller;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InstructionEntity_LookAtEntity extends ScriptLine {
    public String targetUUIDExp;

    public InstructionEntity_LookAtEntity() {
        this.color = 0x0080FF;
        this.targetUUIDExp = "";
    }

    public InstructionEntity_LookAtEntity(String target) {
        this.color = 0x0080FF;
        this.targetUUIDExp = target;
    }

    @Override
    public String GetLineAsPlainText() {
        return "Look At " + targetUUIDExp;
    }

    public String GetLineHandle() {
        return "Look At";
    }

    @Override
    public List<String> Validate() {
        List<String> errors = new ArrayList<>();

        if (targetUUIDExp == null || targetUUIDExp.trim().isEmpty()) {
            errors.add("Target UUID expression cannot be empty.");
            return errors;
        }

        try {
            ExpressionEvaluator.evaluate(targetUUIDExp, new ExecutionContext(null));
        } catch (Exception e) {
            errors.add("Invalid UUID expression: " + e.getMessage());
        }

        return errors;
    }

    public Object Execute(ExecutionContext context) {
        if (targetUUIDExp == null || targetUUIDExp.isEmpty()) return null;

        try {
            // Evaluate the expression to get the actual UUID string
            Object res = ExpressionEvaluator.evaluate(targetUUIDExp, context);
            String cleanUUID = res.toString().replace("\"", "").trim();

            if (cleanUUID.isEmpty()) return null;

            // Log action for simulation
            if (context.isSimulation) {
                context.recordedActions.add("ACTION_LOOK_AT:" + cleanUUID);
                return null;
            }

            TestMod.LOGGER.info("Executing Look At Entity Instruction...");

            if (context.executorEntity == null) return null;
            World world = context.executorEntity.getWorld();

            if (world instanceof ServerWorld serverWorld) {
                UUID uuid = UUID.fromString(cleanUUID);

                boolean success = FunctionCaller.lookAtEntity(serverWorld, context.executorEntity.getUuid(), uuid);

                TestMod.LOGGER.info("Look at command sent. Success: {}", success);
            }
            else {
                TestMod.LOGGER.warn("Script tried to run on Client Side. Ignoring.");
            }
        } catch (IllegalArgumentException e) {
            TestMod.LOGGER.error("Invalid UUID format in script: {}", targetUUIDExp);
        } catch (Exception e) {
            TestMod.LOGGER.error("Error executing Look at: {}", e.getMessage());
        }
        return null;
    }

    @Override
    protected String getTypeID() {
        return "E_LOOK_AT";
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.putString("targetUUID", targetUUIDExp != null ? targetUUIDExp : "");
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.targetUUIDExp = nbt.getString("targetUUID");

        // Backward compatibility
        if (this.targetUUIDExp != null && !this.targetUUIDExp.startsWith("\"") && this.targetUUIDExp.contains("-")) {
            this.targetUUIDExp = "\"" + this.targetUUIDExp + "\"";
        }
    }
}