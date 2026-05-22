package net.linkdarkar.testmod.scripting;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ScriptSummonCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("scriptsummon")
                .requires(source -> source.hasPermissionLevel(2))
                .then(argument("entity", IdentifierArgumentType.identifier())
                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)

                        // BRANCH 1: /scriptsummon <entity> <data> (Spawns at player's location)
                        .then(argument("data", NbtCompoundArgumentType.nbtCompound())
                                .executes(ctx -> executeSummon(
                                        ctx,
                                        IdentifierArgumentType.getIdentifier(ctx, "entity"),
                                        NbtCompoundArgumentType.getNbtCompound(ctx, "data"),
                                        null,
                                        null))
                        )

                        // BRANCH 2 & 3: Position arguments added
                        .then(argument("pos", Vec3ArgumentType.vec3())
                                // BRANCH 2: /scriptsummon <entity> <pos> <data>
                                .then(argument("data", NbtCompoundArgumentType.nbtCompound())
                                        .executes(ctx -> executeSummon(
                                                ctx,
                                                IdentifierArgumentType.getIdentifier(ctx, "entity"),
                                                NbtCompoundArgumentType.getNbtCompound(ctx, "data"),
                                                Vec3ArgumentType.getVec3(ctx, "pos"),
                                                null))
                                )

                                // BRANCH 3: /scriptsummon <entity> <pos> <rot> <data>
                                .then(argument("rot", RotationArgumentType.rotation())
                                        .then(argument("data", NbtCompoundArgumentType.nbtCompound())
                                                .executes(ctx -> executeSummon(
                                                        ctx,
                                                        IdentifierArgumentType.getIdentifier(ctx, "entity"),
                                                        NbtCompoundArgumentType.getNbtCompound(ctx, "data"),
                                                        Vec3ArgumentType.getVec3(ctx, "pos"),
                                                        RotationArgumentType.getRotation(ctx, "rot").toAbsoluteRotation(ctx.getSource())))
                                        )
                                )
                        )
                )
        );
    }

    // Helper method to keep the logic clean across all three command branches
    private static int executeSummon(CommandContext<ServerCommandSource> context, Identifier entityId, NbtCompound data, Vec3d spawnPos, Vec2f spawnRot) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        EntityType<?> type = Registries.ENTITY_TYPE.get(entityId);
        if (type == null) {
            source.sendError(Text.literal("Unknown entity type: " + entityId));
            return 0;
        }

        // Instantiates the entity
        Entity entity = type.create(source.getWorld());
        if (entity == null) {
            source.sendError(Text.literal("Failed to create entity."));
            return 0;
        }

        // --- Vanilla NBT Support ---
        NbtCompound entityNbt = entity.writeNbt(new NbtCompound());
        NbtCompound vanillaNbt = data.copy();

        // Remove script data so it doesn't pollute the vanilla entity memory
        vanillaNbt.remove("script_current");
        vanillaNbt.remove("script_correct");
        vanillaNbt.remove("script_default");
        vanillaNbt.remove("meta");

        entityNbt.copyFrom(vanillaNbt);

        // Prevent the command from accidentally overriding the UUID
        entityNbt.putUuid("UUID", entity.getUuid());

        entity.readNbt(entityNbt);
        // ---------------------------

        // Fallback to the executor's position/rotation if none were provided in the command
        if (spawnPos == null) spawnPos = source.getPosition();
        if (spawnRot == null) spawnRot = source.getRotation();

        // Note: Vec2f maps X to Pitch, and Y to Yaw.
        entity.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, spawnRot.y, spawnRot.x);

        // Spawn it into the world
        source.getWorld().spawnEntity(entity);

        UUID newUuid = entity.getUuid();
        UUID correctUuid = new UUID(newUuid.getMostSignificantBits(), ~newUuid.getLeastSignificantBits());
        UUID defaultUuid = new UUID(~newUuid.getMostSignificantBits(), newUuid.getLeastSignificantBits());

        // Add Scripts
        if (data.contains("script_current")) {
            ScriptActorManager.getInstance().saveBuilder(newUuid, ScriptBuilder.fromNbt(data.getCompound("script_current")));
        }
        if (data.contains("script_correct")) {
            ScriptActorManager.getInstance().saveBuilder(correctUuid, ScriptBuilder.fromNbt(data.getCompound("script_correct")));
        }
        if (data.contains("script_default")) {
            ScriptActorManager.getInstance().saveBuilder(defaultUuid, ScriptBuilder.fromNbt(data.getCompound("script_default")));
        }

        // Add Configuration and Actions
        if (data.contains("meta")) {
            ScriptingConfigManager.getInstance().importAllFromNbt(newUuid, data.getCompound("meta"));
        }

        Vec3d finalSpawnPos = spawnPos;
        source.sendFeedback(() -> Text.literal(String.format("Spawned Scriptable NPC at [%.2f, %.2f, %.2f]", finalSpawnPos.x, finalSpawnPos.y, finalSpawnPos.z)), false);
        return 1;
    }
}