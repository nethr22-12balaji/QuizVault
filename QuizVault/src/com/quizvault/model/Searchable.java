package com.quizvault.model;

/**
 * Generic interface for searchable entities.
 *
 * JAVA TOPICS: Generic Interface (Generics)
 */
public interface Searchable<T> {
    
    /**
     * Checks if the current instance matches the given query.
     * @param query The search query string
     * @return true if matches, false otherwise
     */
    boolean matches(String query);
}
