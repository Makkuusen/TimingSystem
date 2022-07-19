package me.makkuusen.timing.system;


import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.UUID;

public class TPlayer implements Comparable<TPlayer> {
	private TimingSystem plugin;

	private Player player;
	private UUID uuid;
	private String name;
	private long dateJoin, dateNameChange, dateNameCheck, dateSeen;


	@Override
	public int compareTo(TPlayer other) {
		return name.compareTo(other.name);
	}

	// From database
	public TPlayer(TimingSystem plugin, ResultSet data) throws SQLException {
		this.plugin = plugin;
		uuid = UUID.fromString(data.getString("uuid"));
		name = data.getString("name");
		dateJoin = (data.getString("dateJoin") == null ? 1 : 2);
		dateNameChange = data.getLong("dateNameChange");
		dateNameCheck = data.getLong("dateNameCheck");
		dateSeen = data.getLong("dateSeen");
	}

	public TPlayer(TimingSystem plugin, DbRow data)  {
		this.plugin = plugin;
		uuid = UUID.fromString(data.getString("uuid"));
		name = data.getString("name");
		if(data.get("dateJoin")!= null) dateJoin = data.get("dateJoin");
		if(data.get("dateNameChange")!= null) dateNameChange = data.getLong("dateNameChange");
		if(data.get("dateSeen")!= null) dateSeen = data.getLong("dateSeen");
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

	public void setName(String name) {
		plugin.getLogger().info("Updating name of " + uuid + " from " + this.name + " to " + name + ".");

		this.name = name;
		DB.executeUpdateAsync("UPDATE `players` SET `name` = " + Database.sqlString(name) + " WHERE `uuid` = '" + uuid + "';");

		if (player != null) {
			player.setDisplayName(getNameDisplay());
		}
	}

	public Player getPlayer() {
		return player;
	}

	void setPlayer(Player player)
	{
		// Player came online
		if (player != null)
		{
			this.player = player;

		}

		// Player disconnected
		else
		{
			this.player = null;
		}
	}

	public long getDateJoin()
	{
		return dateJoin;
	}

	public long getDateSeen()
	{
		return dateSeen;
	}

	void updateNameChanges()
	{
		// We're not bothering with checking the history if the last name change occurred less than 29 days ago
		if ((long) (ApiUtilities.getTimestamp() - dateNameChange) < 2506000) { return; }

		final TPlayer TPlayer = this;

		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					URLConnection connection = new URL("https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "") + "/names").openConnection();

					InputStream response = connection.getInputStream();
					JSONParser parser = new JSONParser();
					Object obj = parser.parse(new BufferedReader(new InputStreamReader(response, "UTF-8")));
					JSONArray jsonObject = (JSONArray) obj;

					String nameCurrent = null;
					long dateNameChange = 0;

					int newChanges = 0;

					for (@SuppressWarnings("unchecked") Iterator<JSONObject> iterator = jsonObject.iterator(); iterator.hasNext();)
					{
						JSONObject nameChange = iterator.next();

						nameCurrent = (String) nameChange.get("name");

						if (nameCurrent == null)
						{
							plugin.getLogger().warning("Failed to fetch name changes for " + uuid + " (" + name + "): Couldn't parse response from Mojang.");
							return;
						}

						Long dateNameChangeRaw = (Long) nameChange.get("changedToAt");
						dateNameChange = dateNameChangeRaw == null ? 0 : (dateNameChangeRaw / 1000);

						if (dateNameChange > TPlayer.dateNameChange)
						{
							newChanges++;
						}
					}

					if (nameCurrent == null)
					{
						plugin.getLogger().warning("Failed to fetch name changes for " + uuid + " (" + name + "): No name history found.");
						return;
					}

					TPlayer.dateNameChange = dateNameChange;
					TPlayer.dateNameCheck = ApiUtilities.getTimestamp();

					if (newChanges == 0)
					{
						DB.executeUpdateAsync("UPDATE `players` SET `dateNameCheck` = " + TPlayer.dateNameCheck + " WHERE `uuid` = '" + uuid + "';");
					}

					else
					{
						DB.executeUpdateAsync("UPDATE `players` SET `dateNameChange` = " + TPlayer.dateNameChange + ", `dateNameCheck` = " + TPlayer.dateNameCheck + " WHERE `uuid` = '" + uuid + "';");

						plugin.getLogger().info("Cached " + newChanges + " new name " + (newChanges == 1 ? "change" : "changes") + " for " + uuid + " (" + nameCurrent + ").");

						// Only update the cache if the player is offline
						if (!name.equals(nameCurrent) && player == null) { setName(nameCurrent); }
					}
				}

				catch (Exception exception)
				{
					plugin.getLogger().warning("Failed to fetch name changes for " + uuid + " (" + name + "): " + exception.getMessage());
					return;
				}
			}
		});
	}
}
