package net.linkdarkar.testmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.linkdarkar.testmod.entity.ModEntities;
import net.linkdarkar.testmod.entity.client.CustomNPCRenderer;
import net.linkdarkar.testmod.screen.custom.ScreenReturnHandler;

public class TestModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        EntityRendererRegistry.register(ModEntities.NPC_REIMU_TEST, CustomNPCRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(ScreenReturnHandler::tick);
    }
}
