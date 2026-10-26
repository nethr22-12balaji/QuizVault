package com.quizvault.exceptions;

//A custom exception thrown when an invalid score is entered, such as a score outside an expected range.
public class InvalidScoreException extends Exception {
    
    // Constructor that passes the error message to the parent Exception class.
    public InvalidScoreException(String message) {
        super(message);
    }
}