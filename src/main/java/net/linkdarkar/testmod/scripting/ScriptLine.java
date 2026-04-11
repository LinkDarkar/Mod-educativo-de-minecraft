package net.linkdarkar.testmod.scripting;

import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.scripting.instructions.*;
import net.minecraft.nbt.NbtCompound;

import java.util.List;
import java.util.ArrayList;

public abstract class ScriptLine {
    public Object instruction;
    public String text;
    public int color = 0xFFFFFF;

    public abstract String GetLineHandle();
    public abstract String GetLineAsPlainText();

    public abstract Object Execute(ExecutionContext context);

    public abstract List<String> Validate();

    public List<ScriptBlock> getChildBlocks() {
        return new ArrayList<>();
    }

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
            case "ELSE" -> new InstructionELSE();
            case "WHILE" -> new InstructionWHILE();
            case "MATH" -> new InstructionMath();
            case "VAR_ASSIGN" -> new InstructionVarAssign();
            case "PRINT" -> new InstructionPrint();
            case "MC_COMMAND" -> new InstructionMinecraft_ExecuteCommand();
            case "E_FOLLOW_ENTITY" -> new InstructionEntity_FollowEntity();
            case "E_WALK_TOWARDS" -> new InstructionEntity_WalkTowards();
            case "E_WALK_FORWARD" -> new InstructionEntity_WalkForward();
            // TODO: Add other cases here
            default -> null;
        };

        if (line != null) {
            line.loadNbt(nbt);
        }
        else
        {
            TestMod.LOGGER.info("Failed to load from NBT, type not recognized.");
        }
        return line;
    }

    public abstract void loadNbt(NbtCompound nbt);
}
