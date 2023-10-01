package me.makkuusen.timing.system.timetrial;

import me.makkuusen.timing.system.api.events.TimeTrialAttemptEvent;
import me.makkuusen.timing.system.api.events.TimeTrialFinishEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TimeTrialListener implements Listener {

    @EventHandler
    public void onTimeTrialFinishEvent(TimeTrialFinishEvent event) {
        Player player = event.getPlayer();

        if (!TimeTrialController.timeTrialSessions.containsKey(player.getUniqueId())) {
            return;
        }

        var ttSession = TimeTrialController.timeTrialSessions.get(player.getUniqueId());

        if (ttSession.track.getId() == event.getTimeTrialFinish().getTrack()) {
            ttSession.addTimeTrialFinish(event.getTimeTrialFinish());
            ttSession.updateScoreboard();
        }
    }

    @EventHandler
    public void onTimeTrialAttemptEvent(TimeTrialAttemptEvent event) {
        Player player = event.getPlayer();
        if (!TimeTrialController.timeTrialSessions.containsKey(player.getUniqueId())) {
            return;
        }

        var ttSession = TimeTrialController.timeTrialSessions.get(player.getUniqueId());

        if (ttSession.track.getId() == event.getTimeTrialAttempt().getTrackId()) {
            ttSession.addTimeTrialAttempt(event.getTimeTrialAttempt());
            ttSession.updateScoreboard();
        }
    }
}
