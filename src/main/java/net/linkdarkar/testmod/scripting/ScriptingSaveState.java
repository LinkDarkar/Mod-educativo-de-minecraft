package net.linkdarkar.testmod.scripting;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.UUID;

public class ScriptingSaveState extends PersistentState {

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList actorList = new NbtList();

        ScriptActorManager manager = ScriptActorManager.getInstance();

        manager.getActorBuilders().forEach((uuid, builder) -> {
            NbtCompound actorTag = new NbtCompound();
            actorTag.putUuid("uuid", uuid);
            actorTag.put("script", builder.toNbt());
            actorList.add(actorTag);
        });

        nbt.put("scriptActors", actorList);

        NbtList configList = new NbtList();
        ScriptingConfigManager configManager = ScriptingConfigManager.getInstance();

        for (UUID uuid : configManager.getAllTrackedUuids()) {
            NbtCompound configEntry = new NbtCompound();
            configEntry.putUuid("uuid", uuid);
            configEntry.put("data", configManager.exportAllToNbt(uuid));
            configList.add(configEntry);
        }
        nbt.put("scriptConfigs", configList);

        return nbt;
    }

    public static ScriptingSaveState createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        ScriptingSaveState state = new ScriptingSaveState();

        NbtList actorList = nbt.getList("scriptActors", 10);
        ScriptActorManager manager = ScriptActorManager.getInstance();

        for (int i = 0; i < actorList.size(); i++) {
            NbtCompound actorTag = actorList.getCompound(i);
            UUID uuid = actorTag.getUuid("uuid");
            ScriptBuilder builder = ScriptBuilder.fromNbt(actorTag.getCompound("script"));

            manager.saveBuilder(uuid, builder);
        }

        NbtList configList = nbt.getList("scriptConfigs", 10);
        ScriptingConfigManager configManager = ScriptingConfigManager.getInstance();
        configManager.clear(); // Clear old data

        for (int i = 0; i < configList.size(); i++) {
            NbtCompound entry = configList.getCompound(i);
            UUID uuid = entry.getUuid("uuid");
            NbtCompound data = entry.getCompound("data");
            configManager.importAllFromNbt(uuid, data);
        }

        return state;
    }

    public static ScriptingSaveState getServerState(ServerWorld serverWorld) {
        // This ensures the data is saved in the Overworld's data folder (global for the server)
        return serverWorld.getPersistentStateManager().getOrCreate(
                new Type<>(
                        ScriptingSaveState::new,
                        ScriptingSaveState::createFromNbt,
                        null
                ),
                "testmod_scripts"
        );
    }
}