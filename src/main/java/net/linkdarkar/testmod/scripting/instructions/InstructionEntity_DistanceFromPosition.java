package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.scripting.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class InstructionEntity_DistanceFromPosition extends ScriptLine {
    public String targetVarName;
    public String xExp, yExp, zExp;

    public InstructionEntity_DistanceFromPosition() {
        this.targetVarName = "dist";
        this.xExp = "0";
        this.yExp = "0";
        this.zExp = "0";
        this.color = 0x55FFFF;
    }

    @Override
    public String GetLineHandle() { return "Dist ="; }

    @Override
    public String GetLineAsPlainText() {
        return targetVarName + " = DistanceFromPos(" + xExp + ", " + yExp + ", " + zExp + ")";
    }

    @Override
    public List<String> Validate() {
        List<String> errors = new ArrayList<>();
        ExecutionContext dummyCtx = new ExecutionContext(null);

        if (targetVarName == null || targetVarName.trim().isEmpty()) {
            errors.add("Target variable name cannot be empty.");
        }

        String[] fields = {xExp, yExp, zExp};
        String[] labels = {"X", "Y", "Z"};

        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == null || fields[i].trim().isEmpty()) {
                errors.add(labels[i] + " cannot be empty.");
            } else {
                try {
                    ExpressionEvaluator.evaluate(fields[i], dummyCtx);
                } catch (Exception e) {
                    errors.add("Invalid syntax in " + labels[i] + ": " + e.getMessage());
                }
            }
        }
        return errors;
    }

    @Override
    public Object Execute(ExecutionContext c) {
        if (c.isSimulation || c.executorEntity == null) return null;

        try {
            double x = getDouble(c, xExp);
            double y = getDouble(c, yExp);
            double z = getDouble(c, zExp);

            Vec3d targetPos = new Vec3d(x, y, z);
            double dist = c.executorEntity.getPos().distanceTo(targetPos);

            c.SetVar(targetVarName, dist);
        } catch (Exception e) {
            TestMod.LOGGER.error("Failed to execute DistanceFromPos: " + e.getMessage());
        }
        return null;
    }

    private double getDouble(ExecutionContext ctx, String exp) {
        try {
            Object res = ExpressionEvaluator.evaluate(exp, ctx);
            if (res instanceof Number) return ((Number) res).doubleValue();
            return Double.parseDouble(res.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected String getTypeID() { return "E_DIST_POS"; }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.putString("target", targetVarName);
        nbt.putString("x", xExp);
        nbt.putString("y", yExp);
        nbt.putString("z", zExp);
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.targetVarName = nbt.getString("target");
        this.xExp = nbt.getString("x");
        this.yExp = nbt.getString("y");
        this.zExp = nbt.getString("z");
    }
}