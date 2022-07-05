package me.makkuusen.timing.system;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class TimeTrialsController {

    public static HashMap<UUID, TimeTrial> timeTrials = new HashMap<>();

    public static void playerLeavingMap(UUID uuid)
    {
        if (!TimeTrialsController.timeTrials.containsKey(uuid))
        {
            return;
        }
        TimeTrialsController.timeTrials.remove(uuid);
    }

    public static void playerCancelMap(Player player)
    {
        if (!TimeTrialsController.timeTrials.containsKey(player.getUniqueId()))
        {
            return;
        }
        ApiUtilities.msgConsole(player.getName() + " has cancelled run on " + TimeTrialsController.timeTrials.get(player.getUniqueId()).getTrack().getName());
        TimeTrialsController.timeTrials.remove(player.getUniqueId());
    }
}
