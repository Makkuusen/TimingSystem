package me.makkuusen.timing.system.race;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

public class RaceBar {

    public RaceBar() {

    }

    public BossBar getRaceBar(RaceDriver raceDriver) {
        var bb = Bukkit.createBossBar(
                "Title",
                BarColor.WHITE,
                BarStyle.SEGMENTED_20
        );

        return bb;
    }
}
