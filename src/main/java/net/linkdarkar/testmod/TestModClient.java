package net.linkdarkar.testmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.linkdarkar.testmod.entity.ModEntities;
import net.linkdarkar.testmod.entity.client.CustomNPCRenderer;
import net.linkdarkar.testmod.screen.custom.ScreenReturnHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.linkdarkar.testmod.scripting.ScriptActorManager;

public class TestModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        EntityRendererRegistry.register(ModEntities.NPC_REIMU_TEST, CustomNPCRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(ScreenReturnHandler::tick);

        // Event: Player joins a world (Singleplayer or Multiplayer)
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ScriptActorManager.load();
        });

        // Event: Player disconnects (Back to title screen)
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ScriptActorManager.unload();
        });
    }
}
