package net.linkdarkar.testmod.scripting.functionCaller.customFunctionClasses;

import net.linkdarkar.testmod.TestMod;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class WalkForwardGoal extends Goal {
    private MobEntity mobEntity;
    private double speed;
    private Vec3d initialPos;
    private int distance = 0;

    private Vec3d lookDirection;

    private Direction direction;
    private Vec3i directionVector;
    private Vec3d target;

    private boolean needsToExecute = false;

    public WalkForwardGoal (MobEntity mobEntity, double speed, Vec3d position, int distance) {
        this.mobEntity = mobEntity;
        this.speed = speed;
        this.initialPos = position;
        this.distance = distance;

        // TODO ???? do we need this?

        this.direction = mobEntity.getHorizontalFacing();
        this.directionVector = this.direction.getVector();
        this.target = mobEntity.getPos().add(
                this.directionVector.getX() * distance,
                0,
                this.directionVector.getZ() * distance
        );
        this.needsToExecute = true;
    }

    @Override
    public boolean canStart() {
        return true;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        this.mobEntity.getNavigation().stop();
    }

    @Override
    public void tick () {
        if (this.needsToExecute) {
            // will move towards that direction, and since the goal gets created every tick, it will continue until we manually stop it
            this.mobEntity.getNavigation().startMovingTo(this.target.getX(), this.target.getY(), this.target.getZ(), this.speed);
            TestMod.LOGGER.info("walking Forwards until: "+ this.target);
            this.needsToExecute = false;
        }
        else {
            this.stop();
        }

    }
}
