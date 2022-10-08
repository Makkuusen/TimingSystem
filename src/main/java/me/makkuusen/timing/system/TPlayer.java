package me.makkuusen.timing.system;


import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import org.bukkit.TreeSpecies;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TPlayer implements Comparable<TPlayer> {
    private final TimingSystem plugin;

    private Player player;
    private final UUID uuid;
    private String name;
    private Boat.Type boat;
    private boolean toggleSound;


    @Override
    public int compareTo(TPlayer other) {
        return name.compareTo(other.name);
    }

    public TPlayer(TimingSystem plugin, DbRow data) {
        this.plugin = plugin;
        uuid = UUID.fromString(data.getString("uuid"));
        name = data.getString("name");
        boat = stringToType(data.getString("boat"));
        toggleSound = data.get("toggleSound");
    }

    public UUID getUniqueId() {
        return this.uuid;
    }

    public String getName() {
        return name;
    }

    public String getNameDisplay() {
        return getName() + "§r";
    }

    public Boat.Type getBoat() {
        return boat;
    }

    public void setName(String name) {
        plugin.getLogger().info("Updating name of " + uuid + " from " + this.name + " to " + name + ".");

        this.name = name;
        DB.executeUpdateAsync("UPDATE `ts_players` SET `name` = " + Database.sqlString(name) + " WHERE `uuid` = '" + uuid + "';");

        if (player != null) {
            player.setDisplayName(getNameDisplay());
        }
    }

    public void setBoat(Boat.Type boat) {
        this.boat = boat;
        DB.executeUpdateAsync("UPDATE `ts_players` SET `boat` = " + Database.sqlString(boat.name()) + " WHERE `uuid` = '" + uuid + "';");

    }

    public void switchToggleSound() {
        toggleSound = !toggleSound;
        DB.executeUpdateAsync("UPDATE `ts_players` SET `toggleSound` = " + toggleSound + " WHERE `uuid` = '" + uuid + "';");
    }

    public Boolean getToggleSound() {
        return toggleSound;
    }

    public Player getPlayer() {
        return player;
    }

    void setPlayer(Player player) {
        // Player came online
		// Player disconnected
		this.player = player;
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
    private Boat.Type stringToType(String boatType) {
        try {return Boat.Type.valueOf(boatType);}
        catch (IllegalArgumentException e) {
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
