package me.makkuusen.timing.system;


import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import org.bukkit.TreeSpecies;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TPlayer implements Comparable<TPlayer> {
    private final TimingSystem plugin;

    private Player player;
    private final UUID uuid;
    private String name;
    private TreeSpecies boat;
    private boolean toggleSound;


    @Override
    public int compareTo(TPlayer other) {
        return name.compareTo(other.name);
    }

    public TPlayer(TimingSystem plugin, DbRow data) {
        this.plugin = plugin;
        uuid = UUID.fromString(data.getString("uuid"));
        name = data.getString("name");
        boat = data.getString("boat") == null ? TreeSpecies.GENERIC : TreeSpecies.valueOf(data.getString("boat"));
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

    public TreeSpecies getBoat() {
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

    public void setBoat(TreeSpecies boat) {
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

    public static ContextResolver<TreeSpecies, BukkitCommandExecutionContext> getBoatContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            if (TreeSpecies.valueOf(name) != null) {
                return TreeSpecies.valueOf(name);
            } else {
                // User didn't type an Event, show error!
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }
}
