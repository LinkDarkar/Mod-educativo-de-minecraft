package net.linkdarkar.testmod.scripting.functionCaller;

import net.linkdarkar.testmod.mixin.MobEntityAccessor;
import net.linkdarkar.testmod.scripting.functionCaller.customFunctionClasses.BreakBlockGoal;
import net.linkdarkar.testmod.scripting.functionCaller.customFunctionClasses.FollowEntityGoal;
import net.linkdarkar.testmod.scripting.functionCaller.customFunctionClasses.WalkForwardGoal;
import net.linkdarkar.testmod.scripting.functionCaller.customFunctionClasses.WalkTowardsGoal;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.FollowMobGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

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
        mobEntityAccessor.getGoalSelector().add(1, new FollowEntityGoal(mobEntity, target, speed, true));
        return true;
    }

    public static boolean follow(Entity followerEntity, Entity targetEntity, double speed) {

        if (!(followerEntity instanceof MobEntity mobEntity)) return false;
        if (!(targetEntity instanceof LivingEntity target)) return false;
        if(!(target.isAlive())) return false;

        MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) followerEntity;
        mobEntityAccessor.getGoalSelector().add(1, new FollowEntityGoal(mobEntity, target, speed, true));
        return true;
    }

    public static boolean breakBlock(ServerWorld serverWorld, UUID entityUuid, BlockPos blockPos) {
        Entity entity = serverWorld.getEntity(entityUuid);

        if (!(entity instanceof MobEntity mobEntity)) return false;

        MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) mobEntity;
        mobEntityAccessor.getGoalSelector().add(1, new BreakBlockGoal(mobEntity, blockPos));
        return true;
    }

    public static boolean walkTowards(Entity entity, double speed, Vec3d target) {
        if (!(entity instanceof MobEntity mobEntity)) return false;

        MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) mobEntity;
        mobEntityAccessor.getGoalSelector().add(1, new WalkTowardsGoal(mobEntity, speed, target, true));
        return true;
    }

    public static boolean walkForward(Entity entity, double speed, int distance) {
        if (!(entity instanceof MobEntity mobEntity)) return false;

        MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) mobEntity;
        mobEntityAccessor.getGoalSelector().add(1, new WalkForwardGoal(mobEntity, speed, entity.getPos(), distance));
        return true;
    }

    public static boolean aux_forceLookAt(Entity entity, Vec3d desiredLookDir) {
        if (!(entity instanceof MobEntity mobEntity)) return false;
        // if (desiredLookDir == Direction.NORTH)

        mobEntity.getLookControl().lookAt(desiredLookDir);

//        NORTH(2, 3, 2, "north", Direction.AxisDirection.NEGATIVE, Direction.Axis.Z, new Vec3i(0, 0, -1)),
//        SOUTH(3, 2, 0, "south", Direction.AxisDirection.POSITIVE, Direction.Axis.Z, new Vec3i(0, 0, 1)),
//        WEST(4, 5, 1, "west", Direction.AxisDirection.NEGATIVE, Direction.Axis.X, new Vec3i(-1, 0, 0)),
//        EAST(5, 4, 3, "east", Direction.AxisDirection.POSITIVE, Direction.Axis.X, new Vec3i(1, 0, 0));
        return true;
    }

    public static boolean lookAtEntity(ServerWorld serverWorld, UUID followerUuid, UUID targetUuid) {
        Entity entity = serverWorld.getEntity(followerUuid);
        Entity targetEntity = serverWorld.getEntity(targetUuid);

        if (!(entity instanceof MobEntity mobEntity)) return false;

        mobEntity.getLookControl().lookAt(targetEntity);
        return true;
    }

}
