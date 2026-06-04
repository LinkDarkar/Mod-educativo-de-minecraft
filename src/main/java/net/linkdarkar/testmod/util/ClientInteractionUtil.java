package net.linkdarkar.testmod.util;

import net.linkdarkar.testmod.entity.custom.CustomNPCEntity;
import net.linkdarkar.testmod.vn.VisualNovelDialogueScreen;
import net.minecraft.client.MinecraftClient;

public class ClientInteractionUtil {

    public static void copyToClipboard(String text) {
        if (MinecraftClient.getInstance() != null && MinecraftClient.getInstance().keyboard != null) {
            MinecraftClient.getInstance().keyboard.setClipboard(text);
        }
    }

    public static void openDialogueScreen(CustomNPCEntity npc, String path) {
        if (MinecraftClient.getInstance() != null) {
            MinecraftClient.getInstance().setScreen(new VisualNovelDialogueScreen(npc, path));
        }
    }
}