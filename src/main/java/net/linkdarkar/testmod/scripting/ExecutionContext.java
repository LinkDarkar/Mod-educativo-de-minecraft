package net.linkdarkar.testmod.scripting;

import net.minecraft.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutionContext
{
    public Map<String, Object> variables = new HashMap<>();
    public LivingEntity executorEntity;

    public boolean isSimulation = false;

    public int executionSteps = 0;
    public static final int MAX_STEPS = 10000;

    public ExecutionContext(LivingEntity entity)
    {
        this.executorEntity = entity;

        if (entity != null && !this.isSimulation) {
            ScriptingConfigManager.ScriptingConfig config = ScriptingConfigManager.getInstance().getConfig(entity.getUuid());

            // Inject the user's custom test variables
            for (ScriptingConfigManager.PersistentVariable pv : config.userTestVariables) {
                if (pv.name == null || pv.name.trim().isEmpty()) continue;
                String cleanName = pv.name.trim();
                String val = pv.value.trim();

                try {
                    this.variables.put(cleanName, Double.parseDouble(val));
                } catch (NumberFormatException e) {
                    if (!val.startsWith("\"")) val = "\"" + val;
                    if (!val.endsWith("\"")) val = val + "\"";
                    this.variables.put(cleanName, val);
                }
            }
        }
    }

    // List of the "print output" to later verify when validating
    public List<String> printedMessages = new ArrayList<>();

    // List to track physical actions the script tries to perform (like FollowEntity)
    public List<String> recordedActions = new ArrayList<>();

    public void incrementSteps()
    {
        executionSteps++;
        if (executionSteps > MAX_STEPS) {
            // TODO: Send message in-game
            throw new RuntimeException("Infinite loop detected or script too long!");
        }
    }

    public void SetVar(String name, Object value)
    {
        if (name == null) return;
        name = name.trim();

        variables.put(name, value);

        // Update save with the var if it is persistent
        if (executorEntity != null && !this.isSimulation) {
            ScriptingConfigManager.ScriptingConfig config = ScriptingConfigManager.getInstance().getConfig(executorEntity.getUuid());
            for (ScriptingConfigManager.PersistentVariable pv : config.persistentVariables) {
                if (pv.name.trim().equals(name)) {
                    pv.value = value.toString();
                    ScriptingConfigManager.getInstance().markDirty();
                    break;
                }
            }
        }
    }

    public Object GetVar(String name)
    {
        if (name == null) return 0;
        name = name.trim();

        // Check local transient variables first
        if (variables.containsKey(name)) {
            return variables.get(name);
        }

        // Check persistent configuration
        if (executorEntity != null) {
            ScriptingConfigManager.ScriptingConfig config = ScriptingConfigManager.getInstance().getConfig(executorEntity.getUuid());
            for (ScriptingConfigManager.PersistentVariable pv : config.persistentVariables) {
                if (pv.name.trim().equals(name)) {
                    try {
                        return Double.parseDouble(pv.value); // Treat as math if possible
                    } catch (NumberFormatException e) {
                        return pv.value; // Otherwise return the string
                    }
                }
            }
        }

        return 0; // Default undefined behavior
    }
}