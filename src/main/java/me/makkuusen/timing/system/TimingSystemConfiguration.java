package me.makkuusen.timing.system;

import lombok.Getter;

import java.util.List;

@Getter
public class TimingSystemConfiguration {
    private final int leaderboardsUpdateTick;
    private final List<String> leaderboardsFastestTimeLines;
    private final boolean lasersItems;
    private final int timesPageSize;
    private final int laps;
    private final int pits;
    private final int timeLimit;
    private final int startDelay;
    private final String sqlHost;
    private final int sqlPort;
    private final String sqlDatabase;
    private final String sqlUsername;
    private final String sqlPassword;

    TimingSystemConfiguration(TimingSystem plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        leaderboardsUpdateTick = plugin.getConfig().getInt("leaderboards.updateticks");
        leaderboardsFastestTimeLines = plugin.getConfig().getStringList("leaderboards.fastesttime.lines");
        lasersItems = plugin.getConfig().getBoolean("race.lasersItems");
        timesPageSize = plugin.getConfig().getInt("tracks.timesPageSize");

        laps = plugin.getConfig().getInt("finals.laps");
        pits = plugin.getConfig().getInt("finals.pits");
        timeLimit = plugin.getConfig().getInt("qualifying.timeLimitInSeconds");
        startDelay = plugin.getConfig().getInt("qualifying.startDelayInSeconds");

        sqlHost = plugin.getConfig().getString("sql.host");
        sqlPort = plugin.getConfig().getInt("sql.port");
        sqlDatabase = plugin.getConfig().getString("sql.database");
        sqlUsername = plugin.getConfig().getString("sql.username");
        sqlPassword = plugin.getConfig().getString("sql.password");
    }

    public int leaderboardsUpdateTick() {
        return leaderboardsUpdateTick;
    }

    public List<String> leaderboardsFastestTimeLines() {
        return leaderboardsFastestTimeLines;
    }

    public boolean isLasersItems() {
        return lasersItems;
    }

    public int getTimesPageSize() {
        return timesPageSize;
    }
}
