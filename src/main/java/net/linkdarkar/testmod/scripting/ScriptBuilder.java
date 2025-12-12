package net.linkdarkar.testmod.scripting;

import net.linkdarkar.testmod.scripting.enums.ComparisonOperator;
import net.linkdarkar.testmod.scripting.enums.MathOperator;
import net.linkdarkar.testmod.scripting.instructions.*;
import java.util.Stack;

public class ScriptBuilder {
    public ScriptBlock mainScript = new ScriptBlock();
    private Stack<ScriptBlock> blockStack = new Stack<>();

    public ScriptBuilder() {
        blockStack.push(mainScript);
    }

    private void Add(ScriptLine line) {
        blockStack.peek().AddInstruction(line);
    }

    public ScriptBuilder DeclareVar(String name, Object initialValue) {
        ScriptVariable left = new ScriptVariable("0");
        ScriptVariable right = new ScriptVariable(initialValue.toString());

        InstructionMath instr = new InstructionMath(name, left, MathOperator.ASSIGN, right);
        Add(instr);
        return this;
    }

    public ScriptBuilder BeginWhile(String varNameL, ComparisonOperator op, String varNameR) {
        ScriptCondition condition = CreateCondition(varNameL, op, varNameR);
        InstructionWHILE whileInst = new InstructionWHILE(condition);

        Add(whileInst);
        blockStack.push(whileInst.loopBlock);

        return this;
    }

    public ScriptBuilder BeginIf(String varName, ComparisonOperator op, String varNameR) {
        ScriptCondition condition = CreateCondition(varName, op, varNameR);
        InstructionIF ifInst = new InstructionIF(condition);

        Add(ifInst);
        blockStack.push(ifInst.trueBlock);

        return this;
    }

    public ScriptBuilder EndBlock() {
        if (1 < blockStack.size())
            blockStack.pop();
        return this;
    }

    private ScriptCondition CreateCondition(String varNameL, ComparisonOperator op, String varNameR) {
        ScriptVariable left = new ScriptVariable(varNameL);
        ScriptVariable right = new ScriptVariable(varNameR);
        return new ScriptCondition(left, op, right);
    }

    public ScriptBlock GetScript() {
        return mainScript;
    }
}