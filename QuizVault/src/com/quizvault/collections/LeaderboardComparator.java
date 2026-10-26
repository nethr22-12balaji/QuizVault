package com.quizvault.collections;
import com.quizvault.model.Team;
import java.util.Comparator;

public class LeaderboardComparator implements Comparator<Team> {
    @Override
    public int compare(Team t1, Team t2) {
        int scoreDiff = Integer.compare(t2.getTotalScore(), t1.getTotalScore());
        if (scoreDiff != 0) return scoreDiff;
        return t1.getTeamName().compareToIgnoreCase(t2.getTeamName());
    }
}
