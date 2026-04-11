package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.*;
import net.minecraft.nbt.NbtCompound;
import java.util.ArrayList;
import java.util.List;

public class InstructionVarAssign extends ScriptLine {
    public String targetVarName;
    public ScriptBlock valueBlock = new ScriptBlock();

    public InstructionVarAssign() {
        this.targetVarName = "newVar";
        this.color = 0xFFFF55;
    }

    @Override
    public String GetLineAsPlainText() {
        return targetVarName + " = [Inline Block]";
    }

    @Override
    public String GetLineHandle()
    {
        return "VARF";
    }

    @Override
    public List<String> Validate() {
        List<String> errors = new ArrayList<>();
        if (targetVarName == null || targetVarName.trim().isEmpty()) {
            errors.add("Target variable name cannot be empty.");
        }
        return errors;
    }

    @Override
    public List<ScriptBlock> getChildBlocks() {
        return List.of(valueBlock);
    }

    @Override
    public Object Execute(ExecutionContext context) {
        // Evaluates the nested inline block and saves the result to the variable
        Object val = valueBlock.Execute(context);
        context.SetVar(targetVarName, val);
        return val;
    }

    @Override
    protected String getTypeID() {
        return "VAR_ASSIGN";
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.putString("target", targetVarName);
        nbt.put("valBlock", valueBlock.toNbt());
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.targetVarName = nbt.getString("target");
        this.valueBlock = (ScriptBlock) ScriptLine.fromNbt(nbt.getCompound("valBlock"));
    }
}