package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.*;
import net.linkdarkar.testmod.scripting.enums.MathOperator;
import net.minecraft.nbt.NbtCompound;

public class InstructionMath extends ScriptLine {
    public String targetVarName;
    public String expression;

    public InstructionMath()
    {
        this.targetVarName = "";
        this.expression = "";
        this.color = 0xAAAAFF;
    }
    public InstructionMath(String target, String expr) {
        this.targetVarName = target;
        this.expression = expr;
    }

    @Override
    public String GetAsText() {
        return targetVarName + " = " + expression;
    }

    @Override
    public void Execute(ExecutionContext c) {
        try {
            Object result = ExpressionEvaluator.evaluate(expression, c);
            c.SetVar(targetVarName, result);
        } catch (Exception e) {
            // If it fails (e.g.: assignment of string literal), fallback to raw string
            c.SetVar(targetVarName, expression);
        }
    }

    // NBT stuff
    @Override
    protected String getTypeID() {
        return "MATH";
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.putString("target", targetVarName);
        nbt.putString("expr", expression);
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.targetVarName = nbt.getString("target");
        this.expression = nbt.getString("expr");
    }
}

