package net.linkdarkar.testmod.scripting;

import net.linkdarkar.testmod.scripting.enums.ComparisonOperator;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;

public class ScriptCondition {
    public String leftExpression;
    public ComparisonOperator op;
    public String rightExpression;

    public ScriptCondition() {
        this.leftExpression = "";
        this.op = ComparisonOperator.EQUALS;
        this.rightExpression = "";
    }
    public ScriptCondition(String lExp, ComparisonOperator op, String rExp) {
        this.leftExpression = lExp;
        this.op = op;
        this.rightExpression = rExp;
    }

    public boolean Evaluate(ExecutionContext ctx) {
        if (leftExpression.isEmpty() || rightExpression.isEmpty()) {
            return false;
        }

        Object leftObj = ExpressionEvaluator.evaluate(leftExpression, ctx);
        Object rightObj = ExpressionEvaluator.evaluate(rightExpression, ctx);

        boolean areNumbers = (leftObj instanceof Number) && (rightObj instanceof Number);
        double lNum = areNumbers ? ((Number) leftObj).doubleValue() : 0;
        double rNum = areNumbers ? ((Number) rightObj).doubleValue() : 0;

        return switch (op)
        {
            case EQUALS -> {
                if (areNumbers) yield Math.abs(lNum - rNum) < 0.0001;
                yield leftObj.toString().equals(rightObj.toString());
            }
            case DIFFERENT -> {
                if (areNumbers) yield 0.0001 < Math.abs(lNum - rNum);
                yield !leftObj.toString().equals(rightObj.toString());
            }
            case LESS_THAN -> areNumbers && lNum < rNum;
            case LESS_OR_EQUAL_THAN -> areNumbers && lNum <= rNum;
//            case GREATER_THAN -> areNumbers && lNum > rNum;
//            case GREATER_OR_EQUAL_THAN -> areNumbers && lNum >= rNum;
            default -> false;
        };
    }

    public List<String> Validate() {
        List<String> errors = new ArrayList<>();
        ExecutionContext dummyCtx = new ExecutionContext(null);

        if (leftExpression == null || leftExpression.trim().isEmpty()) {
            errors.add("Left expression cannot be empty.");
        } else {
            try {
                ExpressionEvaluator.evaluate(leftExpression, dummyCtx);
            } catch (Exception e) {
                errors.add("Invalid syntax in left expression: " + e.getMessage());
            }
        }

        if (rightExpression == null || rightExpression.trim().isEmpty()) {
            errors.add("Right expression cannot be empty.");
        } else {
            try {
                ExpressionEvaluator.evaluate(rightExpression, dummyCtx);
            } catch (Exception e) {
                errors.add("Invalid syntax in right expression: " + e.getMessage());
            }
        }

        return errors;
    }

    // Save NBT stuff
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("left", leftExpression);
        nbt.putString("op", op.name());
        nbt.putString("right", rightExpression);
        return nbt;
    }

    public void loadNbt(NbtCompound nbt) {
        this.leftExpression = nbt.getString("left");
        this.op = ComparisonOperator.valueOf(nbt.getString("op"));
        this.rightExpression = nbt.getString("right");
    }
}
