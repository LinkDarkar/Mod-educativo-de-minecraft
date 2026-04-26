package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.scripting.*;
import net.linkdarkar.testmod.scripting.functionCaller.FunctionCaller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InstructionEntity_FollowEntity extends ScriptLine {
    public String targetUUIDExp;

    public InstructionEntity_FollowEntity() {
        this.color = 0x999999;
        this.targetUUIDExp = "";
    }
    public InstructionEntity_FollowEntity(String target) {
        this.color = 0x999999;
        this.targetUUIDExp = target;
    }

    @Override
    public String GetLineAsPlainText() {
        return "Follow ["+ targetUUIDExp +"]";
    }
    @Override
    public String GetLineHandle()
    {
        return "FOLLOW";
    }

    @Override
    public List<String> Validate() {
        List<String> errors = new ArrayList<>();

        if (targetUUIDExp == null || targetUUIDExp.trim().isEmpty()) {
            errors.add("Target UUID expression cannot be empty.");
            return errors;
        }

        try {
            ExpressionEvaluator.evaluate(targetUUIDExp, new ExecutionContext(null));
        } catch (Exception e) {
            errors.add("Invalid UUID expression: " + e.getMessage());
        }

        return errors;
    }

    @Override
    public Object Execute(ExecutionContext context) {
        if (targetUUIDExp == null || targetUUIDExp.isEmpty()) return null;

        // If it's a simulation, skips changing the world
        if (context.isSimulation) {
            return null;
        }

        TestMod.LOGGER.info("Executing Follow Entity Instruction...");

        World world = context.executorEntity.getWorld();

        if (world instanceof ServerWorld serverWorld) {
            try {
                // Evaluate the expression to get the actual UUID string
                Object res = ExpressionEvaluator.evaluate(targetUUIDExp, context);
                String cleanUUID = res.toString().replace("\"", "").trim();

                if (cleanUUID.isEmpty()) return null;

                UUID uuid = UUID.fromString(cleanUUID);

                boolean success = FunctionCaller.follow(serverWorld, context.executorEntity.getUuid(), uuid, 1.0);

                TestMod.LOGGER.info("Follow command sent. Success: {}", success);

            } catch (IllegalArgumentException e) {
                TestMod.LOGGER.error("Invalid UUID format in script: {}", targetUUIDExp);
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
        nbt.putString("targetUUID", targetUUIDExp != null ? targetUUIDExp : "");
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.targetUUIDExp = nbt.getString("targetUUID");

        // Backward compatibility: If an old NPC has a raw UUID saved without quotes, wrap it in quotes so that the ExpressionEvaluator treats it as a string and doesn't crash trying to do math on it.
        if (this.targetUUIDExp != null && !this.targetUUIDExp.startsWith("\"") && this.targetUUIDExp.contains("-")) {
            this.targetUUIDExp = "\"" + this.targetUUIDExp + "\"";
        }
    }
}