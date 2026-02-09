package net.linkdarkar.testmod.scripting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

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

    // NBT stuff
    @Override
    protected String getTypeID() {
        return "BLOCK";
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        NbtList list = new NbtList();
        for (ScriptLine line : blockLines) {
            list.add(line.toNbt());
        }
        nbt.put("lines", list);
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        blockLines.clear();
        NbtList list = nbt.getList("lines", 10); // 10 is the ID for Compound tags
        for (int i = 0; i < list.size(); i++) {
            ScriptLine line = ScriptLine.fromNbt(list.getCompound(i));
            if (line != null) blockLines.add(line);
        }
    }
}
