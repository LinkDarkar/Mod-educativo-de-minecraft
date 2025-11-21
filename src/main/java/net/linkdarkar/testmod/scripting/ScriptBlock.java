package net.linkdarkar.testmod.scripting;

import java.util.ArrayList;
import java.util.List;

public class ScriptBlock {
    public List<ScriptLine> blockLines = new ArrayList<>();

    public void AddInstruction (ScriptLine instruction) {
        blockLines.add(instruction);
    }

    public void ExecuteLines () {
        for (ScriptLine scriptLine : blockLines) {
            scriptLine.Execute();
        }
    }
}
