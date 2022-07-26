package me.makkuusen.timing.system.timetrial;

import me.makkuusen.timing.system.ApiUtilities;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class TimeTrialController {

    public static HashMap<UUID, TimeTrial> timeTrials = new HashMap<>();

    public static void playerLeavingMap(UUID uuid) {
        if (!TimeTrialController.timeTrials.containsKey(uuid)) {
            return;
        }
        TimeTrialController.timeTrials.remove(uuid);
    }

    public static void playerCancelMap(Player player) {
        if (!TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
            return;
        }
        ApiUtilities.msgConsole(player.getName() + " has cancelled run on " + TimeTrialController.timeTrials.get(player.getUniqueId()).getTrack().getDisplayName());
        TimeTrialController.timeTrials.remove(player.getUniqueId());
    }

    public static void unload(){
        timeTrials = new HashMap<>();
    }
}
