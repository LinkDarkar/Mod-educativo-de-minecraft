package net.linkdarkar.testmod.scripting.instructions;

import net.linkdarkar.testmod.scripting.*;
import net.linkdarkar.testmod.scripting.functionCaller.FunctionCaller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.UUID;

public class InstructionEntity_FollowEntity extends ScriptLine {
    public String targetUUID;

    public InstructionEntity_FollowEntity() {
        this.color = 0x999999;
    }

    @Override
    public String GetAsText() {
        return "Follow ["+targetUUID+"]";
    }

    @Override
    public void Execute(ExecutionContext context) {

        World world = context.executorEntity.getWorld();
        System.out.println("Trying to follow");
        if (world instanceof ServerWorld serverWorld)
        {
            System.out.println("Following");
            FunctionCaller.follow(serverWorld, context.executorEntity.getUuid(), UUID.fromString(targetUUID), 1.0);
        }
        else {
            Entity followedEntity = findEntityByUUID(targetUUID);
            System.out.println("Checking if entity exists");
            if (followedEntity != null)
            {
                System.out.println("Following");
                FunctionCaller.follow(context.executorEntity, followedEntity, 1.0);
            }
        }
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
        return "E_FOLLOW";
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = super.toNbt();
        nbt.putString("targetUID", targetUUID);
        return nbt;
    }

    @Override
    public void loadNbt(NbtCompound nbt) {
        this.targetUUID = nbt.getString("targetUID");
    }
}