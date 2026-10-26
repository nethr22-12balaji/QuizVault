package com.quizvault.exceptions;

/**
 * Custom exception thrown when a duplicate question is detected in the question bank.
 *
 * JAVA TOPICS: User-defined exception, Constructor overloading, Inheritance (extends Exception)
 */
public class DuplicateQuestionException extends Exception {

    private String questionText;

    // Constructor overloading — single-arg
    public DuplicateQuestionException(String message) {
        super(message);
    }

    // Constructor overloading — two-arg with question context
    public DuplicateQuestionException(String message, String questionText) {
        super(message);
        this.questionText = questionText;
    }

    // Constructor overloading — chained cause
    public DuplicateQuestionException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getQuestionText() {
        return questionText;
    }
}
