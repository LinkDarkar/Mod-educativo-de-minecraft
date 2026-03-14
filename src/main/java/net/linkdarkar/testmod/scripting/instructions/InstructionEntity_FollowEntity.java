package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.scripting.*;
import net.linkdarkar.testmod.scripting.functionCaller.FunctionCaller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InstructionEntity_FollowEntity extends ScriptLine {
    public String targetUUID;

    public InstructionEntity_FollowEntity() {
        this.color = 0x999999;
        this.targetUUID = "";
    }
    public InstructionEntity_FollowEntity(String target) {
        this.color = 0x999999;
        this.targetUUID = target;
    }

    @Override
    public String GetAsText() {
        return "Follow ["+targetUUID+"]";
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

                boolean success = FunctionCaller.follow(serverWorld, context.executorEntity.getUuid(), uuid, 1.0);

                TestMod.LOGGER.info("Follow command sent. Success: {}", success);

            } catch (IllegalArgumentException e) {
                TestMod.LOGGER.error("Invalid UUID format in script: {}", targetUUID);
            } catch (Exception e) {
                TestMod.LOGGER.error("Error executing follow: {}", e.getMessage());
            }
        }
        else {
            TestMod.LOGGER.warn("Script tried to run on Client Side. Ignoring.");
        }
        return null;
    }


    private Entity findEntityByUUID(String uuidString) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return null;

        try {
            UUID uuid = UUID.fromString(uuidString);
            for (Entity entity : client.world.getEntities()) {
                if (entity.getUuid().equals(uuid)) {
                    return entity;
                }
            }
        } catch (IllegalArgumentException e) {
            // Invalid UUID string
            return null;
        }
        // Entity not found or not loaded on client
        return null;
    }

    // NBT stuff
    @Override
    protected String getTypeID() {
        return "E_FOLLOW_ENTITY";
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