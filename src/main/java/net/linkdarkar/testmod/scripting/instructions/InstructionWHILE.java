package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.*;

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
        return "WHILE (CONDITION)";
    }

    @Override
    public void Execute(ExecutionContext context) {
        int safety = 0;
        while (condition != null && condition.Evaluate(context)) {
            safety++;
            if (1000 < safety) break;
            loopBlock.Execute(context);
        }
    }
}