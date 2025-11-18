package net.linkdarkar.testmod.item.custom.questionHandler;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class QuestionLoader {
    private static final Gson GSON = new Gson();

    public static List<QuestionData> LoadQuestions() {
        try (InputStream stream = QuestionLoader.class.getClassLoader()
                .getResourceAsStream("assets/testmod/questions/questionsTest.json")) {
            if (stream == null) {
                throw new RuntimeException("Resource not found!");
            }

            InputStreamReader reader = new InputStreamReader(stream);
            Type wrapperType = new TypeToken<Map<String, List<QuestionData>>>() {}.getType();
            Map<String, List<QuestionData>> wrapper = GSON.fromJson(reader, wrapperType);

            return wrapper.get("questions");
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public static QuestionData getRandomQuestion(List<QuestionData> questionList) {
        if (questionList.isEmpty())
            return null;

        Random random = new Random();
        return questionList.get(random.nextInt(questionList.size()));
    }
}
