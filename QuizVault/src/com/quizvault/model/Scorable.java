package com.quizvault.model;

import com.quizvault.exceptions.InvalidScoreException;

/**
 * Interface for objects that can be scored.
 *
 * JAVA TOPICS: Interface
 */
public interface Scorable {
    void addScore(int score) throws InvalidScoreException;
    void deductScore(int score) throws InvalidScoreException;
    int getTotalScore();
}
