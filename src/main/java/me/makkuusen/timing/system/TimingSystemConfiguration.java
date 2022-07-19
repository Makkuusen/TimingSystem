package me.makkuusen.timing.system;

import lombok.Getter;

import java.util.List;

@Getter
public class TimingSystemConfiguration
{
    private final int leaderboardsUpdateTick;
    private final List<String> leaderboardsFastestTimeLines;
    private boolean lasersItems;
    private int timesPageSize;
    private String sqlHost;
    private int sqlPort;
    private String sqlDatabase;
    private String sqlUsername;
    private String sqlPassword;

    TimingSystemConfiguration(TimingSystem plugin)
    {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        leaderboardsUpdateTick = plugin.getConfig().getInt("leaderboards.updateticks");
        leaderboardsFastestTimeLines = plugin.getConfig().getStringList("leaderboards.fastesttime.lines");
        lasersItems = plugin.getConfig().getBoolean("race.lasersItems");
        timesPageSize = plugin.getConfig().getInt("tracks.timesPageSize");

        sqlHost = plugin.getConfig().getString("sql.host");
        sqlPort = plugin.getConfig().getInt("sql.port");
        sqlDatabase = plugin.getConfig().getString("sql.database");
        sqlUsername = plugin.getConfig().getString("sql.username");
        sqlPassword = plugin.getConfig().getString("sql.password");
    }

    public int leaderboardsUpdateTick()
    {
        return leaderboardsUpdateTick;
    }
    public List<String> leaderboardsFastestTimeLines()
    {
        return leaderboardsFastestTimeLines;
    }
    public boolean isLasersItems() {
        return lasersItems;
    }
    public int getTimesPageSize() {
        return timesPageSize;
    }
}
