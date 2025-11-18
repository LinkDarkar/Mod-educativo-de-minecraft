package net.linkdarkar.testmod.screen.custom;

import net.linkdarkar.testmod.item.custom.questionHandler.QuestionData;
import net.linkdarkar.testmod.item.custom.questionHandler.QuestionLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.List;

public class QuestionsPopupChiselScreen extends Screen {
    private QuestionData question;
    private final List<QuestionData> questionList;

    public QuestionsPopupChiselScreen() {
        super(Text.literal("Quiz"));
        this.questionList = QuestionLoader.LoadQuestions();
    }

    @Override
    protected void init() {
        super.init();
        try {
            this.question = QuestionLoader.getRandomQuestion(this.questionList);
            if (this.question == null) {
                throw new RuntimeException("no question found!");
            }

            for (int i = 0; i < this.question.answers.size(); i += 1) {
                final int index = i;
                this.addDrawableChild(
                        ButtonWidget.builder(
                                Text.literal(this.question.answers.get(i).text),
                                button -> {
                                    this.onAnswerClicked(this.question.answers.get(index).isCorrect);
                                }
                        )
                        .dimensions(this.question.answers.get(i).posX, this.question.answers.get(i).posY, this.question.answers.get(i).width, this.question.answers.get(i).height)
                        .build()
                );
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void onAnswerClicked(boolean isCorrect) {
        assert this.client != null;
        if (client.player == null) {
            return;
        }

        if (isCorrect) {
            client.player.sendMessage(Text.literal("CORRECT!!!"));
            client.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        else {
            client.player.sendMessage(Text.literal("INCORRECT"));
            client.player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }

        this.close();
    }

    @Override
    public void render (DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        String[] lines = this.question.question.split("\n");
        for (int i = 0; i < lines.length; i += 1) {
            context.drawTextWithShadow(
                    this.textRenderer,
                    lines[i],
                    this.question.startXPos,
                    this.question.startYPos + (i * this.question.verticalLineSpacing),
                    0xFFFFFF
            );
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
