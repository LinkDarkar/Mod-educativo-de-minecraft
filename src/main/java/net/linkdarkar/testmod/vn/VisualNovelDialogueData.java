package net.linkdarkar.testmod.vn;

import java.util.ArrayList;
import java.util.List;

public class VisualNovelDialogueData {
    public String id;
    public List<Node> Nodes;

    public static class Node {
        public String id;
        public String speaker;
        public String portrait;
        public int[] portraitCoords;
        public List<String> text;
        public String finalText;
        public int totalCharacters;
        public List<Integer> indexOfCharToStop = new ArrayList<>();
        public String nextNodeId;
    }
}
