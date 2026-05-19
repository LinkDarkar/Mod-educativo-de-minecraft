package net.linkdarkar.testmod.screen.custom;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.item.custom.questionHandler.QuestionData;
import net.linkdarkar.testmod.item.custom.questionHandler.QuestionLoader;
import net.linkdarkar.testmod.item.custom.questionHandler.QuizData;
import net.linkdarkar.testmod.networking.ModNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class QuestionsPopupChiselScreen extends Screen {
    private QuizData quizData;
    private QuestionData question;
    private int currentQuestion = 0;
    private final List<QuestionData> questionList;

    float progress;
    int countCorrect = 0;

    private boolean showExplanation = false;

    // this is here because the process to clear it kinda requires it?
    // idk what I'm doing but maybe just the killChildren is enough?
    // I don't want to try it, it's a pain
    private TextFieldWidget textInput;
    private ButtonWidget submitButton;
    private ButtonWidget nextQuestionButton;

    public QuestionsPopupChiselScreen() {
        super(Text.literal("Quiz"));
        this.quizData = QuestionLoader.LoadQuestions();
        this.questionList = this.quizData.questions;
    }

    public QuestionsPopupChiselScreen(String path) {
        super(Text.literal("Quiz"));
        this.quizData = QuestionLoader.LoadQuestions(path);
        this.questionList = this.quizData.questions;
    }

    @Override
    protected void init() {
        super.init();
        try {
            this.prepareQuestion();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void prepareQuestion() {
        this.question = QuestionLoader.getQuestion(questionList, currentQuestion);
        if (this.question == null) {
            this.endQuiz();
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
            this.textInput = new TextFieldWidget(
                    textRenderer,
                    this.question.textBoxXPos,
                    this.question.textBoxYPos,
                    this.question.textBoxWidth,
                    this.question.textBoxHeight,
                    Text.literal("Answer")
            );
            this.textInput.setMaxLength(256);
            this.addDrawableChild(this.textInput);

            this.submitButton = ButtonWidget.builder(
                    Text.literal("submit"),
                    button -> {
                        this.onSubmitClicked(this.textInput.getText());
                    }
            )
            .dimensions(this.question.textBoxXPos + this.question.textBoxWidth + 10, this.question.textBoxYPos, 100, 20)
            .build();
            this.addDrawableChild(this.submitButton);
        }

        this.nextQuestionButton = ButtonWidget.builder(
                Text.literal("Next Question"),
                button -> {
                    this.onNextQuestionClicked();
                }
        )
        .dimensions(this.width - 120, this.height - 40, 100, 20)
        .build();
        this.addDrawableChild(this.nextQuestionButton);
        this.nextQuestionButton.active = false;
    }

    // is there a real reason to have onAnswer and onSubmit as different methods??
    private void onAnswerClicked(boolean isCorrect) {
        assert this.client != null;
        if (client.player == null) {
            return;
        }

        if (isCorrect) {
            client.player.sendMessage(Text.literal("CORRECT!!!"));
            client.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            this.countCorrect += 1;
        }
        else {
            client.player.sendMessage(Text.literal("INCORRECT"));
            client.player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }

        this.nextQuestionButton.active = true;
    }
    private void onSubmitClicked(String answer) {
        assert this.client != null;
        if (client.player == null) {
            return;
        }
        if (this.textInput == null) {
            return;
        }
        if (this.submitButton == null) {
            return;
        }
        if (answer.isEmpty()) {
            return;
        }

        this.textInput.setEditable(false);
        this.submitButton.active = false;

        if (this.question.answer.equals(answer)) {
            this.textInput.setUneditableColor(0x00FF00);
            client.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            client.player.sendMessage(Text.literal("Respuesta correcta!: " + answer));
            this.countCorrect += 1;
        }
        else {
            this.textInput.setUneditableColor(0xFF0000);
            client.player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            client.player.sendMessage(Text.literal("Respuesta equivocada"));
        }

        this.showExplanation = true;
        this.nextQuestionButton.active = true;
    }

    private void onNextQuestionClicked() {
        if (this.textInput != null) {
            this.textInput.setText("");
        }

        this.currentQuestion += 1;
        this.showExplanation = false;
        this.clearChildren();
        this.prepareQuestion();
    }

    private void endQuiz() {
        // TODO FIX AS SOON AS POSSIBLE, THIS IS JUST TO TEST
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        assert player != null;

        if (this.countCorrect >= this.quizData.threshold) {
            player.sendMessage(Text.literal("Quiz Passed! Scoring: " + this.countCorrect + "/" + this.quizData.threshold));

            if (this.quizData.commands != null && !this.quizData.commands.isEmpty()) {
                for (String cmd : this.quizData.commands) {
                    if (cmd != null && !cmd.trim().isEmpty()) {
                        ClientPlayNetworking.send(new ModNetworking.ExecuteCommandAsPlayerPayload(cmd));
                    }
                }
            }
        }
        else {
            player.sendMessage(Text.literal("Quiz Failed. You scored " + this.countCorrect + ", but need " + this.quizData.threshold));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // shows question text
        String[] questionLines = this.question.question.split("\n");
        List<OrderedText> questionLinesToWrap = new ArrayList<>();
        for (String line : questionLines) {
            questionLinesToWrap.addAll(
                    textRenderer.wrapLines(Text.literal(line), this.width - 60)
            );
        }

        int questionTextY = this.question.startYPos;
        for (OrderedText questionLine : questionLinesToWrap) {
            context.drawTextWithShadow(
                    this.textRenderer,
                    questionLine,
                    this.question.startXPos,
                    questionTextY,
                    0xFFFFFF
            );
            questionTextY += this.question.verticalLineSpacing;
        }

        // TODO show current progress with images
        // show current progress
        context.drawTextWithShadow(
                this.textRenderer,
                (this.currentQuestion + 1) + " / " + this.questionList.size(),
                this.width - 60,
                20,
                0xFFFFFF
        );

        // we then show the explanation
        if (this.showExplanation) {
            String[] explanationLines = this.question.explanation.split("\n");
            List<OrderedText> linesToWrap = new ArrayList<>();
            for (String line : explanationLines) {
                linesToWrap.addAll(
                        textRenderer.wrapLines(Text.literal(line), this.width - 60)
                );
            }

            int textY = this.question.textBoxYPos;
            for (OrderedText line : linesToWrap) {
                context.drawTextWithShadow(
                        this.textRenderer,
                        line,
                        this.question.textBoxXPos,
                        textY + 50,
                        0xFFFFFF
                );

                textY += this.textRenderer.fontHeight;
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
