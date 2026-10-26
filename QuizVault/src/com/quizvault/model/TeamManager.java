package com.quizvault.model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;

public class TeamManager {
    private static TeamManager instance;
    private final List<Team> registeredTeams = Collections.synchronizedList(new ArrayList<>());

    private TeamManager() {}

    public static synchronized TeamManager getInstance() {
        if (instance == null) instance = new TeamManager();
        return instance;
    }

    public void setTeams(List<Team> list) {
        registeredTeams.clear();
        if (list != null) registeredTeams.addAll(list);
    }

    public List<Team> getRegisteredTeams() {
        synchronized (registeredTeams) {
            return new ArrayList<>(registeredTeams);
        }
    }

    public String createTeam(String teamName, String creatorUser) {
        if (teamName == null || teamName.trim().isEmpty()) return "Team name cannot be blank";
        synchronized (registeredTeams) {
            for (Team t : registeredTeams) {
                if (t.getTeamName().equalsIgnoreCase(teamName.trim())) {
                    return "Team name already exists";
                }
            }
            removeUserFromAnyTeam(creatorUser);
            Team t = new Team(teamName, new ArrayList<>());
            t.getPlayers().add(new Player(creatorUser));
            registeredTeams.add(t);
            return null;
        }
    }

    public String joinTeam(String teamName, String user) {
        synchronized (registeredTeams) {
            Team target = null;
            for (Team t : registeredTeams) {
                if (t.getTeamName().equalsIgnoreCase(teamName.trim())) target = t;
            }
            if (target == null) return "Team not found";
            
            removeUserFromAnyTeam(user);
            target.getPlayers().add(new Player(user));
            return null;
        }
    }
    
    private void removeUserFromAnyTeam(String user) {
        for (Team t : registeredTeams) {
            t.getPlayers().removeIf(p -> p.getName().equals(user));
        }
        // Remove empty teams
        registeredTeams.removeIf(t -> t.getPlayers().isEmpty());
    }
}
