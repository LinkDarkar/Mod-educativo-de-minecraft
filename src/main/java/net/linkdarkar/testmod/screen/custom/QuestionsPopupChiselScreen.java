package net.linkdarkar.testmod.screen.custom;

import net.linkdarkar.testmod.item.custom.questionHandler.QuestionData;
import net.linkdarkar.testmod.item.custom.questionHandler.QuestionLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.List;

public class QuestionsPopupChiselScreen extends Screen {
    private QuestionData question;
    private int currentQuestion = 0;
    private final List<QuestionData> questionList;

    float progress;

    public QuestionsPopupChiselScreen() {
        super(Text.literal("Quiz"));

        // TODO change this to allow for other files more dynamically or smth
        this.questionList = QuestionLoader.LoadQuestions();
    }

    @Override
    protected void init() {
        super.init();
        try {
            // TODO call function prepareQuestion? so we can call it when pressing the button too
            this.prepareQuestion();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void prepareQuestion() {
        this.question = QuestionLoader.getQuestion(questionList, currentQuestion);
        if (this.question == null) {
            this.close();
            return;
        }

        if (!this.question.answers.isEmpty() || this.question.type == 0) {
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
        }
        else {
            TextFieldWidget input = new TextFieldWidget(
                    textRenderer,
                    this.question.textBoxXPos,
                    this.question.textBoxYPos,
                    this.question.textBoxWidth,
                    this.question.textBoxHeight,
                    Text.literal("Answer")
            );
            input.setMaxLength(256);
            this.addDrawableChild(input);

            // TODO make the dimensions easier on the eyes by making the parameters be calculated inside the QuestionData class?
            this.addDrawableChild(
                    ButtonWidget.builder(
                                    Text.literal("submitTest"),
                                    button -> {
                                        this.onSubmitClicked();
                                    }
                            )
                            .dimensions(this.question.textBoxXPos + this.question.textBoxWidth + 10, this.question.textBoxYPos, 100, 20)
                            .build()
            );
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

        this.currentQuestion += 1;
        this.prepareQuestion();
    }

    private void onSubmitClicked() {
        assert this.client != null;
        if (client.player == null) {
            return;
        }

        this.currentQuestion += 1;
        this.prepareQuestion();

        // TODO add check to see if answer is correct or not
        client.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        client.player.sendMessage(Text.literal("TEST SUCCESSFUL"));
    }

    @Override
    public void render (DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // renders the question text wow
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
