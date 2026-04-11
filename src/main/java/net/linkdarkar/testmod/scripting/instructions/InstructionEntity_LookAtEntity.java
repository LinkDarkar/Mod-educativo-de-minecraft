package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.scripting.ExecutionContext;
import net.linkdarkar.testmod.scripting.ScriptLine;
import net.linkdarkar.testmod.scripting.functionCaller.FunctionCaller;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InstructionEntity_LookAtEntity extends ScriptLine {
    public String targetUUID;

    public InstructionEntity_LookAtEntity() {
        this.color = 0x55FFFF;
        this.targetUUID = "";
    }

    @Override
    public String GetLineAsPlainText() {
        return "Look At " + targetUUID;
    }

    public String GetLineHandle() {
        return "Look At";
    }

    @Override
    public List<String> Validate() {
        List<String> errors = new ArrayList<>();

        if (targetUUID == null || targetUUID.trim().isEmpty()) {
            errors.add("Target UUID cannot be empty.");
            return errors;
        }

        try {
            String cleanUUID = targetUUID.replace("\"", "");
            java.util.UUID.fromString(cleanUUID);
        } catch (IllegalArgumentException e) {
            errors.add("Invalid UUID format: " + targetUUID);
        }

        return errors;
    }

    @Override
    public Object Execute(ExecutionContext context) {
        if (targetUUID == null || targetUUID.isEmpty()) return null;

        // If it's a simulation, skips changing the world
        if (context.isSimulation) {
            return null;
        }

        TestMod.LOGGER.info("Executing Follow Entity Instruction...");

        World world = context.executorEntity.getWorld();

        if (world instanceof ServerWorld serverWorld) {
            try {
                String cleanUUID = targetUUID.replace("\"", "");

                UUID uuid = UUID.fromString(cleanUUID);

                boolean success = FunctionCaller.lookAtEntity(serverWorld, context.executorEntity.getUuid(), uuid);

                TestMod.LOGGER.info("Look at command sent. Success: {}", success);

            } catch (IllegalArgumentException e) {
                TestMod.LOGGER.error("Invalid UUID format in script: {}", targetUUID);
            } catch (Exception e) {
                TestMod.LOGGER.error("Error executing Look at: {}", e.getMessage());
            }
        }
        else {
            TestMod.LOGGER.warn("Script tried to run on Client Side. Ignoring.");
        }
        return null;
    }

    @Override
    protected String getTypeID() {
        return "E_LOOK_AT";
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.putString("targetUUID", targetUUID != null ? targetUUID : "");
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.targetUUID = nbt.getString("targetUUID");
    }
}