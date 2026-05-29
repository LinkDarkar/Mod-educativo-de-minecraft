package net.linkdarkar.testmod.mixin;

import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.scripting.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(MobEntity.class)
public abstract class MobEntityScriptingMixin extends LivingEntity implements IScriptableEntity {

    @Unique private ScriptBlock storedScript = null;
    @Unique private boolean isScriptRunning = false;

    @Unique private int currentCheckpointIndex = 0;

    protected MobEntityScriptingMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void setStoredScript(ScriptBlock script) {
        this.storedScript = script;
        TestMod.LOGGER.info("Mixin: Script stored on " + this.getUuidAsString());
    }

    @Override
    public void setScriptRunning(boolean running) {
        // Reset the checkpoint index when the loop starts
        if (running && !this.isScriptRunning) {
            this.currentCheckpointIndex = 0;
        }
        this.isScriptRunning = running;
        TestMod.LOGGER.info("Mixin: ticking set to " + running);
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

            // Checkpoint tracking
            ScriptingConfigManager.ScriptingConfig config = ScriptingConfigManager.getInstance().getConfig(this.getUuid());

            if (!config.checkpoints.isEmpty() && this.currentCheckpointIndex < config.checkpoints.size()) {
                ScriptingConfigManager.CheckpointData targetCp = config.checkpoints.get(this.currentCheckpointIndex);

                // Handle dynamic target position based on the mode (Entity vs POS)
                double targetX = targetCp.x;
                double targetY = targetCp.y;
                double targetZ = targetCp.z;
                boolean validTarget = true;

                if (targetCp.useEntity) {
                    validTarget = false; // Default to false until we find the entity
                    try {
                        // Strip quotes if they were added via the GUI's "Insert UUID" button
                        String rawUuid = targetCp.entityUuid.replace("\"", "").trim();
                        UUID targetUuid = UUID.fromString(rawUuid);

                        // Look up the entity on the server world
                        if (this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                            Entity targetEntity = serverWorld.getEntity(targetUuid);
                            if (targetEntity != null) {
                                targetX = targetEntity.getX();
                                targetY = targetEntity.getY();
                                targetZ = targetEntity.getZ();
                                validTarget = true;
                            }
                        }
                    } catch (IllegalArgumentException ignored) {
                        // Invalid UUID format
                    }
                }

                if (validTarget) {
                    // Calculate distance
                    double distSq = this.squaredDistanceTo(targetX, targetY, targetZ);

                    // Use the dynamic radius squared instead of hardcoded 1
                    double radiusSq = targetCp.radius * targetCp.radius;

                    // Check if within the defined radius
                    if (distSq <= radiusSq) {
                        this.currentCheckpointIndex++;
                        TestMod.LOGGER.info("Entity reached checkpoint " + this.currentCheckpointIndex);

                        // If all checkpoints are reached
                        if (this.currentCheckpointIndex >= config.checkpoints.size()) {
                            TestMod.LOGGER.info("Entity completed all checkpoints!");
                            ScriptingConfigManager.EntityActions actions = ScriptingConfigManager.getInstance().getActions(this.getUuid());
                            executeServerAction(actions.executeCorrect);

                            // Stop the script/checkpoints from firing continuously after success
                            this.isScriptRunning = false;
                        }
                    }
                }
            }
        }
    }

    @Unique
    private void executeServerAction(ScriptingConfigManager.ActionEventData actionData) {
        if (actionData.maxExecutions == 0 || actionData.currentExecutions < actionData.maxExecutions) {
            if (actionData.commands != null && !actionData.commands.trim().isEmpty()) {
                MinecraftServer server = this.getWorld().getServer();
                if (server != null) {
                    ServerCommandSource source = this.getCommandSource().withLevel(4);

                    String[] commandsToRun = actionData.commands.split("\\r?\\n");
                    for (String cmd : commandsToRun) {
                        cmd = cmd.trim();
                        if (!cmd.isEmpty()) {
                            if (cmd.startsWith("/")) cmd = cmd.substring(1);
                            server.getCommandManager().executeWithPrefix(source, cmd);
                        }
                    }
                }
            }
            actionData.currentExecutions++;
            ScriptingConfigManager.getInstance().markDirty();
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