package net.linkdarkar.testmod.scripting.functionCaller.customFunctionClasses;

import net.linkdarkar.testmod.TestMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;

public class FollowEntityGoal extends Goal {
    private final MobEntity mobEntity;
    private final LivingEntity target;
    private final double speed;

    public FollowEntityGoal(MobEntity mobEntity, LivingEntity target, double speed) {
        this.mobEntity = mobEntity;
        this.target = target;
        this.speed = speed;
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
        mobEntity.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.mobEntity.getLookControl().lookAt(this.target, 10.0f, this.mobEntity.getMaxLookPitchChange());
        this.mobEntity.getNavigation().startMovingTo(this.target, speed);
        TestMod.LOGGER.info("squared distance to target: "+ this.mobEntity.squaredDistanceTo(target));
    }
}
