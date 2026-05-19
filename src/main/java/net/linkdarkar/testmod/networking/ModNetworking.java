package net.linkdarkar.testmod.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.entity.custom.CustomNPCEntity;
import net.linkdarkar.testmod.scripting.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public class ModNetworking {
    public static final Identifier EXECUTE_ONCE_ID = Identifier.of("testmod", "execute_once");
    public static final Identifier SET_TICKING_ID = Identifier.of("testmod", "set_ticking");
    public static final Identifier EXECUTE_COMMAND_FROM_DIALOGUE = Identifier.of("testmod", "execute_command_from_dialogue");

    public record ExecuteOncePayload(UUID entityUuid, NbtCompound scriptNbt) implements CustomPayload {
        public static final CustomPayload.Id<ExecuteOncePayload> ID =
                new CustomPayload.Id<>(Identifier.of("testmod", "execute_once"));
        public static final PacketCodec<RegistryByteBuf, ExecuteOncePayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, ExecuteOncePayload::entityUuid,
            PacketCodecs.NBT_COMPOUND, ExecuteOncePayload::scriptNbt,
            ExecuteOncePayload::new
        );
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SetTickingPayload(UUID entityUuid, boolean shouldRun, NbtCompound scriptNbt) implements CustomPayload {
        public static final CustomPayload.Id<SetTickingPayload> ID =
                new CustomPayload.Id<>(Identifier.of("testmod", "set_ticking"));
        public static final PacketCodec<RegistryByteBuf, SetTickingPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, SetTickingPayload::entityUuid,
            PacketCodecs.BOOL, SetTickingPayload::shouldRun,
            PacketCodecs.NBT_COMPOUND, SetTickingPayload::scriptNbt,
            SetTickingPayload::new
        );
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ExecuteCommandFromDialoguePayload(UUID entityUuid, String command) implements CustomPayload {
        public static final CustomPayload.Id<ExecuteCommandFromDialoguePayload> ID = new CustomPayload.Id<>(Identifier.of("testmod", "execute_command_from_dialogue"));

        public static final PacketCodec<RegistryByteBuf, ExecuteCommandFromDialoguePayload> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, ExecuteCommandFromDialoguePayload::entityUuid,
                PacketCodecs.STRING, ExecuteCommandFromDialoguePayload::command,
                ExecuteCommandFromDialoguePayload::new
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId()
        {
            return ID;
        }
    }
    public record ExecuteCommandAsPlayerPayload(String command) implements CustomPayload {
        public static final CustomPayload.Id<ExecuteCommandAsPlayerPayload> ID = new CustomPayload.Id<>(Identifier.of("testmod", "execute_command_as_player"));

        public static final PacketCodec<RegistryByteBuf, ExecuteCommandAsPlayerPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, ExecuteCommandAsPlayerPayload::command,
                ExecuteCommandAsPlayerPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId()
        {
            return ID;
        }
    }

    public static void registerC2SPackets() {
        PayloadTypeRegistry.playC2S().register(ExecuteOncePayload.ID, ExecuteOncePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetTickingPayload.ID, SetTickingPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ExecuteCommandFromDialoguePayload.ID, ExecuteCommandFromDialoguePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ExecuteCommandAsPlayerPayload.ID, ExecuteCommandAsPlayerPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ExecuteOncePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                Entity entity = ((net.minecraft.server.world.ServerWorld) context.player().getWorld()).getEntity(payload.entityUuid());

                if (entity instanceof MobEntity mob) {
                    ScriptBuilder builder = ScriptBuilder.fromNbt(payload.scriptNbt());
                    ExecutionContext ctx = new ExecutionContext(mob);

                    ctx.SetVar("posX", mob.getX());
                    ctx.SetVar("posY", mob.getY());
                    ctx.SetVar("posZ", mob.getZ());

                    builder.GetScript().Execute(ctx);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SetTickingPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                Entity entity = ((net.minecraft.server.world.ServerWorld) context.player().getWorld()).getEntity(payload.entityUuid());

                if (!(entity instanceof IScriptableEntity scriptable)) {
                    assert entity != null;
                    TestMod.LOGGER.info("ERROR: Entity {} is NOT scriptable! Mixin failed.", entity.getName().getString());
                    return;
                }

                if (payload.shouldRun()) {
                    ScriptBuilder builder = ScriptBuilder.fromNbt(payload.scriptNbt());
                    scriptable.setStoredScript(builder.GetScript());
                    scriptable.setScriptRunning(true);
                    context.player().sendMessage(net.minecraft.text.Text.literal("Script Loop STARTED"), false);
                } else {
                    scriptable.setScriptRunning(false);
                    context.player().sendMessage(net.minecraft.text.Text.literal("Script Loop STOPPED"), false);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ExecuteCommandFromDialoguePayload.ID, ((payload, context) -> {
            context.server().execute(() -> {
                MinecraftServer minecraftServer = context.server();
                Entity entity = ((net.minecraft.server.world.ServerWorld) context.player().getWorld()).getEntity(payload.entityUuid());
                if (!(entity instanceof CustomNPCEntity npcEntity)) {
                    assert entity != null;
                    TestMod.LOGGER.info("entity {} is not npc", entity.getName().getString());
                    return;
                }
                ServerCommandSource source = entity.getCommandSource();
                source = source.withLevel(4);
                minecraftServer.getCommandManager().executeWithPrefix(source, payload.command());
            });
        }));

        ServerPlayNetworking.registerGlobalReceiver(ModNetworking.ExecuteCommandAsPlayerPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                if (player != null) {
                    String command = payload.command();
                    if (command.startsWith("/")) {
                        command = command.substring(1);
                    }
                    player.server.getCommandManager().executeWithPrefix(player.getCommandSource().withLevel(2), command);
                }
            });
        });
    }
}