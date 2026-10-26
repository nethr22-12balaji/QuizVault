package com.quizvault.model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestionBank {
    private static QuestionBank instance;
    private final List<Question> questions = Collections.synchronizedList(new ArrayList<>());

    private QuestionBank() {}

    public static synchronized QuestionBank getInstance() {
        if (instance == null) instance = new QuestionBank();
        return instance;
    }

    public void setQuestions(List<Question> list) {
        questions.clear();
        if (list != null) questions.addAll(list);
    }

    public List<Question> getAllQuestions() {
        synchronized (questions) {
            return new ArrayList<>(questions);
        }
    }

    public boolean addQuestion(Question q) {
        synchronized (questions) {
            for (Question existing : questions) {
                if (existing.equals(q)) return false;
            }
            questions.add(q);
            return true;
        }
    }
    
    public boolean removeQuestion(String text) {
        synchronized (questions) {
            return questions.removeIf(q -> q.getQuestionText().equalsIgnoreCase(text.trim()));
        }
    }
}
