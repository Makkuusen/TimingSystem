package me.makkuusen.timing.system;

import java.util.List;

public class TimingSystemConfiguration
{
    private final int leaderboardsUpdateTick;
    private final List<String> leaderboardsFastestTimeLines;
    private boolean lasersItems;

    TimingSystemConfiguration(TimingSystem plugin)
    {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.leaderboardsUpdateTick = plugin.getConfig().getInt("leaderboards.updateticks");
        this.leaderboardsFastestTimeLines = plugin.getConfig().getStringList("leaderboards.fastesttime.lines");
        this.lasersItems = plugin.getConfig().getBoolean("race.lasersItems");
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
}
