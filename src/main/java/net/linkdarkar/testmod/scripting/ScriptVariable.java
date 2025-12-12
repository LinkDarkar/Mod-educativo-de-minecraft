package net.linkdarkar.testmod.scripting;

import net.linkdarkar.testmod.scripting.enums.ScriptVariableType;

public class ScriptVariable {
    public String variableName;
    public ScriptVariableType type;
    public Object value;

    public ScriptVariable(String rawInput) {
        UpdateValue(rawInput);
    }

    public void UpdateValue(String rawInput) {
        try {
            // Try parsing as number
            this.value = Double.parseDouble(rawInput);
            this.type = ScriptVariableType.NUMBER;
            this.variableName = null;
        } catch (NumberFormatException e) {
            // It's a string or variable reference
            this.variableName = rawInput;
            this.type = ScriptVariableType.REFERENCE;
            this.value = rawInput;
        }
    }

    public Object GetResolvedValue(ExecutionContext ctx) {
        if (type == ScriptVariableType.REFERENCE) return ctx.GetVar(variableName);
        return value;
    }
    public String GetOriginalValue() {
        if (type == ScriptVariableType.REFERENCE) return variableName;
        return value.toString();
    }
}