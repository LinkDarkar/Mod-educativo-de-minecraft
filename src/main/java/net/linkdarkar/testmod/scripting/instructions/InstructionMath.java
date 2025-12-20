package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.*;
import net.linkdarkar.testmod.scripting.enums.MathOperator;

public class InstructionMath extends ScriptLine {
    public String targetVarName;
    public String expression;

    public InstructionMath(String target, String expr) {
        this.targetVarName = target;
        this.expression = expr;
        this.color = 0xAAAAFF;
    }

    @Override
    public String GetAsText() {
        return targetVarName + " = " + expression;
    }

    @Override
    public void Execute(ExecutionContext c) {
        try {
            double result = ExpressionEvaluator.evaluate(expression, c);
            c.SetVar(targetVarName, result);
        } catch (Exception e) {
            // If it fails (e.g., assignment of string literal), fallback to raw string
            c.SetVar(targetVarName, expression);
        }
    }
}

