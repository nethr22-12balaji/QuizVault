package com.quizvault.model;

/**
 * Interface for any entity that can be displayed on a projector.
 *
 * JAVA TOPICS: Interface, abstract methods
 */
public interface Displayable {
    
    /**
     * Gets the formatted text to be displayed.
     * @return String representation for display
     */
    String getDisplayString();
}
