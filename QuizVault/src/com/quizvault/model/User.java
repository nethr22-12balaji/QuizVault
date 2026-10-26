package com.quizvault.model;

import java.io.Serializable;

/**
 * Abstract base class for all users.
 *
 * JAVA TOPICS: Abstract Classes
 */
public abstract class User implements Serializable {
    protected String username;
    protected String role;

    public User(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }
    
    public String getRole() {
        return role;
    }

    /**
     * Abstract method for viewing scores, to be implemented by subclasses.
     */
    public abstract void viewScores();
}
