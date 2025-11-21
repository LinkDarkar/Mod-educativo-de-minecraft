package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.ScriptCondition;
import net.linkdarkar.testmod.scripting.ScriptLine;

public class InstructionIF extends ScriptLine {
    public ScriptCondition condition;

    public InstructionIF() {
        this.color = 0xFF0000;
    }

    public String GetAsText() {
        return "IF (CONDITION)";
    }

    @Override
    public void Execute() {
        System.out.println("TRIES IF!!!!");
    }
}
