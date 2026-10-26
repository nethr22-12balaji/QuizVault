package com.quizvault.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a Quiz with event registration details.
 *
 * JAVA TOPICS: Classes & Objects
 */
public class QuizEvent implements Serializable {
    private String eventId;
    private Quiz quiz;
    private List<String> registeredParticipantUsernames;

    public QuizEvent(String eventId, Quiz quiz) {
        this.eventId = eventId;
        this.quiz = quiz;
        this.registeredParticipantUsernames = new ArrayList<>();
    }

    public String getEventId() {
        return eventId;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void registerParticipant(String username) {
        if (!registeredParticipantUsernames.contains(username)) {
            registeredParticipantUsernames.add(username);
        }
    }

    public List<String> getRegisteredParticipants() {
        return registeredParticipantUsernames;
    }
}
