package net.linkdarkar.testmod.item.custom;

import net.linkdarkar.testmod.block.ModBlocks;
import net.linkdarkar.testmod.mixin.MobEntityAccessor;
import net.linkdarkar.testmod.screen.custom.ScriptingDebugScreen;
import net.linkdarkar.testmod.screen.custom.ScriptingScreen;
import net.linkdarkar.testmod.scripting.functionCaller.FunctionCaller;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class ChiselItem extends Item {
    private List<Pair<String, Vec3d>> mobPositionList = new ArrayList<>();

    public ChiselItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        World world = user.getWorld();

        // Things like opening the screen must happen only if it's the client
        if (world.isClient()) {
            MinecraftClient client = MinecraftClient.getInstance();

            if (entity.isAlive()) {
                if (user.isSneaking())
                {

                }
                else
                {
                    // Should call this to avoid crash
                    client.execute(() -> {
                        this.killAi(user, entity);
                        client.setScreen(new ScriptingScreen(entity));
                    });
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        }

        // (Everything below this point only runs on the Server)

        if (entity.isAlive()) {
            // user.sendMessage(Text.literal("Entity UUID >> " + entity.getUuidAsString()));
            entity.setGlowing(false);

            return ActionResult.SUCCESS;
        } else {
            user.sendMessage(Text.literal("is NOT alive???"));
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (world.isClient()) return;
        if (!(world instanceof ServerWorld serverWorld)) return;

        if (!(entity instanceof ServerPlayerEntity player)) return;

        if (this.mobPositionList == null || this.mobPositionList.isEmpty()) return;
        if (this.mobPositionList.getFirst() == null) return;

        MobEntity mobEntity = this.getMobEntityFromUUID(serverWorld, this.mobPositionList.getFirst().getLeft());
        if (mobEntity != null) {
            this.navigateToPoint(serverWorld, mobEntity, player);
        }
        else {
            player.sendMessage(Text.literal("entity dead"));
            this.mobPositionList.clear();
        }
    }

    private void setupNavigationFromPointToPointTest(MobEntity mobEntity) {
        Vec3d mobEntityInitialPos = mobEntity.getPos();
        String entityUUID = mobEntity.getUuidAsString();

        // sets up objectives for movement test
        /*
         * (x + 10, 0, z + 10)
         * (x + 25, 0, z + 10)
         * (x, y, z)
         */
        this.mobPositionList.add(new Pair<String,Vec3d>(entityUUID,
                new Vec3d(mobEntityInitialPos.x + 10, mobEntityInitialPos.y, mobEntityInitialPos.z + 10)));
        this.mobPositionList.add(new Pair<String,Vec3d>(entityUUID,
                new Vec3d(mobEntityInitialPos.x + 25, mobEntityInitialPos.y, mobEntityInitialPos.z + 10)));
        this.mobPositionList.add(new Pair<String,Vec3d>(entityUUID,
                new Vec3d(mobEntityInitialPos.x, mobEntityInitialPos.y, mobEntityInitialPos.z)));
    }

    private MobEntity getMobEntityFromUUID(World world, String uuid) {
        UUID targetUUID = UUID.fromString(uuid);
        Entity targetEntity = ((ServerWorld) world).getEntity(targetUUID);
        return (MobEntity) targetEntity;
    }

    private void navigateToPoint(ServerWorld serverWorld, MobEntity mobEntity, ServerPlayerEntity player) {
        if (serverWorld.isClient()) return;
        if (!(mobEntity instanceof PathAwareEntity pathEntity)) {
            player.sendMessage(Text.literal("Entity cannot navigate."), false);
            return;
        }

        Vec3d currentObjective = this.mobPositionList.getFirst().getRight();
        mobEntity.getNavigation().startMovingTo(currentObjective.x, currentObjective.y, currentObjective.z, 2);

        if (mobEntity.getPos().distanceTo(currentObjective) < 2.0) {
            this.mobPositionList.removeFirst();
            player.sendMessage(Text.literal("Removed objective").formatted(Formatting.GREEN), false);
        }
    }

    private void killAi(PlayerEntity user, LivingEntity entity) {
        user.sendMessage(Text.literal("KILLED AI"));
        ((MobEntity)entity).clearGoalsAndTasks();
    }
}
