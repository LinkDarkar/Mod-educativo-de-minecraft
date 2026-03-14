package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.*;
import net.linkdarkar.testmod.scripting.enums.ComparisonOperator;
import net.minecraft.nbt.NbtCompound;

import java.util.List;

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

    @Override
    public String GetAsText() {
        return "IF [" + condition.leftExpression + "] " + condition.op.name() + " [" + condition.rightExpression + "]";
    }

    @Override
    public List<String> Validate() {
        return condition.Validate();
    }

    @Override
    public List<ScriptBlock> getChildBlocks() {
        return List.of(trueBlock);
    }

    @Override
    public Object Execute(ExecutionContext context) {
        if (condition != null && condition.Evaluate(context)) {
            trueBlock.Execute(context);
            return true;
        }
        return false;
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