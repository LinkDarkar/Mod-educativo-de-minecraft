package net.linkdarkar.testmod.scripting;

import java.util.ArrayList;
import java.util.List;

public class ScriptBlock extends ScriptLine {
    public List<ScriptLine> blockLines = new ArrayList<>();

    public void AddInstruction (ScriptLine instruction) {
        blockLines.add(instruction);
    }

    @Override
    public void Execute(ExecutionContext context) {
        for (ScriptLine scriptLine : blockLines) {
            scriptLine.Execute(context);
        }
    }

    @Override
    public String GetAsText() {
        return "BLOCK";
    }

    public void ExecuteLines (ExecutionContext context) {
        this.Execute(context);
    }
}
