package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.ExecutionContext;
import net.linkdarkar.testmod.scripting.ScriptLine;
import net.linkdarkar.testmod.scripting.ScriptVariable;
import net.linkdarkar.testmod.scripting.enums.MathOperator;
import net.minecraft.nbt.NbtCompound;

public class InstructionMathSimple extends ScriptLine {
    public String targetVarName;
    public ScriptVariable left;
    public ScriptVariable right;
    public MathOperator op;

    public InstructionMathSimple() {
        this.targetVarName = "";
        this.left = new ScriptVariable();
        this.op = MathOperator.ADD;
        this.right = new ScriptVariable();
        this.color = 0xAAAAFF;
    }
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

    @Override
    protected String getTypeID() {
        return "MATH_SIMPLE";
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.putString("target", targetVarName);
        nbt.put("left", left.toNbt());
        // Save Enum as String
        nbt.putString("op", op.name());
        nbt.put("right", right.toNbt());
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.targetVarName = nbt.getString("target");
        this.left = ScriptVariable.fromNbt(nbt.getCompound("left"));

        try {
            this.op = MathOperator.valueOf(nbt.getString("op"));
        } catch (IllegalArgumentException e) {
            this.op = MathOperator.ADD;
        }

        this.right = ScriptVariable.fromNbt(nbt.getCompound("right"));
    }
}
