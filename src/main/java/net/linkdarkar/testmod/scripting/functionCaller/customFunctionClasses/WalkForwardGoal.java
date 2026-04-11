package net.linkdarkar.testmod.scripting.functionCaller.customFunctionClasses;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class WalkForwardGoal extends Goal {
    private MobEntity entity;
    private double speed;
    private Vec3d initialPos;
    private int distance = 0;

    private Vec3d lookDirection;

    private Direction direction;
    private Vec3i directionVector;
    private Vec3d target;

    private boolean needsToExecute = false;

    public WalkForwardGoal (MobEntity entity, double speed, Vec3d position, int distance) {
        this.entity = entity;
        this.speed = speed;
        this.initialPos = position;
        this.distance = distance;

        // TODO ???? do we need this?
        this.lookDirection = new Vec3d(
                this.entity.getLookControl().getLookX(),
                this.entity.getLookControl().getLookY(),
                this.entity.getLookControl().getLookZ()
        );

        this.direction = entity.getHorizontalFacing();
        this.directionVector = this.direction.getVector();
        this.target = entity.getPos().add(
                this.directionVector.getX() * distance,
                0,
                this.directionVector.getZ() * distance
        );
//        this.entity.getNavigation().startMovingTo();
    }

    @Override
    public boolean canStart() {
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void tick () {
        if (needsToExecute) {

        }

    }
}
