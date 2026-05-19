package net.linkdarkar.testmod.scripting;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ScriptModifyCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("scriptmodify")
                .requires(source -> source.hasPermissionLevel(2))
                .then(argument("target", EntityArgumentType.entity())
                        .then(argument("data", NbtCompoundArgumentType.nbtCompound())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    Entity entity = EntityArgumentType.getEntity(context, "target");
                                    NbtCompound data = NbtCompoundArgumentType.getNbtCompound(context, "data");

                                    UUID targetUuid = entity.getUuid();
                                    UUID correctUuid = new UUID(targetUuid.getMostSignificantBits(), ~targetUuid.getLeastSignificantBits());
                                    UUID defaultUuid = new UUID(~targetUuid.getMostSignificantBits(), targetUuid.getLeastSignificantBits());

                                    if (data.contains("script_current")) {
                                        ScriptActorManager.getInstance().saveBuilder(targetUuid, ScriptBuilder.fromNbt(data.getCompound("script_current")));
                                    }
                                    if (data.contains("script_correct")) {
                                        ScriptActorManager.getInstance().saveBuilder(correctUuid, ScriptBuilder.fromNbt(data.getCompound("script_correct")));
                                    }
                                    if (data.contains("script_default")) {
                                        ScriptActorManager.getInstance().saveBuilder(defaultUuid, ScriptBuilder.fromNbt(data.getCompound("script_default")));
                                    }

                                    if (data.contains("meta")) {
                                        ScriptingConfigManager.getInstance().importAllFromNbt(targetUuid, data.getCompound("meta"));
                                    }

                                    source.sendFeedback(() -> Text.literal("Successfully modified script data for " + entity.getName().getString()), false);
                                    return 1;
                                })
                        )
                )
        );
    }
}