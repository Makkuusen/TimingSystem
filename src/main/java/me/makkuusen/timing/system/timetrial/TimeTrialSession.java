package me.makkuusen.timing.system.timetrial;

import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.track.Track;

import java.util.ArrayList;
import java.util.List;

public class TimeTrialSession {

    TPlayer tPlayer;
    Track track;
    List<TimeTrialFinish> timeTrialFinishes = new ArrayList<>();
    List<TimeTrialAttempt> timeTrialAttempts = new ArrayList<>();
    TimeTrialScoreboard timeTrialScoreboard;

    public TimeTrialSession(TPlayer tPlayer, Track track) {
        this.track = track;
        this.tPlayer = tPlayer;
    }

    public void addTimeTrialFinish(TimeTrialFinish timeTrialFinish) {
        timeTrialFinishes.add(timeTrialFinish);
    }

    public void addTimeTrialAttempt(TimeTrialAttempt timeTrialAttempt) {
        timeTrialAttempts.add(timeTrialAttempt);
    }

    public List<TimeTrialFinish> getTimeTrialFinishes() {
        return timeTrialFinishes;
    }

    public List<TimeTrialAttempt> getTimeTrialAttempts() {
        return timeTrialAttempts;
    }

    public Track getTrack() {
        return track;
    }

    public void updateScoreboard() {
        if (tPlayer.getPlayer() == null) {
            if (timeTrialScoreboard != null) {
                timeTrialScoreboard.removeScoreboard();
                timeTrialScoreboard = null;
            }
            return;
        }
        if (timeTrialScoreboard == null) {
            timeTrialScoreboard = new TimeTrialScoreboard(tPlayer, this);
        }
        timeTrialScoreboard.setDriverLines();
    }

    public void clearScoreboard() {
        if (timeTrialScoreboard != null) {
            timeTrialScoreboard.removeScoreboard();
            timeTrialScoreboard = null;
        }
    }
}
