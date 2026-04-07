package net.linkdarkar.testmod.vn;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.entity.custom.CustomNPCEntity;
import net.linkdarkar.testmod.networking.ModNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.*;

public class VisualNovelDialogueScreen extends Screen {
    private CustomNPCEntity npcEntity;

    private String dialogue;

    private final VisualNovelDialogueData dialogueData;
    private final Map<String, VisualNovelDialogueData.Node> dialogueNodeMap = new HashMap<>();
    private VisualNovelDialogueData.Node currentNode;

    private int latestCharIndex = 0;
    private int currentPointToStop;
    private int currentPointToStopIndex = 0;

    private int tickCounter = 0;
    private boolean waitingToContinue = false;

    public VisualNovelDialogueScreen() {
        super(Text.literal("Dialogue"));
        this.dialogueData = VisualNovelDialogueLoader.loadDialogue("testDialogue/dialogue_test_01.json");
        for (VisualNovelDialogueData.Node node : dialogueData.Nodes) {
            dialogueNodeMap.put(node.id, node);
        }
        VisualNovelDialogueLoader.mergeTextLines(this.dialogueData);
    }

    public VisualNovelDialogueScreen(String path) {
        super(Text.literal("Dialogue"));
        System.out.println(path);
        this.dialogueData = VisualNovelDialogueLoader.loadDialogue(path);
        for (VisualNovelDialogueData.Node node : dialogueData.Nodes) {
            dialogueNodeMap.put(node.id, node);
        }
        VisualNovelDialogueLoader.mergeTextLines(this.dialogueData);
    }

    public VisualNovelDialogueScreen(CustomNPCEntity npcEntity, String path) {
        super(Text.literal("Dialogue"));
        System.out.println(path);
        this.npcEntity = npcEntity;
        this.dialogueData = VisualNovelDialogueLoader.loadDialogue(path);
        for (VisualNovelDialogueData.Node node : dialogueData.Nodes) {
            dialogueNodeMap.put(node.id, node);
        }
        VisualNovelDialogueLoader.mergeTextLines(this.dialogueData);
    }

    // TODO problema aquí para que no inicie siempre desde 0
    protected void init() {
        super.init();
        // this.setupForCurrent();  // this should be the thing that sets up buttons on the screen and stuff? in any case, it is not what draws the dialogue, that goes in render
        TestMod.LOGGER.info("se inicia");
        this.currentNode = this.dialogueData.Nodes.getFirst();
        this.currentPointToStop = this.currentNode.indexOfCharToStop.getFirst();

        this.latestCharIndex = 0;
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        VisualNovelDialogueData.Node savedCurrNode = this.currentNode;
        int savedLatestCharIndex = this.latestCharIndex;
        int savedCurrentPointToStop = this.currentPointToStop;
        int savedCurrentPointToStopIndex = this.currentPointToStopIndex;
        boolean savedWaitingToContinue = this.waitingToContinue;

        super.resize(client, width, height);

        this.currentNode = savedCurrNode;
        this.latestCharIndex = savedLatestCharIndex;
        this.currentPointToStop = savedCurrentPointToStop;
        this.currentPointToStopIndex = savedCurrentPointToStopIndex;
        this.waitingToContinue = savedWaitingToContinue;
    }

    @Override
    public void render (DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        // draw portrait
        // draw dialogue box
        this.drawDialogueBox(context);
        // draw name tag
        // typing animation to reveal characters over time
        this.getShowableText();
        this.renderVisibleText(context);
    }

    private void drawDialogueBox (DrawContext context) {
        // TODO change shit
        int boxX = 20;
        int boxY = 20;
        int boxW = this.width - 40;
        int boxH = this.height - 40;

        // draws the box following the "coordinates" on the screen, could get more creative with this
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xAA000000);
    }

    private void getShowableText() {
        if (this.latestCharIndex < this.currentNode.finalText.length()) {
            if (this.tickCounter % 3 == 0) {
                // TODO make this configurable
                int advance = 1;

                this.latestCharIndex = Math.min(
                        Math.min(this.currentNode.finalText.length(), this.currentPointToStop),
                        this.latestCharIndex + advance
                );

                if (latestCharIndex >= this.currentPointToStop) {
                    this.waitingToContinue = true;
                }
            }

            this.tickCounter += 1;
        }
    }

    private void renderVisibleText(DrawContext context) {
        int safeIndex = Math.min(this.latestCharIndex, this.currentNode.finalText.length());
        String visibleText = this.currentNode.finalText.substring(0, safeIndex);
        String[] lines = visibleText.split("\n");

        // After manually splitting the lines, we split them again according to the screen width
        List<OrderedText> linesToWrap = new ArrayList<>();
        for (String line : lines) {
            linesToWrap.addAll(
                    textRenderer.wrapLines(Text.literal(line), this.width - 60)
            );
        }

        // TODO replace magical numbers with real shit
        int textStartX = 20 + 12;
        int textY = 20 + 30;

        for (OrderedText line : linesToWrap) {
            context.drawTextWithShadow(
                    this.textRenderer,
                    line,
                    textStartX,
                    textY, // TODO need to replace the magical number (12) with a variable like on the other item
                    0xFFFFFF
            );

            textY += this.textRenderer.fontHeight;
        }
    }

    private void advanceNode() {
        // manda el paquete al servidor en caso de que tenga que ejecutar comandos al final del nodo
        if (this.currentNode.executeCommands != null)
        {
            for (String commandLine : this.currentNode.executeCommands)
            {
                if (!commandLine.isEmpty())
                {
                    ClientPlayNetworking.send(
                            new ModNetworking.ExecuteCommandFromDialoguePayload(this.npcEntity.getUuid(), commandLine)
                    );
                }
            }
        }

        this.currentNode = this.dialogueNodeMap.get(this.currentNode.nextNodeId);
        if (this.currentNode == null) {
            this.close();
            return;
        }

        this.latestCharIndex = 0;
        this.tickCounter = 0;
        this.currentPointToStopIndex = 0;
        this.currentPointToStop = this.currentNode.indexOfCharToStop.getFirst();
        this.waitingToContinue = false;

        if (this.client != null && this.client.player != null) {
            this.client.player.playSound(SoundEvents.UI_TOAST_IN, 1.0f, 1.0f);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // check when they are the same
        if (this.currentPointToStop <= this.latestCharIndex) {
            // TODO fix this because it will break
            if (this.currentNode.finalText.length() <= this.latestCharIndex) {
//                TestMod.LOGGER.info("Page end");
                this.advanceNode();
            }
            else
            {
//                TestMod.LOGGER.info("Paragraph end");
                this.currentPointToStopIndex += 1;
                if (this.currentPointToStopIndex < this.currentNode.indexOfCharToStop.size()) {
                    this.currentPointToStop = this.currentNode.indexOfCharToStop.get(this.currentPointToStopIndex);
                }
                else {
                    this.currentPointToStop = this.currentNode.finalText.length();
                }
            }
            this.waitingToContinue = false;
            return true;
        }
        else if (!this.waitingToContinue) {
            this.latestCharIndex = this.currentPointToStop;
            this.waitingToContinue = true;
            return true;
        }

        // this.advanceNode();
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
