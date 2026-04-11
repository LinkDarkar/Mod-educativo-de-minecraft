package net.linkdarkar.testmod.scripting;

import java.util.ArrayList;
import java.util.List;

import net.linkdarkar.testmod.scripting.instructions.InstructionELSE;
import net.linkdarkar.testmod.scripting.instructions.InstructionIF;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public class ScriptBlock extends ScriptLine {
    public List<ScriptLine> blockLines = new ArrayList<>();

    public void AddInstruction (ScriptLine instruction) {
        blockLines.add(instruction);
    }

    @Override
    public Object Execute(ExecutionContext context) {
        Object lastVal = null;

        for (int i = 0; i < blockLines.size(); i++) {
            ScriptLine scriptLine = blockLines.get(i);

            // Link certain things before executing (Like the IF with the ELSE)
            if (scriptLine instanceof InstructionIF ifLine) {

                if (i + 1 < blockLines.size() && blockLines.get(i + 1) instanceof InstructionELSE elseLine) {
                    ifLine.elseBlock = elseLine.elseBlock;
                } else {
                    // Clear in case it was previously set and the script changed
                    ifLine.elseBlock = null;
                }

                lastVal = ifLine.Execute(context);

            } else if (scriptLine instanceof InstructionELSE) {
                // Skip ELSEs, as the IF should execute it

                continue;

            } else {
                lastVal = scriptLine.Execute(context);
            }
        }
        return lastVal;
    }

    @Override
    public String GetLineAsPlainText() {
        return "BLOCK";
    }

    @Override
    public String GetLineHandle()
    {
        return "";
    }

    @Override
    public List<String> Validate() {
        List<String> errors = new ArrayList<>();
        for (ScriptLine line : blockLines) {
            errors.addAll(line.Validate());
        }
        return errors;
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
