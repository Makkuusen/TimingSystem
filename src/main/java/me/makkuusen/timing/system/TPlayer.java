package me.makkuusen.timing.system;


import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.event.EventDatabase;
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
    private Player player;
    private String name;
    private Boat.Type boat;
    private boolean chestBoat;
    private boolean toggleSound;
    private boolean verbose;
    private boolean timeTrial;
    private boolean override;
    private boolean compactScoreboard;
    private boolean sendFinalLaps;
    private String color;
    private BaseGui openGui;
    private Theme theme;
    private TrackFilter filter;
    private TrackSort trackSort;
    private Track.TrackType trackType;
    private Integer page;
    private Integer boatUtilsVersion = null;


    public TPlayer(TimingSystem plugin, DbRow data) {
        this.plugin = plugin;
        uuid = UUID.fromString(data.getString("uuid"));
        name = data.getString("name");
        boat = stringToType(data.getString("boat"));
        chestBoat = data.get("chestBoat");
        toggleSound = data.get("toggleSound");
        verbose = data.get("verbose");
        timeTrial = data.get("timetrial");
        color = data.getString("color");
        compactScoreboard = data.get("compactScoreboard");
        sendFinalLaps = data.get("sendFinalLaps");

        theme = TimingSystem.defaultTheme;
    }

    public static ContextResolver<Boat.Type, BukkitCommandExecutionContext> getBoatContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return Boat.Type.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                //no matching boat types
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
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

        for(Component line : lines) {
            scoreboard.line(lines.indexOf(line), line);
        }
    }

    public BaseGui getOpenGui() {
        return openGui;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        plugin.getLogger().info("Updating name of " + uuid + " from " + this.name + " to " + name + ".");

        this.name = name;
        DB.executeUpdateAsync("UPDATE `ts_players` SET `name` = " + Database.sqlString(name) + " WHERE `uuid` = '" + uuid + "';");
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
        DB.executeUpdateAsync("UPDATE `ts_players` SET `color` = '" + hexColor + "' WHERE `uuid` = '" + uuid + "';");
    }

    public String getColorCode() {
        return String.valueOf(net.md_5.bungee.api.ChatColor.of(color));
    }

    public org.bukkit.Color getBukkitColor() {
        var c = Color.decode(color);
        return org.bukkit.Color.fromRGB(c.getRed(), c.getGreen(), c.getBlue());
    }

    public TextColor getTextColor() {
        return TextColor.fromHexString(color);
    }

    public Boat.Type getBoat() {
        return boat;
    }

    public void setBoat(Boat.Type boat) {
        this.boat = boat;
        DB.executeUpdateAsync("UPDATE `ts_players` SET `boat` = " + Database.sqlString(boat.name()) + " WHERE `uuid` = '" + uuid + "';");
    }

    public boolean isChestBoat() {
        return chestBoat;
    }

    public void setChestBoat(boolean b) {
        if (chestBoat != b) {
            chestBoat = b;
            DB.executeUpdateAsync("UPDATE `ts_players` SET `chestBoat` = " + chestBoat + " WHERE `uuid` = '" + uuid + "';");
        }
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isOverride() {
        return override;
    }

    public boolean isTimeTrial() {
        return timeTrial;
    }

    public boolean isCompactScoreboard() {
        return compactScoreboard;
    }

    public boolean isSendFinalLaps() {
        return sendFinalLaps;
    }

    public void toggleOverride() {
        override = !override;
        DB.executeUpdateAsync("UPDATE `ts_players` SET `override` = " + override + " WHERE `uuid` = '" + uuid + "';");
    }

    public void toggleVerbose() {
        verbose = !verbose;
        DB.executeUpdateAsync("UPDATE `ts_players` SET `verbose` = " + verbose + " WHERE `uuid` = '" + uuid + "';");
    }

    public void toggleTimeTrial() {
        timeTrial = !timeTrial;
        DB.executeUpdateAsync("UPDATE `ts_players` SET `timetrial` = " + timeTrial + " WHERE `uuid` = '" + uuid + "';");
    }

    public void toggleSound() {
        toggleSound = !toggleSound;
        DB.executeUpdateAsync("UPDATE `ts_players` SET `toggleSound` = " + toggleSound + " WHERE `uuid` = '" + uuid + "';");
    }

    public void toggleCompactScoreboard() {
        this.compactScoreboard = !compactScoreboard;
        DB.executeUpdateAsync("UPDATE `ts_players` SET `compactScoreboard` = " + compactScoreboard + " WHERE `uuid` = '" + uuid + "';");
    }

    public void toggleSendFinalLaps() {
        sendFinalLaps = !sendFinalLaps;
        DB.executeUpdateAsync("UPDATE `ts_players` SET `sendFinalLaps` = " + sendFinalLaps + " WHERE `uuid` = '" + uuid + "';");
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

    public Theme getTheme() {
        return theme;
    }

    public TrackSort getTrackSort() {
        return trackSort;
    }

    public void setTrackSort(TrackSort trackSort) {
        this.trackSort = trackSort;
    }

    public TrackFilter getFilter() {
        return filter;
    }

    public void setFilter(TrackFilter filter) {
        this.filter = filter;
    }

    public Track.TrackType getTrackType() {
        return trackType;
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

    public Player getPlayer() {
        return player;
    }

    void setPlayer(Player player) {
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


