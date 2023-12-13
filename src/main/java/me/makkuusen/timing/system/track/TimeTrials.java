package me.makkuusen.timing.system.track;

import co.aikar.idb.DB;
import lombok.Getter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.timetrial.TimeTrialAttempt;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.timetrial.TimeTrialFinishComparator;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class TimeTrials {

    private final Map<TPlayer, List<TimeTrialAttempt>> timeTrialAttempts = new HashMap<>();
    private Map<TPlayer, List<TimeTrialFinish>> timeTrialFinishes = new HashMap<>();
    private List<TPlayer> cachedPositions = new ArrayList<>();
    @Getter
    private long totalTimeSpent = 0;
    private final int trackId;
    public TimeTrials(int trackId) {
        this.trackId = trackId;
    }

    public void addTimeTrialFinish(TimeTrialFinish timeTrialFinish) {
        if (timeTrialFinishes.get(timeTrialFinish.getPlayer()) == null) {
            List<TimeTrialFinish> list = new ArrayList<>();
            list.add(timeTrialFinish);
            timeTrialFinishes.put(timeTrialFinish.getPlayer(), list);
            return;
        }
        if (timeTrialFinishes.get(timeTrialFinish.getPlayer()).contains(timeTrialFinish)) {
            return;
        }
        timeTrialFinishes.get(timeTrialFinish.getPlayer()).add(timeTrialFinish);
    }

    public TimeTrialFinish newTimeTrialFinish(long time, UUID uuid) {
        try {
            long date = ApiUtilities.getTimestamp();
            var finishId = DB.executeInsert("INSERT INTO `ts_finishes` (`trackId`, `uuid`, `date`, `time`, `isRemoved`) VALUES(" + trackId + ", '" + uuid + "', " + date + ", " + time + ", 0);");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_finishes` WHERE `id` = " + finishId + ";");
            TimeTrialFinish timeTrialFinish = new TimeTrialFinish(dbRow);

            addTimeTrialFinish(timeTrialFinish);
            return timeTrialFinish;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public void addTimeTrialAttempt(TimeTrialAttempt timeTrialAttempt) {
        if (timeTrialAttempts.get(timeTrialAttempt.getPlayer()) == null) {
            List<TimeTrialAttempt> list = new ArrayList<>();
            list.add(timeTrialAttempt);
            timeTrialAttempts.put(timeTrialAttempt.getPlayer(), list);
            return;
        }
        timeTrialAttempts.get(timeTrialAttempt.getPlayer()).add(timeTrialAttempt);
    }

    public TimeTrialAttempt newTimeTrialAttempt(long time, UUID uuid) {
        long date = ApiUtilities.getTimestamp();
        TimingSystem.getTrackDatabase().createAttempt(trackId, uuid, date, time);
        TimeTrialAttempt timeTrialAttempt = new TimeTrialAttempt(trackId, uuid, ApiUtilities.getTimestamp(), time);
        addTimeTrialAttempt(timeTrialAttempt);
        return timeTrialAttempt;
    }

    public TimeTrialFinish getBestFinish(TPlayer player) {
        if (timeTrialFinishes.get(player) == null) {
            return null;
        }
        List<TimeTrialFinish> ttTimes = new ArrayList<>();
        var times = timeTrialFinishes.get(player);
        ttTimes.addAll(times);
        if (ttTimes.isEmpty()) {
            return null;
        }

        ttTimes.sort(new TimeTrialFinishComparator());
        return ttTimes.get(0);
    }

    public boolean hasPlayedTrack(TPlayer tPlayer) {
        return timeTrialFinishes.containsKey(tPlayer) || timeTrialAttempts.containsKey(tPlayer);
    }

    public void deleteBestFinish(TPlayer player, TimeTrialFinish bestFinish) {
        timeTrialFinishes.get(player).remove(bestFinish);
        TimingSystem.getTrackDatabase().removeFinish(bestFinish.getId());
    }

    public void deleteAllFinishes(TPlayer player) {
        timeTrialFinishes.remove(player);
        TimingSystem.getTrackDatabase().removeAllFinishes(trackId, player.getUniqueId());
    }

    public void deleteAllFinishes() {
        timeTrialFinishes = new HashMap<>();
        TimingSystem.getTrackDatabase().removeAllFinishes(trackId);
    }

    public Integer getPlayerTopListPosition(TPlayer TPlayer) {
        var topList = getTopList(-1);
        for (int i = 0; i < topList.size(); i++) {
            if (topList.get(i).getPlayer().equals(TPlayer)) {
                return ++i;
            }
        }
        return -1;
    }

    public Integer getCachedPlayerPosition(TPlayer tPlayer) {
        int pos = cachedPositions.indexOf(tPlayer);
        if (pos != -1) {
            pos++;
        }
        return pos;
    }

    public List<TimeTrialFinish> getTopList(int limit) {

        List<TimeTrialFinish> bestTimes = new ArrayList<>();
        for (TPlayer player : timeTrialFinishes.keySet()) {
            TimeTrialFinish bestFinish = getBestFinish(player);
            if (bestFinish != null) {
                bestTimes.add(bestFinish);
            }
        }
        bestTimes.sort(new TimeTrialFinishComparator());
        cachedPositions = new ArrayList<>();
        bestTimes.forEach(timeTrialFinish -> cachedPositions.add(timeTrialFinish.getPlayer()));

        if (limit == -1) {
            return bestTimes;
        }

        return bestTimes.stream().limit(limit).collect(Collectors.toList());
    }

    public List<TimeTrialFinish> getTopList() {
        return getTopList(-1);
    }

    public int getPlayerTotalFinishes(TPlayer tPlayer) {
        if (!timeTrialFinishes.containsKey(tPlayer)) {
            return 0;
        }
        return timeTrialFinishes.get(tPlayer).size();
    }

    public int getPlayerTotalAttempts(TPlayer tPlayer) {
        if (!timeTrialAttempts.containsKey(tPlayer)) {
            return 0;
        }
        return timeTrialAttempts.get(tPlayer).size();
    }

    public int getTotalFinishes() {
        int laps = 0;
        for (List<TimeTrialFinish> l : timeTrialFinishes.values()) {
            laps += l.size();
        }
        return laps;

    }

    public int getTotalAttempts() {
        int laps = 0;
        for (List<TimeTrialAttempt> l : timeTrialAttempts.values()) {
            laps += l.size();
        }
        return laps;
    }

    public long getPlayerTotalTimeSpent(TPlayer tPlayer) {
        long time = 0L;

        if (timeTrialAttempts.containsKey(tPlayer)) {
            for (TimeTrialAttempt l : timeTrialAttempts.get(tPlayer)) {
                time += l.getTime();
            }
        }
        if (timeTrialFinishes.containsKey(tPlayer)) {
            for (TimeTrialFinish l : timeTrialFinishes.get(tPlayer)) {
                time += l.getTime();
            }
        }
        return time;
    }

    public void setTotalTimeSpent(long time) {
        totalTimeSpent = time;
    }

}
