package net.linkdarkar.testmod.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.linkdarkar.testmod.scripting.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public class ModNetworking {
    public static final Identifier EXECUTE_ONCE_ID = Identifier.of("testmod", "execute_once");
    public static final Identifier SET_TICKING_ID = Identifier.of("testmod", "set_ticking");

    public record ExecuteOncePayload(UUID entityUuid, NbtCompound scriptNbt) implements CustomPayload {
        public static final CustomPayload.Id<ExecuteOncePayload> ID = new CustomPayload.Id<>(Identifier.of("testmod", "execute_once"));
        public static final PacketCodec<RegistryByteBuf, ExecuteOncePayload> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, ExecuteOncePayload::entityUuid,
                PacketCodecs.NBT_COMPOUND, ExecuteOncePayload::scriptNbt,
                ExecuteOncePayload::new
        );
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SetTickingPayload(UUID entityUuid, boolean shouldRun, NbtCompound scriptNbt) implements CustomPayload {
        public static final CustomPayload.Id<SetTickingPayload> ID = new CustomPayload.Id<>(Identifier.of("testmod", "set_ticking"));
        public static final PacketCodec<RegistryByteBuf, SetTickingPayload> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, SetTickingPayload::entityUuid,
                PacketCodecs.BOOL, SetTickingPayload::shouldRun,
                PacketCodecs.NBT_COMPOUND, SetTickingPayload::scriptNbt,
                SetTickingPayload::new
        );
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void registerC2SPackets() {
        PayloadTypeRegistry.playC2S().register(ExecuteOncePayload.ID, ExecuteOncePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetTickingPayload.ID, SetTickingPayload.CODEC);

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

                if (!(entity instanceof IScriptableEntity)) {
                    System.out.println("ERROR: Entity " + entity.getName().getString() + " is NOT scriptable! Mixin failed.");
                    return;
                }

                if (entity instanceof IScriptableEntity scriptable) {
                    if (payload.shouldRun()) {
                        ScriptBuilder builder = ScriptBuilder.fromNbt(payload.scriptNbt());
                        scriptable.setStoredScript(builder.GetScript());
                        scriptable.setScriptRunning(true);
                        context.player().sendMessage(net.minecraft.text.Text.literal("Script Loop STARTED"), false);
                    } else {
                        scriptable.setScriptRunning(false);
                        context.player().sendMessage(net.minecraft.text.Text.literal("Script Loop STOPPED"), false);
                    }
                }
            });
        });
    }
}