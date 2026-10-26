package com.quizvault.model;
import java.io.Serializable;
import java.util.Objects;

public class Question implements Serializable {
    private static final long serialVersionUID = 1L;
    private String questionText;
    private String answerText;
    private String category;
    private String difficulty;
    private String tags;

    public Question(String questionText, String answerText, String category, String difficulty, String tags) {
        this.questionText = questionText != null ? questionText.trim() : "";
        this.answerText = answerText != null ? answerText.trim() : "";
        this.category = category != null ? category.trim() : "";
        this.tags = tags != null ? tags.trim() : "";
        
        String d = difficulty != null ? difficulty.trim() : "";
        if (d.equalsIgnoreCase("easy")) this.difficulty = "Easy";
        else if (d.equalsIgnoreCase("hard")) this.difficulty = "Hard";
        else this.difficulty = "Medium";
    }

    public String getQuestionText() { return questionText; }
    public String getAnswerText() { return answerText; }
    public String getCategory() { return category; }
    public String getDifficulty() { return difficulty; }
    public String getTags() { return tags; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        return Objects.equals(questionText, question.questionText) &&
               Objects.equals(answerText, question.answerText) &&
               Objects.equals(difficulty, question.difficulty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questionText, answerText, difficulty);
    }
}
