package net.linkdarkar.testmod.scripting.functionCaller.customFunctionClasses;

import net.minecraft.block.Block;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;

public class BreakBlockGoal extends Goal {
    MobEntity mobEntity;
    BlockPos blockPos;

    public BreakBlockGoal (MobEntity mobEntity, BlockPos blockPos) {
        this.mobEntity = mobEntity;
        this.blockPos = blockPos;
    }

    @Override
    public boolean canStart() {
        return mobEntity.getWorld().getBlockState(blockPos).isSolidBlock(mobEntity.getWorld(), blockPos);
//        return false;
    }

    @Override
    public void tick() {
        double distanceToBlock = mobEntity.squaredDistanceTo(
                blockPos.getX() + 0.5,
                blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5
        );

        this.mobEntity.getLookControl().lookAt(
                blockPos.getX() + 1,
                blockPos.getY() + 1,
                blockPos.getZ() + 1
        );

        if (distanceToBlock > 6.0) {
            this.mobEntity.getNavigation().startMovingTo(
                    blockPos.getX(),
                    blockPos.getY(),
                    blockPos.getZ(),
                    1.0
            );

            return;
        }

        this.mobEntity.getWorld().breakBlock(this.blockPos, true, this.mobEntity);
    }
}
