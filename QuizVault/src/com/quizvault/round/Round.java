package com.quizvault.round;
import java.io.Serializable;

public class Round implements Serializable {
    private static final long serialVersionUID = 1L;
    private String roundName;
    private int questionCount;

    public Round(String roundName, int count) {
        this.roundName = roundName;
        this.questionCount = count;
    }

    public String getRoundName() { return roundName; }
    public int getQuestionCount() { return questionCount; }
}