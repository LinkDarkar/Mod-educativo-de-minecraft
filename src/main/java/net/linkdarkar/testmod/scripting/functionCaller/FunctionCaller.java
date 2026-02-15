package net.linkdarkar.testmod.scripting.functionCaller;

import net.linkdarkar.testmod.mixin.MobEntityAccessor;
import net.linkdarkar.testmod.scripting.functionCaller.customFunctionClasses.BreakBlockGoal;
import net.linkdarkar.testmod.scripting.functionCaller.customFunctionClasses.FollowEntityGoal;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.FollowMobGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public final class FunctionCaller {

    private FunctionCaller () {}

    public static boolean follow(ServerWorld serverWorld, UUID followerUuid, UUID targetUuid, double speed) {
        Entity followerEntity = serverWorld.getEntity(followerUuid);
        Entity targetEntity = serverWorld.getEntity(targetUuid);

        if (!(followerEntity instanceof MobEntity mobEntity)) return false;
        if (!(targetEntity instanceof LivingEntity target)) return false;
        if(!(target.isAlive())) return false;

        MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) followerEntity;
        mobEntityAccessor.getGoalSelector().add(1, new FollowEntityGoal(mobEntity, target, speed));
        return true;
    }

    public static boolean follow(Entity followerEntity, Entity targetEntity, double speed) {

        if (!(followerEntity instanceof MobEntity mobEntity)) return false;
        if (!(targetEntity instanceof LivingEntity target)) return false;
        if(!(target.isAlive())) return false;

        MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) followerEntity;
        mobEntityAccessor.getGoalSelector().add(1, new FollowEntityGoal(mobEntity, target, speed));
        return true;
    }

    public static boolean breakBlock(ServerWorld serverWorld, UUID entityUuid, BlockPos blockPos) {
        Entity entity = serverWorld.getEntity(entityUuid);

        if (!(entity instanceof MobEntity mobEntity)) return false;

        MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) mobEntity;
        mobEntityAccessor.getGoalSelector().add(1, new BreakBlockGoal(mobEntity, blockPos));
        return true;
    }

}
