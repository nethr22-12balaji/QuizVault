package com.quizvault.exceptions;

// A custom exception thrown when a team cannot be found during an operation. 
public class TeamNotFoundException extends Exception {

    // Constructor that passes the error message to the parent Exception class.
    public TeamNotFoundException(String message) {
        super(message); 
    }
}