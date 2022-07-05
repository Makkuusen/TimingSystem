package me.makkuusen.timing.system;


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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.UUID;

public class TSPlayer implements Comparable<TSPlayer> {
	private TimingSystem plugin;

	private Player player;
	private UUID uuid;
	private String name;
	private long dateJoin, dateNameChange, dateNameCheck, dateSeen;


	@Override
	public int compareTo(TSPlayer other) {
		return name.compareTo(other.name);
	}

	// From database
	public TSPlayer(TimingSystem plugin, ResultSet data) throws SQLException {
		this.plugin = plugin;

		uuid = UUID.fromString(data.getString("uuid"));
		name = data.getString("name");
		dateJoin = data.getLong("dateJoin");
		dateNameChange = data.getLong("dateNameChange");
		dateNameCheck = data.getLong("dateNameCheck");
		dateSeen = data.getLong("dateSeen");
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
		ApiDatabase.asynchronousQuery(new String[]{"UPDATE `players` SET `name` = " + ApiDatabase.sqlString(name) + " WHERE `uuid` = '" + uuid + "';"});

		if (player != null) {
			player.setDisplayName(getNameDisplay());
		}
	}


	public void reload() {
		try {
			Connection connection = ApiDatabase.getConnection();
			Statement statement = connection.createStatement();

			ResultSet data = statement.executeQuery("SELECT * FROM `players` WHERE `uuid` = '" + uuid + "';");
			data.next();


			data.close();
			connection.close();

		} catch (Exception exception) {
			plugin.getLogger().warning("Failed to reload player: " + exception.getMessage());
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

		final TSPlayer TSPlayer = this;

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

						if (dateNameChange > TSPlayer.dateNameChange)
						{
							newChanges++;
						}
					}

					if (nameCurrent == null)
					{
						plugin.getLogger().warning("Failed to fetch name changes for " + uuid + " (" + name + "): No name history found.");
						return;
					}

					TSPlayer.dateNameChange = dateNameChange;
					TSPlayer.dateNameCheck = ApiUtilities.getTimestamp();

					if (newChanges == 0)
					{
						ApiDatabase.asynchronousQuery(new String[] { "UPDATE `players` SET `dateNameCheck` = " + TSPlayer.dateNameCheck + " WHERE `uuid` = '" + uuid + "';" });
					}

					else
					{
						ApiDatabase.asynchronousQuery(new String[] { "UPDATE `players` SET `dateNameChange` = " + TSPlayer.dateNameChange + ", `dateNameCheck` = " + TSPlayer.dateNameCheck + " WHERE `uuid` = '" + uuid + "';"});

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
