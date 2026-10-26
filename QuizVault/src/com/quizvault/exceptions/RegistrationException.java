package com.quizvault.exceptions;

/**
 * Custom exception thrown when a participant fails to register for an event.
 *
 * JAVA TOPICS: User-defined exception, Inheritance
 */
public class RegistrationException extends Exception {
    
    public RegistrationException(String message) {
        super(message);
    }
}
