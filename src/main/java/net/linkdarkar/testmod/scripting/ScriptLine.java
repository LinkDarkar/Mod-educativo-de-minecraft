package net.linkdarkar.testmod.scripting;

import net.linkdarkar.testmod.scripting.instructions.*;
import net.minecraft.nbt.NbtCompound;

public abstract class ScriptLine {
    public Object instruction;
    public String text;
    public int color = 0xFFFFFF;

    public abstract String GetAsText();

    public abstract void Execute(ExecutionContext context);

    // Save to NBT stuff
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("type", getTypeID());
        return nbt;
    }

    protected abstract String getTypeID();

    public static ScriptLine fromNbt(NbtCompound nbt) {
        String type = nbt.getString("type");
        ScriptLine line = switch (type) {
            case "BLOCK" -> new ScriptBlock();
            case "IF" -> new InstructionIF();
            case "WHILE" -> new InstructionWHILE();
            case "MATH" -> new InstructionMath();
            case "MATH_SIMPLE" -> new InstructionMathSimple();
            case "PRINT" -> new InstructionPrint();
            case "FOLLOW_ENTITY" -> new InstructionEntity_FollowEntity();
            // TODO: Add other cases here
            default -> null;
        };

        if (line != null) {
            line.loadNbt(nbt);
        }
        return line;
    }

    public abstract void loadNbt(NbtCompound nbt);
}
