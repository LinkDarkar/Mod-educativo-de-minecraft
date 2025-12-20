package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.ExecutionContext;
import net.linkdarkar.testmod.scripting.ScriptLine;
import net.linkdarkar.testmod.scripting.ScriptVariable;
import net.linkdarkar.testmod.scripting.enums.MathOperator;

public class InstructionMathSimple extends ScriptLine {
    public String targetVarName;
    public ScriptVariable left;
    public ScriptVariable right;
    public MathOperator op;

    public InstructionMathSimple(String target, ScriptVariable l, MathOperator op, ScriptVariable r) {
        this.targetVarName = target;
        this.left = l;
        this.op = op;
        this.right = r;
        this.color = 0xAAAAFF;
    }

    @Override
    public String GetAsText() {
        return targetVarName + " = " + left.variableName + " " + op.toString() + " " + right.variableName;
    }

    @Override
    public void Execute(ExecutionContext c) {
        Object lObj = left.GetResolvedValue(c);
        Object rObj = right.GetResolvedValue(c);

        double lVal = 0;
        double rVal = 0;

        try {
            lVal = Double.parseDouble(lObj.toString());
            rVal = Double.parseDouble(rObj.toString());
        } catch (Exception e) {
            // Non number math errors
        }

        double result = 0;

        switch (op) {
            case ADD: result = lVal + rVal; break;
            case SUBTRACT: result = lVal - rVal; break;
            case MULTIPLY: result = lVal * rVal; break;
            // TODO: Return Infinite when dividing by 0
            case DIVIDE: result = rVal == 0 ? 0 : (lVal / rVal); break;
            case ASSIGN: result = rVal; break;
        }

        c.SetVar(targetVarName, result);
    }
}
