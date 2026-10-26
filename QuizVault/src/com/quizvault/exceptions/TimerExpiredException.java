package com.quizvault.exceptions;

/**
 * Custom exception thrown when a timer expires during a round.
 *
 * JAVA TOPICS: User-defined exception, Inheritance
 */
public class TimerExpiredException extends Exception {
    
    public TimerExpiredException(String message) {
        super(message);
    }
}
