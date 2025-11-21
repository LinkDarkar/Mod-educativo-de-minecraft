package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.ScriptCondition;
import net.linkdarkar.testmod.scripting.ScriptLine;

public class InstructionWHILE extends ScriptLine {
    public ScriptCondition condition;

    public InstructionWHILE() {
        this.color = 0x66FF66;
    }

    @Override
    public String GetAsText() {
        return "WHILE (CONDITION)";
    }

    @Override
    public void Execute() {
        System.out.println("TRIES WHILE!!!!");
    }
}
