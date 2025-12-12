package net.linkdarkar.testmod.scripting;

import net.linkdarkar.testmod.scripting.enums.ComparisonOperator;

public class ScriptCondition {
    public ScriptVariable lVar;
    public ComparisonOperator op;
    public ScriptVariable rVar;

    public ScriptCondition() {
        this.lVar = new ScriptVariable("");
        this.op = ComparisonOperator.EQUALS;
        this.rVar = new ScriptVariable("");
    }
    public ScriptCondition(ScriptVariable lVar, ComparisonOperator op, ScriptVariable rVar) {
        this.lVar = lVar;
        this.op = op;
        this.rVar = rVar;
    }

    public boolean Evaluate(ExecutionContext ctx) {
        if (lVar.GetOriginalValue().isEmpty() || rVar.GetOriginalValue().isEmpty()) {
            return false;
        }
        Object leftObj = lVar.GetResolvedValue(ctx);
        Object rightObj = rVar.GetResolvedValue(ctx);

        double lNum = 0, rNum = 0;
        boolean isNumeric = false;

        try {
            lNum = Double.parseDouble(leftObj.toString());
            rNum = Double.parseDouble(rightObj.toString());
            isNumeric = true;
        } catch (NumberFormatException e) {
            isNumeric = false;
        }

        return switch (op) {
            case EQUALS -> {
                if (isNumeric) yield Math.abs(lNum - rNum) < 0.0001;
                yield leftObj.equals(rightObj);
            }
            case DIFFERENT -> {
                if (isNumeric) yield 0.0001 < Math.abs(lNum - rNum);
                yield !leftObj.equals(rightObj);
            }
            case LESS_THAN -> isNumeric && lNum < rNum;
            case GREATER_THAN -> isNumeric && lNum > rNum;
            default -> false;
        };
    }
}
