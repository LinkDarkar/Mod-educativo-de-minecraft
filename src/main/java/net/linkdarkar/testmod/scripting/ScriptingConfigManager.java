package net.linkdarkar.testmod.scripting;

import java.util.HashMap;
import java.util.UUID;

public class ScriptingConfigManager {
    private static final ScriptingConfigManager INSTANCE = new ScriptingConfigManager();
    private final HashMap<UUID, ScriptingConfig> configs = new HashMap<>();

    public static ScriptingConfigManager getInstance() { return INSTANCE; }

    public ScriptingConfig getConfig(UUID entityUuid) {
        return configs.computeIfAbsent(entityUuid, k -> new ScriptingConfig());
    }

    // Stores the state for a specific entity's normal ScriptingScreen
    public static class ScriptingConfig {
        public boolean allowVar = true;
        public boolean allowIf = true;
        public boolean allowWhile = true;
        public boolean allowPrint = true;
        public boolean allowFollow = true;
        public boolean allowPlace = true;
    }
}