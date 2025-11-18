package net.linkdarkar.testmod.item.custom;

import net.linkdarkar.testmod.block.ModBlocks;
import net.linkdarkar.testmod.mixin.MobEntityAccessor;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class ChiselItem extends Item {
    private static final Map<Block, Block> CHISEL_MAP =
            Map.of(
                    Blocks.STONE, Blocks.STONE_BRICKS,
                    Blocks.END_STONE, Blocks.END_STONE_BRICKS,
                    Blocks.OAK_LOG, ModBlocks.PINK_GARNET_BLOCK,
                    Blocks.GOLD_BLOCK, Blocks.NETHERITE_BLOCK
            );

    private List<Pair<String, Vec3d>> mobPositionList = new ArrayList<>();

    public ChiselItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        Block clickedBlock = world.getBlockState(context.getBlockPos()).getBlock();

        if (CHISEL_MAP.containsKey(clickedBlock)) {
            if (!world.isClient()) {
                world.setBlockState(context.getBlockPos(), CHISEL_MAP.get(clickedBlock).getDefaultState());

                context.getStack().damage(1, ((ServerWorld) world), (ServerPlayerEntity) context.getPlayer(),
                        item -> context.getPlayer().sendEquipmentBreakStatus(item, EquipmentSlot.MAINHAND));

                world.playSound(null, context.getBlockPos(), SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS);
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        //Entity entity1;


        if (player != null && entity.isAlive()) {
            player.sendMessage(Text.literal("Entity UUID >> "+ entity.getUuidAsString()));
            entity.setGlowing(false);

            if (entity instanceof MobEntity mobEntity) {
                player.sendMessage(Text.literal("is path aware"));
                if (entity instanceof SheepEntity sheepEntity) {
                    MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) sheepEntity;
                    mobEntityAccessor.getGoalSelector().getGoals().clear();
                    mobEntityAccessor.getTargetSelector().getGoals().clear();
                    sheepEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,999999, 4000));
                    sheepEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 999999, 200));
                    user.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                    player.sendMessage(Text.literal("disabled goals and targets"));
                }
                else {
                    MobEntityAccessor mobEntityAccessor = (MobEntityAccessor) mobEntity;
                    mobEntityAccessor.getGoalSelector().getGoals().clear();
                    mobEntityAccessor.getGoalSelector().add(10, new LookAroundGoal(mobEntity));

                    WolfEntity wolf = (WolfEntity) mobEntity;
                    wolf.setAngryAt(null);
                    wolf.setTarget(null);
                    wolf.setAttacker(null);
                    wolf.setAngerTime(0);

                    mobEntityAccessor.getTargetSelector().getGoals().clear();

                    this.setupNavigationFromPointToPointTest(mobEntity);
                }
            }
            else {
                player.sendMessage(Text.literal("is NOT path aware"));
            }
        }
        else if (!entity.isAlive()) {
            player.sendMessage(Text.literal("is NOT alive???"));
        }

        // return super.useOnEntity(stack, user, entity, hand);
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
            // player.sendMessage(Text.literal("AAAAAAAAA").formatted(Formatting.GREEN), false);
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

        // setup objectives
        /*
         * (x + 10, 0, z + 10)
         * (x + 25, 0, z + 10)
         * (x, y, z)
         */
        this.mobPositionList.add(new Pair<String,Vec3d>(entityUUID, new Vec3d(mobEntityInitialPos.x + 10, mobEntityInitialPos.y, mobEntityInitialPos.z + 10)));
        this.mobPositionList.add(new Pair<String,Vec3d>(entityUUID, new Vec3d(mobEntityInitialPos.x + 25, mobEntityInitialPos.y, mobEntityInitialPos.z + 10)));
        this.mobPositionList.add(new Pair<String,Vec3d>(entityUUID, new Vec3d(mobEntityInitialPos.x, mobEntityInitialPos.y, mobEntityInitialPos.z)));
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
}
