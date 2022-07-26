package me.makkuusen.timing.system;


import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import org.bukkit.Bukkit;
import org.bukkit.TreeSpecies;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.UUID;

public class TPlayer implements Comparable<TPlayer> {
    private final TimingSystem plugin;

    private Player player;
    private final UUID uuid;
    private String name;
    private long dateJoin, dateNameChange, dateNameCheck, dateSeen;
    private TreeSpecies boat;


    @Override
    public int compareTo(TPlayer other) {
        return name.compareTo(other.name);
    }

    public TPlayer(TimingSystem plugin, DbRow data) {
        this.plugin = plugin;
        uuid = UUID.fromString(data.getString("uuid"));
        name = data.getString("name");
        if (data.get("dateJoin") != null) dateJoin = data.get("dateJoin");
        if (data.get("dateNameChange") != null) dateNameChange = data.getLong("dateNameChange");
        if (data.get("dateSeen") != null) dateSeen = data.getLong("dateSeen");
        boat = data.getString("boat") == null ? TreeSpecies.GENERIC : TreeSpecies.valueOf(data.getString("boat"));
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

    public Player getPlayer() {
        return player;
    }

    void setPlayer(Player player) {
        // Player came online
		// Player disconnected
		this.player = player;
    }

    public long getDateJoin() {
        return dateJoin;
    }

    public long getDateSeen() {
        return dateSeen;
    }

    void updateNameChanges() {
        // We're not bothering with checking the history if the last name change occurred less than 29 days ago
        if ((ApiUtilities.getTimestamp() - dateNameChange) < 2506000) {
            return;
        }

        final TPlayer TPlayer = this;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    URLConnection connection = new URL("https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "") + "/names").openConnection();

                    InputStream response = connection.getInputStream();
                    JSONParser parser = new JSONParser();
                    Object obj = parser.parse(new BufferedReader(new InputStreamReader(response, StandardCharsets.UTF_8)));
                    JSONArray jsonObject = (JSONArray) obj;

                    String nameCurrent = null;
                    long dateNameChange = 0;

                    int newChanges = 0;

                    for (@SuppressWarnings("unchecked") Iterator<JSONObject> iterator = jsonObject.iterator(); iterator.hasNext(); ) {
                        JSONObject nameChange = iterator.next();

                        nameCurrent = (String) nameChange.get("name");

                        if (nameCurrent == null) {
                            plugin.getLogger().warning("Failed to fetch name changes for " + uuid + " (" + name + "): Couldn't parse response from Mojang.");
                            return;
                        }

                        Long dateNameChangeRaw = (Long) nameChange.get("changedToAt");
                        dateNameChange = dateNameChangeRaw == null ? 0 : (dateNameChangeRaw / 1000);

                        if (dateNameChange > TPlayer.dateNameChange) {
                            newChanges++;
                        }
                    }

                    if (nameCurrent == null) {
                        plugin.getLogger().warning("Failed to fetch name changes for " + uuid + " (" + name + "): No name history found.");
                        return;
                    }

                    TPlayer.dateNameChange = dateNameChange;
                    TPlayer.dateNameCheck = ApiUtilities.getTimestamp();

                    if (newChanges == 0) {
                        DB.executeUpdateAsync("UPDATE `ts_players` SET `dateNameCheck` = " + TPlayer.dateNameCheck + " WHERE `uuid` = '" + uuid + "';");
                    } else {
                        DB.executeUpdateAsync("UPDATE `ts_players` SET `dateNameChange` = " + TPlayer.dateNameChange + ", `dateNameCheck` = " + TPlayer.dateNameCheck + " WHERE `uuid` = '" + uuid + "';");

                        plugin.getLogger().info("Cached " + newChanges + " new name " + (newChanges == 1 ? "change" : "changes") + " for " + uuid + " (" + nameCurrent + ").");

                        // Only update the cache if the player is offline
                        if (!name.equals(nameCurrent) && player == null) {
                            setName(nameCurrent);
                        }
                    }
                } catch (Exception exception) {
                    plugin.getLogger().warning("Failed to fetch name changes for " + uuid + " (" + name + "): " + exception.getMessage());
                    return;
                }
            }
        });
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
