package me.makkuusen.timing.system.tplayer;


import co.aikar.idb.DbRow;
import lombok.Getter;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.EventDatabase;
import me.makkuusen.timing.system.gui.BaseGui;
import me.makkuusen.timing.system.gui.TrackFilter;
import me.makkuusen.timing.system.gui.TrackSort;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.List;
import java.util.UUID;

public class TPlayer implements Comparable<TPlayer> {
    private final TimingSystem plugin;
    private final UUID uuid;
    private Sidebar scoreboard;
    @Getter
    private Player player;
    @Getter
    private String name;
    @Getter
    private Settings settings;
    @Getter
    private BaseGui openGui;
    @Getter
    private Theme theme;
    @Getter
    private TrackFilter filter;
    @Getter
    private TrackSort trackSort;
    @Getter
    private Track.TrackType trackType;
    private Integer page;
    private Integer boatUtilsVersion = null;


    public TPlayer(TimingSystem plugin, DbRow data) {
        this.plugin = plugin;
        uuid = UUID.fromString(data.getString("uuid"));
        name = data.getString("name");
        settings = new Settings(uuid, data);
        theme = TimingSystem.defaultTheme;
    }

    @Override
    public int compareTo(TPlayer other) {
        return name.compareTo(other.name);
    }

    public void initScoreboard() {
        if (player == null) {
            return;
        }
        if (scoreboard == null) {
            scoreboard = TimingSystem.scoreboardLibrary.createSidebar(TimingSystem.configuration.getScoreboardMaxRows());
            scoreboard.addPlayer(player);
        }
    }

    public void clearScoreboard() {
        if (scoreboard != null) {
            scoreboard.close();
            scoreboard = null;
        }
    }

    public void setScoreBoardTitle(Component title) {
        if (player == null) {
            return;
        }
        if (scoreboard == null) {
            initScoreboard();
        }

        scoreboard.title(title);
    }

    public void setScoreBoardLines(List<Component> lines) {
        if (player == null) {
            return;
        }

        if (scoreboard == null) {
            initScoreboard();
        }

        for (int i = 0; i < Math.min(lines.size(), scoreboard.maxLines()); i++) {
            scoreboard.line(i, lines.get(i));
        }
    }

    public void setOpenGui(BaseGui openGui) {
        this.openGui = openGui;
    }

    public UUID getUniqueId() {
        return this.uuid;
    }

    public void setName(String name) {
        plugin.getLogger().info("Updating name of " + uuid + " from " + this.name + " to " + name + ".");

        this.name = name;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "name", name);
    }

    public String getNameDisplay() {
        return getName();
    }

    public boolean hasBoatUtils() {
        if (boatUtilsVersion == null) {
            return false;
        }
        return boatUtilsVersion >= 0;
    }

    public int getBoatUtilsVersion() {
        return boatUtilsVersion;
    }

    public void setBoatUtilsVersion(Integer boatUtilsVersion) {
        this.boatUtilsVersion = boatUtilsVersion;
    }


    public void setTrackSort(TrackSort trackSort) {
        this.trackSort = trackSort;
    }

    public void setFilter(TrackFilter filter) {
        this.filter = filter;
    }

    public void setTrackType(Track.TrackType trackType) {
        this.trackType = trackType;
    }

    public Integer getTrackPage() {
        return page;
    }

    public void setTrackPage(Integer page) {
        this.page = page;
    }

    // Not sure if this was protected for a reason.
    // Made it public so that the database can be reworked and stored in a different package.
    public void setPlayer(Player player) {
        this.player = player;
    }
}


