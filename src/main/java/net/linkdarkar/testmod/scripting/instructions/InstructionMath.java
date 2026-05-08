package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.*;
import net.linkdarkar.testmod.scripting.enums.MathOperator;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;

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
    public String GetLineHandle()
    {
        return "       =";
    }

    @Override
    public String GetLineAsPlainText() {
        return targetVarName + " = " + expression;
    }

    @Override
    public List<String> Validate() {
        List<String> errors = new ArrayList<>();
        if (targetVarName == null || targetVarName.trim().isEmpty()) {
            errors.add("Target variable name cannot be empty.");
        }
        if (expression == null || expression.trim().isEmpty()) {
            errors.add("Math expression cannot be empty.");
        } else {
            try {
                ExpressionEvaluator.evaluate(expression, new ExecutionContext(null));
            } catch (Exception e) {
                errors.add("Invalid math syntax: " + e.getMessage());
            }
        }

        return errors;
    }

    @Override
    public Object Execute(ExecutionContext c) {
        String cleanName = targetVarName != null ? targetVarName.trim() : "";
        if (cleanName.isEmpty()) return null;

        try {
            Object result = ExpressionEvaluator.evaluate(expression, c);
            c.SetVar(cleanName, result);
            return result;
        } catch (Exception e) {
            // If it fails (e.g.: assignment of string literal), fallback to raw string
            c.SetVar(cleanName, expression);
            return expression;
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

