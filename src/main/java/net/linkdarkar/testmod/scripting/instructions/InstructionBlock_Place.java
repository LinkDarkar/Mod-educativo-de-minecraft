package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.scripting.*;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class InstructionBlock_Place extends ScriptLine {
    public String xExp;
    public String yExp;
    public String zExp;

    public InstructionBlock_Place() {
        this.xExp = "0";
        this.yExp = "0";
        this.zExp = "0";
        this.color = 0x00FF80;
    }

    public InstructionBlock_Place(String x, String y, String z) {
        this.xExp = x;
        this.yExp = y;
        this.zExp = z;
        this.color = 0x00FF80;
    }

    @Override
    public String GetLineAsPlainText() {
        return "PlaceBlock " + xExp + " " + yExp + " " + zExp;
    }

    @Override
    public String GetLineHandle()
    {
        return "Place Block";
    }

    @Override
    public List<String> Validate() {
        List<String> errors = new ArrayList<>();
        ExecutionContext dummyCtx = new ExecutionContext(null);

        String[] fields = {xExp, yExp, zExp};
        String[] labels = {"X", "Y", "Z"};

        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == null || fields[i].trim().isEmpty()) {
                errors.add("Coordinate " + labels[i] + " cannot be empty.");
            } else {
                try {
                    ExpressionEvaluator.evaluate(fields[i], dummyCtx);
                } catch (Exception e) {
                    errors.add("Invalid syntax in " + labels[i] + " coordinate: " + e.getMessage());
                }
            }
        }
        return errors;
    }

    @Override
    public Object Execute(ExecutionContext context) {
        if (context.executorEntity == null) return null;

        // If it's a simulation, skips changing the world
        if (context.isSimulation) {
            return null;
        }

        // Thwe block placement must happen on the server. If done on the client, it creates a "ghost block"
        net.minecraft.world.World world = context.executorEntity.getWorld();
        if (world.isClient()) return null;

        try {
            double xVal = getDouble(context, xExp);
            double yVal = getDouble(context, yExp);
            double zVal = getDouble(context, zExp);

            BlockPos pos = new BlockPos((int) xVal, (int) yVal, (int) zVal);

            // For now, hardcoded to Grass Block
            // The flag '3' means: Update neighbors (1) + Notify clients (2)
            world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState(), 3);

            TestMod.LOGGER.info("Placed block");

        } catch (Exception e) {
            TestMod.LOGGER.info("Failed to place block: " + e.getMessage());
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
    protected String getTypeID() {
        return "PLACE_BLOCK";
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.putString("x", xExp);
        nbt.putString("y", yExp);
        nbt.putString("z", zExp);
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.xExp = nbt.getString("x");
        this.yExp = nbt.getString("y");
        this.zExp = nbt.getString("z");
    }
}