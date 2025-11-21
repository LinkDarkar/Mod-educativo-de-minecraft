package net.linkdarkar.testmod.scripting;

public abstract class ScriptLine {
    public Object instruction;
    // public ScriptBlock scriptBlock;     // wtf
    public String text;
    public int color = 0xFFFFFF;

    public abstract String GetAsText();
    public abstract void Execute();
}
