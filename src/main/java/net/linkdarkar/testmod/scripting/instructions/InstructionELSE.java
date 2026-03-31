package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.*;
import net.minecraft.nbt.NbtCompound;
import java.util.ArrayList;
import java.util.List;

public class InstructionELSE extends ScriptLine {
    public ScriptBlock elseBlock = new ScriptBlock();

    public InstructionELSE() {
        this.color = 0xFFFFAA00;
    }

    @Override
    public String GetAsText() {
        return "ELSE";
    }

    @Override
    public List<String> Validate() {
        return new ArrayList<>();
    }

    @Override
    public List<ScriptBlock> getChildBlocks() {
        return List.of(elseBlock);
    }

    @Override
    public Object Execute(ExecutionContext context) {
        // The Else doesn't execute when read, instead the previous IF executes it
        return null;
    }

    @Override
    protected String getTypeID() {
        return "ELSE";
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.put("elseBlock", elseBlock.toNbt());
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.elseBlock = (ScriptBlock) ScriptLine.fromNbt(nbt.getCompound("elseBlock"));
    }
}