package me.makkuusen.timing.system;


import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RPlayer implements Comparable<RPlayer> {
	private Race plugin;

	private Player player;
	private UUID uuid;
	private String name;
	private Map<String, Boolean> toggles = new HashMap<>();


	@Override
	public int compareTo(RPlayer other) {
		return name.compareTo(other.name);
	}

	// From database
	public RPlayer(Race plugin, ResultSet data) throws SQLException {
		this.plugin = plugin;

		uuid = UUID.fromString(data.getString("uuid"));
		name = data.getString("name");
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
}
