package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.scripting.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InstructionEntity_DistanceFromEntity extends ScriptLine {
    public String targetVarName;
    public String targetUUIDExp;

    public InstructionEntity_DistanceFromEntity() {
        this.targetVarName = "dist";
        this.targetUUIDExp = "\"\"";
        this.color = 0x55FFFF;
    }

    @Override
    public String GetLineHandle() { return "Dist ="; }

    @Override
    public String GetLineAsPlainText() {
        return targetVarName + " = DistanceFromEntity(" + targetUUIDExp + ")";
    }

    @Override
    public List<String> Validate() {
        List<String> errors = new ArrayList<>();
        if (targetVarName == null || targetVarName.trim().isEmpty()) {
            errors.add("Target variable name cannot be empty.");
        }
        if (targetUUIDExp == null || targetUUIDExp.trim().isEmpty()) {
            errors.add("Target UUID expression cannot be empty.");
        } else {
            try {
                ExpressionEvaluator.evaluate(targetUUIDExp, new ExecutionContext(null));
            } catch (Exception e) {
                errors.add("Invalid UUID expression: " + e.getMessage());
            }
        }
        return errors;
    }

    @Override
    public Object Execute(ExecutionContext c) {
        if (c.isSimulation || c.executorEntity == null) return null;

        net.minecraft.world.World world = c.executorEntity.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            try {
                Object uuidRes = ExpressionEvaluator.evaluate(targetUUIDExp, c);
                String uuidStr = uuidRes.toString().replace("\"", "");

                UUID uuid = UUID.fromString(uuidStr);
                Entity target = serverWorld.getEntity(uuid);

                if (target != null) {
                    double dist = c.executorEntity.distanceTo(target);
                    c.SetVar(targetVarName, dist);
                } else {
                    c.SetVar(targetVarName, -1.0); // Target not found or dead
                }
            } catch (Exception e) {
                TestMod.LOGGER.error("Failed to execute DistanceFromEntity: " + e.getMessage());
            }
        }
        return null;
    }

    @Override
    protected String getTypeID() { return "E_DIST_ENTITY"; }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.putString("target", targetVarName);
        nbt.putString("uuidExp", targetUUIDExp);
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.targetVarName = nbt.getString("target");
        this.targetUUIDExp = nbt.getString("uuidExp");
    }
}