package com.quizvault.model;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Team implements Serializable {
    private static final long serialVersionUID = 1L;
    private String teamName;
    private List<Player> players;
    private int totalScore = 0;

    public Team(String teamName, List<Player> players) {
        this.teamName = teamName != null ? teamName.trim() : "";
        this.players = players != null ? new ArrayList<>(players) : new ArrayList<>();
    }

    public String getTeamName() { return teamName; }
    public List<Player> getPlayers() { return players; }
    public int getTotalScore() { return totalScore; }

    public void addScore(int points) { this.totalScore += points; }
    public void deductScore(int points) { this.totalScore -= points; }
}
