package net.linkdarkar.testmod.scripting;

import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.UUID;

public class ScriptingConfigManager {
    private static final ScriptingConfigManager INSTANCE = new ScriptingConfigManager();

    private final HashMap<UUID, ScriptingConfig> configs = new HashMap<>();
    private final HashMap<UUID, EntityActions> actionsMap = new HashMap<>();

    public static ScriptingConfigManager getInstance() { return INSTANCE; }

    public ScriptingConfig getConfig(UUID entityUuid) {
        return configs.computeIfAbsent(entityUuid, k -> new ScriptingConfig());
    }

    public EntityActions getActions(UUID entityUuid) {
        return actionsMap.computeIfAbsent(entityUuid, k -> new EntityActions());
    }

    public static class ScriptingConfig {
        public boolean allowVar = true;
        public boolean allowIf = true;
        public boolean allowElse = true;
        public boolean allowWhile = true;
        public boolean allowPrint = true;
        public boolean allowFollow = true;
        public boolean allowPlace = true;
        public boolean allowCommand = true;
    }

    public static class ActionEventData {
        public String commands = "";
        public int maxExecutions = 0; // 0 means infinite
        public int currentExecutions = 0;

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("cmds", commands);
            nbt.putInt("max", maxExecutions);
            nbt.putInt("count", currentExecutions);
            return nbt;
        }

        public void loadNbt(NbtCompound nbt) {
            this.commands = nbt.getString("cmds");
            this.maxExecutions = nbt.getInt("max");
            this.currentExecutions = nbt.getInt("count");
        }
    }

    public static class EntityActions {
        public ActionEventData anyExecute = new ActionEventData();
        public ActionEventData executeCorrect = new ActionEventData();
        public ActionEventData executeWrong = new ActionEventData();

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.put("any", anyExecute.toNbt());
            nbt.put("correct", executeCorrect.toNbt());
            nbt.put("wrong", executeWrong.toNbt());
            return nbt;
        }

        public void loadNbt(NbtCompound nbt) {
            if (nbt.contains("any")) anyExecute.loadNbt(nbt.getCompound("any"));
            if (nbt.contains("correct")) executeCorrect.loadNbt(nbt.getCompound("correct"));
            if (nbt.contains("wrong")) executeWrong.loadNbt(nbt.getCompound("wrong"));
        }
    }
}