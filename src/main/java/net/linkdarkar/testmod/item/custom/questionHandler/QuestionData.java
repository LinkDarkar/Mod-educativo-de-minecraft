package net.linkdarkar.testmod.item.custom.questionHandler;

import java.util.ArrayList;
import java.util.List;

public class QuestionData {
    public String id;
    public String question;
    public int type = 1;
    public int verticalLineSpacing = 12;
    public int startXPos = 10;
    public int startYPos = 10;
    public int textBoxXPos = 30;
    public int textBoxYPos = 150;
    public int textBoxWidth = 300;
    public int textBoxHeight = 20;
    public String answer = "";
    public String explanation = "there is no...\nexplanation";
    public List<AnswerData> answers = new ArrayList<>();
}
