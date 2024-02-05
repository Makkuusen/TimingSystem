package me.makkuusen.timing.system.listeners;


import dev.geco.gsit.api.event.PreEntitySitEvent;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class GSitListener implements Listener {

    @EventHandler
    public void onGSitEvent(PreEntitySitEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (TimeTrialController.timeTrials.containsKey(player.getUniqueId())){
                event.setCancelled(true);
            }
        }
    }
}
