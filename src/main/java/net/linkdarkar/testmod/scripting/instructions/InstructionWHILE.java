package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.*;
import net.minecraft.nbt.NbtCompound;

import java.util.List;

public class InstructionWHILE extends ScriptLine {
    public ScriptCondition condition;
    public ScriptBlock loopBlock = new ScriptBlock();

    public InstructionWHILE(ScriptCondition cond) {
        this.condition = cond;
        this.color = 0x66FF66;
    }

    public InstructionWHILE() {
        this.color = 0x66FF66;
        this.condition = new ScriptCondition();
    }

    @Override
    public String GetAsText() {
        return "WHILE [" + condition.leftExpression + "] " + condition.op.name() + " [" + condition.rightExpression + "]";
    }

    @Override
    public List<String> Validate() {
        return condition.Validate();
    }

    @Override
    public List<ScriptBlock> getChildBlocks() {
        return List.of(loopBlock);
    }

    @Override
    public Object Execute(ExecutionContext context) {
        int safety = 0;
        while (condition != null && condition.Evaluate(context)) {
            safety++;
            if (1000 < safety) break;
            loopBlock.Execute(context);
        }
        return null;
    }

    @Override
    protected String getTypeID() {
        return "WHILE";
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.put("cond", condition.toNbt());
        nbt.put("loopBlock", loopBlock.toNbt());
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.condition = new ScriptCondition();
        this.condition.loadNbt(nbt.getCompound("cond"));

        this.loopBlock = (ScriptBlock) ScriptLine.fromNbt(nbt.getCompound("loopBlock"));
    }
}