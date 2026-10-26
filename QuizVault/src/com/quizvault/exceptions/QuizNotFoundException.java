package com.quizvault.exceptions;

// A custom exception thrown when a quiz cannot be found in the archive. 
public class QuizNotFoundException extends Exception {

    // Constructor that passes the error message to the parent Exception class.
    public QuizNotFoundException(String message) {
        super(message);
    }
}