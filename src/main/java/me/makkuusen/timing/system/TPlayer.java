package me.makkuusen.timing.system;


import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import dev.jcsoftware.jscoreboards.JPerPlayerMethodBasedScoreboard;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.gui.BaseGui;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.List;
import java.util.UUID;

public class TPlayer implements Comparable<TPlayer> {
    private final TimingSystem plugin;
    private final UUID uuid;
    JPerPlayerMethodBasedScoreboard jScoreboard;
    private Player player;
    private String name;
    private Boat.Type boat;
    private boolean chestBoat;
    private boolean toggleSound;
    private boolean verbose;
    private boolean timeTrial;
    private boolean override;
    private boolean compactScoreboard;
    private String color;
    private BaseGui openGui;


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
    }

    public static ContextResolver<Boat.Type, BukkitCommandExecutionContext> getBoatContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return Boat.Type.valueOf(name);
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
        if (jScoreboard == null) {
            jScoreboard = new JPerPlayerMethodBasedScoreboard();
            jScoreboard.addPlayer(player);
        }
    }

    public void clearScoreboard() {
        if (jScoreboard != null) {
            jScoreboard.destroy();
            jScoreboard = null;
        }
    }

    public void setScoreBoardTitle(String title) {
        if (player == null) {
            return;
        }
        if (jScoreboard == null) {
            initScoreboard();
        }

        jScoreboard.setTitle(player, title);
    }

    public void setScoreBoardLines(List<String> lines) {
        if (player == null) {
            return;
        }

        if (jScoreboard == null) {
            initScoreboard();
        }

        jScoreboard.setLines(player, lines);
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
        return getName() + "§r";
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

    public boolean isSound() {
        return toggleSound;
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
        if (boat.equalsIgnoreCase("BAMBOO")) {
            boat += "_RAFT";
        } else {
            boat += "_BOAT";
        }
        return Material.valueOf(boat);
    }

    private Boat.Type stringToType(String boatType) {
        if (boatType == null) {
            return Boat.Type.OAK;
        }
        try {
            return Boat.Type.valueOf(boatType);
        } catch (IllegalArgumentException e) {
            return boatMigration(boatType);
        }
    }

    private Boat.Type boatMigration(String oldtype) {
        TreeSpecies oldTree = TreeSpecies.valueOf(oldtype);
        switch (oldTree) {
            case ACACIA -> {
                setBoat(Boat.Type.ACACIA);
                return Boat.Type.ACACIA;
            }
            case BIRCH -> {
                setBoat(Boat.Type.BIRCH);
                return Boat.Type.BIRCH;
            }
            case DARK_OAK -> {
                setBoat(Boat.Type.DARK_OAK);
                return (Boat.Type.DARK_OAK);
            }
            case REDWOOD -> {
                setBoat(Boat.Type.SPRUCE);
                return (Boat.Type.SPRUCE);
            }
            case JUNGLE -> {
                setBoat(Boat.Type.JUNGLE);
                return Boat.Type.JUNGLE;
            }
            default -> {
                setBoat(Boat.Type.OAK);
                return Boat.Type.OAK;
            }
        }

    }
}
