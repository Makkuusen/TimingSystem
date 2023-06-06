package me.makkuusen.timing.system;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;


public class CommandCooldownManager {

	private static HashMap<UUID, Instant> commandBoatLastUsed = new HashMap<UUID, Instant>();
	private static final long COMMAND_BOAT_DEFAULT_COOLDOWN = 3000;

	/**
	 * This method checks if the cooldown is complete for the /boat command.
	 * This method will send a cooldown message to the player if required.
	 *
	 * @param player The {@link Player} who is performing the command.
	 * @return This method will return true if the cooldown is complete or it will
	 *         return false if the cooldown is not finished.
	 */
	public static boolean isCommandBoatCooldownComplete(Player player) {
		if (TimingSystem.getPlugin().getConfig().getLong("commandcooldowns.boat") < 0) {
			// Disable /boat cooldown if cooldown is set below 0
			return true;
		}
		
		UUID uuid = player.getUniqueId();
		if (commandBoatLastUsed.containsKey(uuid)) {
			// Has used /boat command since last reboot.
			Instant lastUsed = commandBoatLastUsed.get(uuid);
			long millsUntilNextUse = Duration.between(lastUsed, Instant.now()).toMillis();

			if (millsUntilNextUse > getCommandBoatCooldownFromConfig()) {
				// Allow command again
				commandBoatLastUsed.put(uuid, Instant.now());
				return true;
			} else {
				// Send wait for cooldown message
				String message = TimingSystem.getPlugin().getLocalizedMessage(player, "messages.error.commandCooldownGeneric", 
	        			"%cooldownRemaining%", ApiUtilities.formatAsTime(getCommandBoatCooldownFromConfig()-millsUntilNextUse));      
				player.sendMessage(message);
				return false;
			}

		}

		// Hasn't used /boat since the last reboot.
		commandBoatLastUsed.put(uuid, Instant.now());
		return true;

	}

	private static long getCommandBoatCooldownFromConfig() {
		long boatCommandCooldown = TimingSystem.getPlugin().getConfig().getLong("commandcooldowns.boat");

		if (boatCommandCooldown == 0) {
			boatCommandCooldown = COMMAND_BOAT_DEFAULT_COOLDOWN;
		} 
		return boatCommandCooldown;
	}
}
