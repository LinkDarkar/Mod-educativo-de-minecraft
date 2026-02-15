package net.linkdarkar.testmod.mixin;

import net.linkdarkar.testmod.scripting.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntityScriptingMixin extends LivingEntity implements IScriptableEntity {

    @Unique private ScriptBlock storedScript = null;
    @Unique private boolean isScriptRunning = false;

    protected MobEntityScriptingMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void setStoredScript(ScriptBlock script) {
        this.storedScript = script;
        System.out.println("Mixin: Script stored on " + this.getUuidAsString());
    }

    @Override
    public void setScriptRunning(boolean running) {
        this.isScriptRunning = running;
        System.out.println("Mixin: ticking set to " + running);
    }

    @Override
    public boolean isScriptRunning() {
        return this.isScriptRunning;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickScript(CallbackInfo ci) {
        // Only run on Server
        if (!this.getWorld().isClient && isScriptRunning && storedScript != null) {

            ExecutionContext ctx = new ExecutionContext((MobEntity)(Object)this);

            ctx.SetVar("posX", this.getX());
            ctx.SetVar("posY", this.getY());
            ctx.SetVar("posZ", this.getZ());

            try {
                storedScript.Execute(ctx);
            } catch (Exception e) {
                System.err.println("Script Error on entity " + this.getUuidAsString() + ": " + e.getMessage());
                this.isScriptRunning = false; // Emergency stop
            }
        }
    }

    // SAVING/LOADING DATA
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeScriptData(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("ScriptRunning", isScriptRunning);
        if (storedScript != null) {
            nbt.put("StoredScript", storedScript.toNbt());
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readScriptData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("ScriptRunning")) {
            this.isScriptRunning = nbt.getBoolean("ScriptRunning");
        }
        if (nbt.contains("StoredScript")) {
            this.storedScript = (ScriptBlock) ScriptLine.fromNbt(nbt.getCompound("StoredScript"));
        }
    }

    @Override
    public NbtCompound getScriptNbt() {
        return storedScript != null ? storedScript.toNbt() : new NbtCompound();
    }

    @Override
    public void setScriptNbt(NbtCompound nbt) {
        this.storedScript = (ScriptBlock) ScriptLine.fromNbt(nbt);
    }
}