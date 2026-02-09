package net.linkdarkar.testmod.scripting;

import net.linkdarkar.testmod.scripting.enums.MathOperator;
import net.linkdarkar.testmod.scripting.instructions.*;
import net.minecraft.nbt.NbtCompound;

public class ScriptBuilder {
    public ScriptBlock mainScript = new ScriptBlock();

    public ScriptBlock parentBlockOfSelection;
    public ScriptLine selectedLine;

    public ScriptBuilder() {
        parentBlockOfSelection = mainScript;
        selectedLine = null;
    }

    public void Select(ScriptBlock parent, ScriptLine line) {
        this.parentBlockOfSelection = parent;
        this.selectedLine = line;
    }

    public void Deselect() {
        this.parentBlockOfSelection = mainScript;
        this.selectedLine = null;
    }

    private void Insert(ScriptLine line) {
        if (selectedLine != null && parentBlockOfSelection != null)
        {
            int index = parentBlockOfSelection.blockLines.indexOf(selectedLine);
            if (index != -1)
            {
                parentBlockOfSelection.blockLines.add(index + 1, line);
            }
            else
            {
                parentBlockOfSelection.blockLines.add(line);
            }
        }
        else
        {
            mainScript.blockLines.add(line);
        }

        Select(parentBlockOfSelection != null ? parentBlockOfSelection : mainScript, line);
    }

    /*private void Add(ScriptLine line) {
        blockStack.peek().AddInstruction(line);
    }*/

    public void DeclareVar(String name, Object initialValue) {
        Insert(new InstructionMath(name, initialValue.toString()));
    }
    public void DeclareVarSimple(String name, Object initialValue) {
        ScriptVariable left = new ScriptVariable("0");
        ScriptVariable right = new ScriptVariable(initialValue.toString());
        Insert(new InstructionMathSimple(name, left, MathOperator.ASSIGN, right));
    }

    public void AddMath() {
        Insert(new InstructionMath("var", "0"));
    }

    public void AddPrint() {
        Insert(new InstructionPrint("\"Value: \" + i"));
    }

    public void AddIf() {
        InstructionIF instr = new InstructionIF();
        Insert(instr);
    }

    public void AddWhile() {
        InstructionWHILE instr = new InstructionWHILE();
        Insert(instr);
    }

    public void DeleteSelected() {
        if (selectedLine == null || parentBlockOfSelection == null) return;

        int index = parentBlockOfSelection.blockLines.indexOf(selectedLine);
        parentBlockOfSelection.blockLines.remove(selectedLine);

        if (index > 0) {
            Select(parentBlockOfSelection, parentBlockOfSelection.blockLines.get(index - 1));
        } else {
            selectedLine = null;
        }
    }

    public void MoveUp() {
        if (selectedLine == null || parentBlockOfSelection == null) return;
        int index = parentBlockOfSelection.blockLines.indexOf(selectedLine);
        if (index > 0) {
            ScriptLine temp = parentBlockOfSelection.blockLines.remove(index);
            parentBlockOfSelection.blockLines.add(index - 1, temp);
        }
    }

    public void MoveDown() {
        if (selectedLine == null || parentBlockOfSelection == null) return;
        int index = parentBlockOfSelection.blockLines.indexOf(selectedLine);
        if (index < parentBlockOfSelection.blockLines.size() - 1) {
            ScriptLine temp = parentBlockOfSelection.blockLines.remove(index);
            parentBlockOfSelection.blockLines.add(index + 1, temp);
        }
    }

    public void MoveLeft() {
        if (selectedLine == null || parentBlockOfSelection == null) return;
        if (parentBlockOfSelection == mainScript) return;

        // Find the parent of the current parentBlock
        ScriptBlock grandParent = FindParentBlock(mainScript, parentBlockOfSelection);
        if (grandParent != null) {
            // Find where the current parentBlock is located in the grandparent
            // We need to find the Instruction (IF/WHILE) that owns the current block
            ScriptLine ownerInstruction = FindInstructionOwningBlock(grandParent, parentBlockOfSelection);
            int ownerIndex = grandParent.blockLines.indexOf(ownerInstruction);

            // Move selected line to grandparent
            parentBlockOfSelection.blockLines.remove(selectedLine);
            grandParent.blockLines.add(ownerIndex + 1, selectedLine);

            Select(grandParent, selectedLine);
        }
    }

    public void MoveRight() {
        if (selectedLine == null || parentBlockOfSelection == null) return;
        int index = parentBlockOfSelection.blockLines.indexOf(selectedLine);

        if (index > 0) {
            ScriptLine prevLine = parentBlockOfSelection.blockLines.get(index - 1);
            ScriptBlock targetBlock = null;

            if (prevLine instanceof InstructionIF) targetBlock = ((InstructionIF) prevLine).trueBlock;
            else if (prevLine instanceof InstructionWHILE) targetBlock = ((InstructionWHILE) prevLine).loopBlock;

            if (targetBlock != null) {
                parentBlockOfSelection.blockLines.remove(selectedLine);
                targetBlock.blockLines.add(selectedLine);
                Select(targetBlock, selectedLine);
            }
        }
    }

    private ScriptBlock FindParentBlock(ScriptBlock currentScope, ScriptBlock target) {
        for (ScriptLine line : currentScope.blockLines) {
            ScriptBlock childBlock = null;
            if (line instanceof InstructionIF) childBlock = ((InstructionIF) line).trueBlock;
            if (line instanceof InstructionWHILE) childBlock = ((InstructionWHILE) line).loopBlock;

            if (childBlock == target) return currentScope;

            if (childBlock != null) {
                ScriptBlock result = FindParentBlock(childBlock, target);
                if (result != null) return result;
            }
        }
        return null;
    }

    private ScriptLine FindInstructionOwningBlock(ScriptBlock scope, ScriptBlock targetBlock) {
        for (ScriptLine line : scope.blockLines) {
            if (line instanceof InstructionIF && ((InstructionIF) line).trueBlock == targetBlock) return line;
            if (line instanceof InstructionWHILE && ((InstructionWHILE) line).loopBlock == targetBlock) return line;
        }
        return null;
    }

    public ScriptBlock GetScript() {
        return mainScript;
    }

    // Save NBT stuff
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.put("main", mainScript.toNbt());
        return nbt;
    }

    public static ScriptBuilder fromNbt(NbtCompound nbt) {
        ScriptBuilder builder = new ScriptBuilder();
        builder.mainScript = (ScriptBlock) ScriptLine.fromNbt(nbt.getCompound("main"));

        builder.Deselect();
        return builder;
    }
}