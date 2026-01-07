package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.*;
import net.linkdarkar.testmod.scripting.enums.ComparisonOperator;
import net.minecraft.nbt.NbtCompound;

public class InstructionIF extends ScriptLine {
    public ScriptCondition condition;
    public ScriptBlock trueBlock = new ScriptBlock();

    public InstructionIF() {
        this.color = 0xFF0000;
        this.condition = new ScriptCondition();
    }
    public InstructionIF(ScriptCondition cond) {
        this.condition = cond;
        this.color = 0xFF0000;
    }

    public String GetAsText() {
        return "IF";
    }

    @Override
    public void Execute(ExecutionContext context) {
        if (condition != null && condition.Evaluate(context)) {
            trueBlock.Execute(context);
        }
    }

    // NBT stuff
    @Override
    protected String getTypeID() {
        return "IF";
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.put("cond", condition.toNbt());
        nbt.put("trueBlock", trueBlock.toNbt());
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.condition = new ScriptCondition();
        this.condition.loadNbt(nbt.getCompound("cond"));

        this.trueBlock = (ScriptBlock) ScriptLine.fromNbt(nbt.getCompound("trueBlock"));
    }
}