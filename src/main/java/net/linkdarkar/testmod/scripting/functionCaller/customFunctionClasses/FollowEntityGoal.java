package net.linkdarkar.testmod.scripting.functionCaller.customFunctionClasses;

import net.linkdarkar.testmod.TestMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;

public class FollowEntityGoal extends Goal {
    private final MobEntity mobEntity;
    private final LivingEntity target;
    private final double speed;
    private boolean needsToExecute = false;

    public FollowEntityGoal(MobEntity mobEntity, LivingEntity target, double speed, boolean needsToExecute) {
        this.mobEntity = mobEntity;
        this.target = target;
        this.speed = speed;
        this.needsToExecute = needsToExecute;
    }

    @Override
    public boolean canStart() {
        return this.target.isAlive() && this.mobEntity.squaredDistanceTo(target) > 7.0;
    }
    @Override
    public boolean shouldContinue() {
        return this.target.isAlive() && this.mobEntity.squaredDistanceTo(target) > 7.0;
    }

    @Override
    public void stop() {
        this.mobEntity.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.needsToExecute) {
            this.mobEntity.getLookControl().lookAt(this.target, 10.0f, this.mobEntity.getMaxLookPitchChange());
            this.mobEntity.getNavigation().startMovingTo(this.target, speed);
            TestMod.LOGGER.info("squared distance to target: "+ this.mobEntity.squaredDistanceTo(target));
            this.needsToExecute = false;
        }
        else {
            this.stop();
        }
    }
}
