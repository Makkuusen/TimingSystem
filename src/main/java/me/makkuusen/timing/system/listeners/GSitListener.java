package me.makkuusen.timing.system.listeners;


import dev.geco.gsit.api.event.PrePlayerPlayerSitEvent;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class GSitListener implements Listener {

    @EventHandler
    public void onGSitEvent(PrePlayerPlayerSitEvent event) {
        if (TimeTrialController.timeTrials.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
