package me.makkuusen.timing.system;


import co.aikar.idb.DbRow;
import lombok.Getter;
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
    private Boat.Type boat;
    @Getter
    private boolean chestBoat;
    private boolean toggleSound;
    @Getter
    private boolean verbose;
    @Getter
    private boolean timeTrial;
    @Getter
    private boolean override;
    @Getter
    private boolean compactScoreboard;
    @Getter
    private boolean sendFinalLaps;
    private String color;
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
        boat = stringToType(data.getString("boat"));
        chestBoat = data.get("chestBoat") instanceof Boolean ? data.get("chestBoat") : data.get("chestBoat").equals(1);
        toggleSound = data.get("toggleSound") instanceof Boolean ? data.get("toggleSound") : data.get("toggleSound").equals(1);
        verbose = data.get("verbose") instanceof Boolean ? data.get("verbose") : data.get("verbose").equals(1);
        timeTrial = data.get("timetrial") instanceof  Boolean ? data.get("timetrial") : data.get("timetrial").equals(1);
        color = data.getString("color");
        compactScoreboard = data.get("compactScoreboard") instanceof Boolean ? data.get("compactScoreboard") : data.get("compactScoreboard").equals(1);
        sendFinalLaps = data.get("sendFinalLaps") instanceof Boolean ? data.get("sendFinalLaps") : data.get("sendFinalLaps").equals(1);

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

    public boolean getCompactScoreboard() {
        return compactScoreboard;
    }

    public void setName(String name) {
        plugin.getLogger().info("Updating name of " + uuid + " from " + this.name + " to " + name + ".");

        this.name = name;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "name", name);
    }

    public String getNameDisplay() {
        return getName();
    }

    public String getHexColor() {
        return color;
    }

    public void setHexColor(String hexColor) {
        color = hexColor;
        EventDatabase.getDriverFromRunningHeat(uuid).ifPresent(driver -> driver.getHeat().updateScoreboard());
        TimingSystem.getDatabase().playerUpdateValue(uuid, "color", hexColor);
    }

    public org.bukkit.Color getBukkitColor() {
        var c = Color.decode(color);
        return org.bukkit.Color.fromRGB(c.getRed(), c.getGreen(), c.getBlue());
    }

    public TextColor getTextColor() {
        return TextColor.fromHexString(color);
    }

    public void setBoat(Boat.Type boat) {
        this.boat = boat;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "boat", boat.name());
    }

    public void setChestBoat(boolean b) {
        if (chestBoat == b)
            return;
        chestBoat = b;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "chestBoat", chestBoat);
    }

    public void toggleOverride() {
        override = !override;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "override", override);
    }

    public void toggleVerbose() {
        verbose = !verbose;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "verbose", verbose);
    }

    public void toggleTimeTrial() {
        timeTrial = !timeTrial;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "timetrial", timeTrial);
    }

    public void toggleSound() {
        toggleSound = !toggleSound;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "toggleSound", toggleSound);
    }

    public void toggleCompactScoreboard() {
        this.compactScoreboard = !compactScoreboard;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "compactScoreboard", compactScoreboard);
    }

    public void toggleSendFinalLaps() {
        sendFinalLaps = !sendFinalLaps;
        TimingSystem.getDatabase().playerUpdateValue(uuid, "sendFinalLaps", sendFinalLaps);
    }

    public boolean isCompactScoreboard() {
        return compactScoreboard;
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

    public boolean isSound() {
        return toggleSound;
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

    public Material getBoatMaterial() {
        String boat = getBoat().name();
        if (chestBoat) {
            boat += "_CHEST";
        }
        if (boat.contains("BAMBOO")) {
            boat += "_RAFT";
        } else {
            boat += "_BOAT";
        }
        return Material.valueOf(boat);
    }

    private Boat.Type stringToType(String boatType) {
        if (boatType == null) {
            return Boat.Type.BIRCH;
        }
        try {
            return Boat.Type.valueOf(boatType);
        } catch (IllegalArgumentException e) {
            //REDWOOD is the only old option possible.
            return Boat.Type.SPRUCE;
        }
    }
}


