package com.quizvault.model;
import com.quizvault.round.Round;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Quiz implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum Status { SETUP, IN_PROGRESS, COMPLETED }

    private String quizName;
    private String quizDate;
    private Status status = Status.SETUP;
    private List<Team> teams = new ArrayList<>();
    private List<Round> rounds = new ArrayList<>();
    
    // Per-team queues and progress
    private Map<String, List<Question>> teamQuestions = new HashMap<>();
    private Map<String, Integer> teamProgress = new HashMap<>(); // index into teamQuestions

    public Quiz(String quizName, String quizDate) {
        this.quizName = quizName;
        this.quizDate = quizDate;
    }

    public String getQuizName() { return quizName; }
    public String getQuizDate() { return quizDate; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public List<Team> getTeams() { return teams; }
    public void setTeams(List<Team> teams) { this.teams = new ArrayList<>(teams); }

    public List<Round> getRounds() { return rounds; }
    public void addRound(Round r) { rounds.add(r); }

    public Map<String, List<Question>> getTeamQuestions() { return teamQuestions; }
    public Map<String, Integer> getTeamProgress() { return teamProgress; }
}
