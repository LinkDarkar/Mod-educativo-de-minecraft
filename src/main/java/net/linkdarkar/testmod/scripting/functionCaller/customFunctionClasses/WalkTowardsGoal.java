package net.linkdarkar.testmod.scripting.functionCaller.customFunctionClasses;

import net.linkdarkar.testmod.TestMod;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

public class WalkTowardsGoal extends Goal {
    private final MobEntity mobEntity;
    private final double speed;
    private boolean needsToExecute = false;
    private final Vec3d target;

    public WalkTowardsGoal (MobEntity mobEntity, double speed, Vec3d target, boolean needsToExecute) {
        this.mobEntity = mobEntity;
        this.speed = speed;
        this.target = target;
        this.needsToExecute = needsToExecute;
    }

    @Override
    public boolean canStart() {
        return true;
    }

    @Override
    public void start() {
        this.mobEntity.getNavigation().startMovingTo(this.target.getX(), this.target.getY(), this.target.getZ(), this.speed);
    }

    @Override
    public void stop() {
        this.mobEntity.getNavigation().stop();
    }

    @Override
    public boolean shouldContinue() {
//        TestMod.LOGGER.info("DistToTrgt: "+this.mobEntity.squaredDistanceTo(this.target));
        return 0.1 < this.mobEntity.squaredDistanceTo(this.target);
    }

    @Override
    public void tick () {
        if (this.needsToExecute) {
            this.mobEntity.getNavigation().startMovingTo(this.target.getX(), this.target.getY(), this.target.getZ(), this.speed);
            TestMod.LOGGER.info("walking Towards: "+ this.target);
            this.needsToExecute = false;
        }
        else {
            this.stop();
        }
    }


}
