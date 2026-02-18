package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.scripting.*;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public class InstructionBlock_Place extends ScriptLine {
    public String xExp;
    public String yExp;
    public String zExp;

    public InstructionBlock_Place() {
        this.xExp = "0";
        this.yExp = "0";
        this.zExp = "0";
        this.color = 0x55FFFF;
    }

    public InstructionBlock_Place(String x, String y, String z) {
        this.xExp = x;
        this.yExp = y;
        this.zExp = z;
        this.color = 0x55FFFF;
    }

    @Override
    public String GetAsText() {
        return "PlaceBlock " + xExp + " " + yExp + " " + zExp;
    }

    @Override
    public void Execute(ExecutionContext context) {
        if (context.executorEntity == null) return;

        // Thwe block placement must happen on the server. If done on the client, it creates a "ghost block"
        net.minecraft.world.World world = context.executorEntity.getWorld();
        if (world.isClient()) return;

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