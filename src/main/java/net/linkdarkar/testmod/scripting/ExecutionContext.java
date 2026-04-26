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
    }

    // List of the "print output" to later verify when validating
    public List<String> printedMessages = new ArrayList<>();

    public void incrementSteps()
    {
        executionSteps++;
        if (executionSteps > MAX_STEPS) {
            throw new RuntimeException("Infinite loop detected or script too long!");
        }
    }

    public void SetVar(String name, Object value)
    {
        variables.put(name, value);

        // Update save with the var if it is persistent
        if (executorEntity != null) {
            ScriptingConfigManager.ScriptingConfig config = ScriptingConfigManager.getInstance().getConfig(executorEntity.getUuid());
            for (ScriptingConfigManager.PersistentVariable pv : config.persistentVariables) {
                if (pv.name.equals(name)) {
                    pv.value = value.toString();
                    ScriptingConfigManager.getInstance().markDirty();
                    break;
                }
            }
        }
    }

    public Object GetVar(String name)
    {
        // Check local transient variables first
        if (variables.containsKey(name)) {
            return variables.get(name);
        }

        // Check persistent configuration
        if (executorEntity != null) {
            ScriptingConfigManager.ScriptingConfig config = ScriptingConfigManager.getInstance().getConfig(executorEntity.getUuid());
            for (ScriptingConfigManager.PersistentVariable pv : config.persistentVariables) {
                if (pv.name.equals(name)) {
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