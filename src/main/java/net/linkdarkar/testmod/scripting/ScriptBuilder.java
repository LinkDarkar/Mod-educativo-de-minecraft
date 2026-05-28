package net.linkdarkar.testmod.scripting;

import net.linkdarkar.testmod.scripting.enums.MathOperator;
import net.linkdarkar.testmod.scripting.instructions.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
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
            // If the selected line is a Block Owner, inject inside it
            if (selectedLine instanceof InstructionIF ifLine) {
                ifLine.trueBlock.blockLines.add(0, line);
                Select(ifLine.trueBlock, line);
            } else if (selectedLine instanceof InstructionELSE elseLine) {
                elseLine.elseBlock.blockLines.add(0, line);
                Select(elseLine.elseBlock, line);
            } else if (selectedLine instanceof InstructionWHILE whileLine) {
                whileLine.loopBlock.blockLines.add(0, line);
                Select(whileLine.loopBlock, line);
            } else if (selectedLine instanceof InstructionVarAssign varAssignLine) {
                varAssignLine.valueBlock.blockLines.add(0, line);
                Select(varAssignLine.valueBlock, line);
            } else {
                // Otherwise, insert immediately beneath the selected line
                int index = parentBlockOfSelection.blockLines.indexOf(selectedLine);
                if (index != -1) {
                    parentBlockOfSelection.blockLines.add(index + 1, line);
                } else {
                    parentBlockOfSelection.blockLines.add(line);
                }
                Select(parentBlockOfSelection, line);
            }
        }
        else
        {
            mainScript.blockLines.add(line);
            Select(mainScript, line);
        }
    }

    /*private void Add(ScriptLine line) {
        blockStack.peek().AddInstruction(line);
    }*/

    public void DeclareVar(String name, Object initialValue) {
        Insert(new InstructionMath(name, initialValue.toString()));
    }

    public void AddMath()
    {
        Insert(new InstructionMath("var", "0"));
    }

    public void AddVarAssign()
    {
        Insert(new InstructionVarAssign());
    }

    public void AddPrint()
    {
        Insert(new InstructionPrint("\"Value: \" + var"));
    }

    public void AddIf()
    {
        Insert(new InstructionIF());
    }

    public void AddElse()
    {
        Insert(new InstructionELSE());
    }

    public void AddWhile()
    {
        Insert(new InstructionWHILE());
    }

    public void AddCommand()
    { Insert(new InstructionMinecraft_ExecuteCommand()); }

    public void AddLookAtEntity()
    {
        Insert(new InstructionEntity_LookAtEntity());
    }

    public void AddFollowEntity()
    {
        Insert(new InstructionEntity_FollowEntity());
    }
    public void AddWalkTowards()
    {
        Insert(new InstructionEntity_WalkTowards());
    }

    public void AddWalkForward()
    {
        Insert(new InstructionEntity_WalkForward());
    }

    public void AddPlaceBlock()
    {
        Insert(new InstructionBlock_Place("0", "0", "0"));
    }

    public void AddDistanceFromEntity()
    {
        Insert(new InstructionEntity_DistanceFromEntity());
    }
    public void AddDistanceFromPosition()
    {
        Insert(new InstructionEntity_DistanceFromPosition());
    }

    public void DuplicateSelected() {
        if (selectedLine == null || parentBlockOfSelection == null) return;

        // Deep copy the line (and all its children) using the existing NBT system
        NbtCompound nbt = selectedLine.toNbt();
        ScriptLine copy = ScriptLine.fromNbt(nbt);

        if (copy != null) {
            int index = parentBlockOfSelection.blockLines.indexOf(selectedLine);

            // Insert the copy right after the original
            parentBlockOfSelection.blockLines.add(index + 1, copy);

            Select(parentBlockOfSelection, copy);
        }
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
            else if (prevLine instanceof InstructionELSE) targetBlock = ((InstructionELSE) prevLine).elseBlock;
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
            else if (line instanceof InstructionELSE) childBlock = ((InstructionELSE) line).elseBlock;
            else if (line instanceof InstructionWHILE) childBlock = ((InstructionWHILE) line).loopBlock;
            else if (line instanceof InstructionVarAssign) childBlock = ((InstructionVarAssign) line).valueBlock;

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
            else if (line instanceof InstructionELSE && ((InstructionELSE) line).elseBlock == targetBlock) return line;
            else if (line instanceof InstructionWHILE && ((InstructionWHILE) line).loopBlock == targetBlock) return line;
            else if (line instanceof InstructionVarAssign && ((InstructionVarAssign) line).valueBlock == targetBlock) return line;
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
        for (int i = 0; i < block.blockLines.size(); i++) {
            ScriptLine line = block.blockLines.get(i);
            List<String> lineErrors = line.Validate();

            // Check if an ELSE is directly beneath an IF
            if (line instanceof InstructionELSE) {
                if (i == 0 || !(block.blockLines.get(i - 1) instanceof InstructionIF)) {
                    if (lineErrors == null) lineErrors = new ArrayList<>();
                    lineErrors.add("ELSE must be placed immediately below an IF instruction.");
                }
            }

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