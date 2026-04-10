package net.linkdarkar.testmod.scripting;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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
                                .then(argument("data", NbtCompoundArgumentType.nbtCompound())
                                        .executes(context -> {
                                            ServerCommandSource source = context.getSource();
                                            Identifier entityId = IdentifierArgumentType.getIdentifier(context, "entity");
                                            NbtCompound data = NbtCompoundArgumentType.getNbtCompound(context, "data");

                                            EntityType<?> type = Registries.ENTITY_TYPE.get(entityId);
                                            if (type == null) {
                                                source.sendError(Text.literal("Unknown entity type: " + entityId));
                                                return 0;
                                            }

                                            // .create() instantiates the entity
                                            Entity entity = type.create(source.getWorld());
                                            if (entity == null) {
                                                source.sendError(Text.literal("Failed to create entity."));
                                                return 0;
                                            }

                                            Vec3d pos = source.getPosition();
                                            entity.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0F, 0.0F);

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

                                            source.sendFeedback(() -> Text.literal("Spawned NPC with pre-loaded scripts!"), false);
                                            return 1;
                                        })
                                )
                        )
        );
    }
}