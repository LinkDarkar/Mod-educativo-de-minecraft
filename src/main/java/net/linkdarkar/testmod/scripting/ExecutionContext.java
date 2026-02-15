package net.linkdarkar.testmod.scripting;

import net.minecraft.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public class ExecutionContext
{
    public Map<String, Object> variables = new HashMap<>();
    public LivingEntity executorEntity;

    public ExecutionContext(LivingEntity entity) {
        this.executorEntity = entity;
    }

    public void SetVar(String name, Object value)
    {
        variables.put(name, value);
    }

    public Object GetVar(String name)
    {
        return variables.getOrDefault(name, 0);
    }
}