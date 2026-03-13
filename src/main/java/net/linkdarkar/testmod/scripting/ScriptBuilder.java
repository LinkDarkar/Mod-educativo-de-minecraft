package net.linkdarkar.testmod.scripting;

import net.linkdarkar.testmod.scripting.enums.MathOperator;
import net.linkdarkar.testmod.scripting.instructions.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptBuilder {
    public LivingEntity entity;
    public ScriptBlock mainScript = new ScriptBlock();

    public ScriptBlock parentBlockOfSelection;
    public ScriptLine selectedLine;

    public ScriptBuilder() {
        entity = null;
        parentBlockOfSelection = mainScript;
        selectedLine = null;
    }
    public ScriptBuilder(LivingEntity entity) {
        this.entity = entity;
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

    public void AddMath() {
        Insert(new InstructionMath("var", "0"));
    }

    public void AddVarAssign() {
        Insert(new InstructionVarAssign());
    }

    public void AddPrint() {
        Insert(new InstructionPrint("\"Value: \" + i"));
    }

    public void AddIf() {
        Insert(new InstructionIF());
    }

    public void AddWhile() {
        Insert(new InstructionWHILE());
    }

    public void AddFollowEntity() {
        Insert(new InstructionEntity_FollowEntity());
    }

    public void AddPlaceBlock() {
        Insert(new InstructionBlock_Place("0", "0", "0"));
    }

    public void DeleteSelected() {
        if (selectedLine == null || parentBlockOfSelection == null) return;

        int index = parentBlockOfSelection.blockLines.indexOf(selectedLine);
        parentBlockOfSelection.blockLines.remove(selectedLine);

        if (0 < index) {
            Select(parentBlockOfSelection, parentBlockOfSelection.blockLines.get(index - 1));
        } else {
            selectedLine = null;
        }
    }

    public void MoveUp() {
        if (selectedLine == null || parentBlockOfSelection == null) return;
        int index = parentBlockOfSelection.blockLines.indexOf(selectedLine);
        if (0 < index) {
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
            // Finds where the current parentBlock is located in the grandparent
            // We need to find the Instruction (IF/WHILE) that owns the current block
            ScriptLine ownerInstruction = FindInstructionOwningBlock(grandParent, parentBlockOfSelection);
            int ownerIndex = grandParent.blockLines.indexOf(ownerInstruction);

            // Moves selected line to grandparent
            parentBlockOfSelection.blockLines.remove(selectedLine);
            grandParent.blockLines.add(ownerIndex + 1, selectedLine);

            Select(grandParent, selectedLine);
        }
    }

    public void MoveRight() {
        if (selectedLine == null || parentBlockOfSelection == null) return;
        int index = parentBlockOfSelection.blockLines.indexOf(selectedLine);

        if (0 < index) {
            ScriptLine prevLine = parentBlockOfSelection.blockLines.get(index - 1);
            ScriptBlock targetBlock = null;

            if (prevLine instanceof InstructionIF) targetBlock = ((InstructionIF) prevLine).trueBlock;
            else if (prevLine instanceof InstructionWHILE) targetBlock = ((InstructionWHILE) prevLine).loopBlock;
            else if (prevLine instanceof InstructionVarAssign) targetBlock = ((InstructionVarAssign) prevLine).valueBlock;

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
            if (line instanceof InstructionVarAssign) childBlock = ((InstructionVarAssign) line).valueBlock;

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
            if (line instanceof InstructionVarAssign && ((InstructionVarAssign) line).valueBlock == targetBlock) return line;
        }
        return null;
    }

    public ScriptBlock GetScript() {
        return mainScript;
    }

    public Map<ScriptLine, List<String>> GetScriptErrors() {
        Map<ScriptLine, List<String>> errorMap = new HashMap<>();
        collectErrors(mainScript, errorMap);
        return errorMap;
    }

    private void collectErrors(ScriptBlock block, Map<ScriptLine, List<String>> errorMap) {
        for (ScriptLine line : block.blockLines) {
            List<String> lineErrors = line.Validate();
            if (lineErrors != null && !lineErrors.isEmpty()) {
                errorMap.put(line, lineErrors);
            }

            for (ScriptBlock childBlock : line.getChildBlocks()) {
                collectErrors(childBlock, errorMap);
            }
        }
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