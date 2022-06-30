package me.makkuusen.timing.system;

import java.util.List;

public class RaceConfiguration
{
    private final int leaderboardsUpdateTick;
    private final List<String> leaderboardsFastestTimeLines;

    RaceConfiguration(Race plugin)
    {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.leaderboardsUpdateTick = plugin.getConfig().getInt("leaderboards.updateticks");
        this.leaderboardsFastestTimeLines = plugin.getConfig().getStringList("leaderboards.fastesttime.lines");
    }

    public int leaderboardsUpdateTick()
    {
        return leaderboardsUpdateTick;
    }
    public List<String> leaderboardsFastestTimeLines()
    {
        return leaderboardsFastestTimeLines;
    }
}
