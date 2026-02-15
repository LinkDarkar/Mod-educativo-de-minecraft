package net.linkdarkar.testmod.scripting;

import net.minecraft.nbt.NbtCompound;

public interface IScriptableEntity {
    void setStoredScript(ScriptBlock script);
    void setScriptRunning(boolean running);
    boolean isScriptRunning();

    NbtCompound getScriptNbt();
    void setScriptNbt(NbtCompound nbt);
}