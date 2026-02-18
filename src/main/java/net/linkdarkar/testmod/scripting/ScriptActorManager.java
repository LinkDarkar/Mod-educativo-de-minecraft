package net.linkdarkar.testmod.scripting;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.linkdarkar.testmod.TestMod;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScriptActorManager {
    // The 'volatile' keyword ensures that multiple threads handle the instance variable correctly when it is being initialized to the Singleton instance.
    private static volatile ScriptActorManager instance;

    protected ScriptActorManager() {
        TestMod.LOGGER.info("ScriptActorManager initialized.");
    }

    public static ScriptActorManager getInstance() {
        if (instance == null) {
            // Synchronize on the class level to ensure thread safety
            synchronized (ScriptActorManager.class) {
                // Second check: If two threads enter the if block safely, one will wait here while the other creates the instance.
                // When the second enters, the instance will no longer be null.
                if (instance == null) {
                    instance = new ScriptActorManager();
                }
            }
        }
        return instance;
    }

    public static void load() {
        if (instance == null) {
            instance = new ScriptActorManager();
        }
    }

    public static void unload() {
        // cleanup data
        instance = null;
    }


    // Now the actual useful stuff -------------------------------------------------------------------------------

    private final Map<UUID, ScriptBuilder> actorBuilders = new HashMap<>();
    public Map<UUID, ScriptBuilder> getActorBuilders() {
        return actorBuilders;
    }

    public ScriptBuilder getBuilder(UUID uuid) {
        return actorBuilders.get(uuid);
    }

    public void saveBuilder(UUID uuid, ScriptBuilder builder) {
        actorBuilders.put(uuid, builder);
        markDirty();
    }

    private ScriptingSaveState activeState;

    public static void registerLifecycle() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            // Only load from Overworld to avoid duplicate loading
            if (world.getRegistryKey() == net.minecraft.world.World.OVERWORLD) {
                ScriptActorManager.getInstance().loadFromWorld(world);
            }
        });
    }

    public void loadFromWorld(ServerWorld world) {
        this.activeState = ScriptingSaveState.getServerState(world);
    }

    public void markDirty() {
        if (this.activeState != null) {
            this.activeState.markDirty();
        }
    }

    public void removeBuilder(UUID uuid) {
        actorBuilders.remove(uuid);
    }
}
