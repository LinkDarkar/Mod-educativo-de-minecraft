package net.linkdarkar.testmod.vn;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.linkdarkar.testmod.item.custom.questionHandler.QuestionData;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class VisualNovelDialogueLoader {
    private static final Gson GSON = new Gson();

    public static VisualNovelDialogueData loadDialogue(String resourceName) {
        try (InputStream stream = VisualNovelDialogueLoader.class.getClassLoader()
                .getResourceAsStream("assets/testmod/vn/"+ resourceName)) {
            if (stream == null) {
                throw new RuntimeException("Resource not found!");
            }

            InputStreamReader reader = new InputStreamReader(stream);
            Type wrapperType = new TypeToken<VisualNovelDialogueData>() {}.getType();
            VisualNovelDialogueData dialogueData = GSON.fromJson(reader, wrapperType);

            return dialogueData;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mergeTextLines(VisualNovelDialogueData dialogueData) {
        /*
        for (VisualNovelDialogueData.Node node : dialogueData.Nodes) {
            int charactersAdded = 0;
            node.totalCharacters = 0;
            for (String string : node.text) {
                // TODO: might have to add a +2 in case it needs to handle the delimiter like below
                node.indexOfCharToStop.add(charactersAdded + string.length());
                charactersAdded += string.length();
                // node.totalCharacters += charactersAdded;
            }
            node.finalText = String.join("\n", node.text);
            node.totalCharacters = node.finalText.length() - (node.text.size() - 1);
        }

         */
        for (VisualNovelDialogueData.Node node : dialogueData.Nodes) {
            node.finalText = String.join("\n", node.text);
            node.indexOfCharToStop.clear();

            // Recalculate stop positions based on the merged finalText
            int runningIndex = 0;

            for (int i = 0; i < node.text.size(); i += 1) {
                String line = node.text.get(i);

                // Stop at the end of this line
                runningIndex += line.length();
                node.indexOfCharToStop.add(runningIndex);

                // Add 1 for the newline IF not the last line
                if (i < node.text.size() - 1) {
                    runningIndex += 1;  // count the '\n'
                }
            }

            node.totalCharacters = node.finalText.length();
        }
    }
}
