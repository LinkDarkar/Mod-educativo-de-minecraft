package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.*;
import net.linkdarkar.testmod.scripting.enums.ComparisonOperator;

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
}