package net.linkdarkar.testmod.screen.custom;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class ScreenReturnHandler {
    public static Screen returnScreen = null;

    public static void tick (MinecraftClient client) {
        if (returnScreen != null && client.currentScreen == null) {
            client.setScreen(returnScreen);
            returnScreen = null;
        }
    }
}
