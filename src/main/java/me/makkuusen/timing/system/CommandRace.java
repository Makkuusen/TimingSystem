package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import me.makkuusen.timing.system.text.TextUtilities;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("race")
public class CommandRace extends BaseCommand {

    Event event;
    Round round;
    Heat heat;

    @CommandPermission("race.admin")
    @Subcommand("start")
    public void onStart(Player player) {

        if (heat == null) {
            player.sendMessage("§cFirst you need to create a race with /race create");
            return;
        }
        if (heat.startCountdown()) {
            player.sendMessage(TextUtilities.success("Started countdown for " + heat.getName()));
            return;
        }
        player.sendMessage(TextUtilities.error("Couldn't start " + heat.getName()));

    }

    @CommandPermission("race.admin")
    @Subcommand("end")
    public void onEnd(Player player) {
        if (event == null) {
            player.sendMessage("§cThere is no race to end.");
            return;
        }
        if (heat != null && heat.getHeatState() == HeatState.RACING) {
            if (!heat.finishHeat()) {
                player.sendMessage("§cCouldn't end " + heat.getName());
                return;
            }
        } else if (heat != null && heat.getHeatState() == HeatState.LOADED) {
            heat.resetHeat();
        }

        deleteEvent();
        player.sendMessage("§aPrevious race has ended.");
    }

    @CommandPermission("race.admin")
    @Subcommand("create")
    @CommandCompletion("@track laps pits")
    public void onCreate(Player player, Track track, @Optional Integer laps, @Optional Integer pits) {

        if (heat != null) {
            if (heat.isFinished()) {
                deleteEvent();
            } else {
                player.sendMessage("§cIs old race still running? If you want to end it, do /race end.");
                return;
            }
        }

        if (!track.isOpen()) {
            player.sendMessage("§cTrack is closed and can't be used.");
            return;
        }


        String name = "QuickRace";
        var maybeEvent = EventDatabase.eventNew(player.getUniqueId(), name);
        if (maybeEvent.isEmpty()) {
            player.sendMessage(TextUtilities.error("Could not create QuickRace, check with an administrator to find out why."));
            return;
        }
        player.sendMessage("§aCreated " + RoundType.FINAL.name() + " round.");

        event = maybeEvent.get();
        event.setTrack(track);

        if (!EventDatabase.roundNew(event, RoundType.FINAL, 1)) {
            player.sendMessage("§cCould not create new round");
            return;
        }

        var maybeRound = event.getEventSchedule().getRound(1);

        if (maybeRound.isEmpty()) {
            player.sendMessage("§cCould not create heat");
            return;
        }

        round = maybeRound.get();

        round.createHeat(1);

        var maybeHeat = round.getHeat("R1F1");

        if (maybeHeat.isEmpty()) {
            player.sendMessage("§cCould not create heat");
            return;
        }

        heat = maybeHeat.get();


        if (track.isStage()) {
            heat.setTotalLaps(1);
            heat.setTotalPits(0);
        } else {
            if (laps != null && laps > 0) {
                heat.setTotalLaps(laps);
            } else {
                heat.setTotalLaps(5);
            }

            if (track.hasRegion(TrackRegion.RegionType.PIT) && pits == null) {
                heat.setTotalPits(1);
            } else if (pits != null) {
                heat.setTotalPits(Math.min(laps,pits));
            } else {
                heat.setTotalPits(0);
            }
        }

        var state = heat.getHeatState();
        if (state != HeatState.SETUP) {
            if (!heat.resetHeat()) {
                player.sendMessage("§cCouldn't reload " + heat.getName());
                return;
            }
        }

        if (!heat.loadHeat()) {
            player.sendMessage("§cCouldn't load " + heat.getName());
            deleteEvent();
            return;
        }

        var message = Component.text("§3--> Click to join a " + heat.getTotalLaps() + " laps race on §b§l" + event.getTrack().getDisplayName() + " §3<--").clickEvent(ClickEvent.runCommand("/race join"));
        for (Player p : Bukkit.getOnlinePlayers()) {

            if (heat.getDrivers().containsKey(p.getUniqueId())) {
                continue;
            }
            p.sendMessage("");
            p.sendMessage(message);
            p.sendMessage("");
        }
    }

    @Subcommand("join")
    public void onClickToJoin(Player player) {

        if (heat == null) {
            player.sendMessage("§cYou can not sign up for a race right now.");
            return;
        }

        if (heat.getHeatState() != HeatState.LOADED) {
            player.sendMessage("§cYou can not sign up for a race right now.");
            return;
        }

        if (heat.getDrivers().get(player.getUniqueId()) != null) {
            player.sendMessage("§cYou are already signed up for this race!");
            return;
        }

        if (heat.getMaxDrivers() <= heat.getDrivers().size()) {
            player.sendMessage("§cMax allowed amount of drivers have been added");
            return;
        }

        if (EventDatabase.heatDriverNew(player.getUniqueId(), heat, heat.getDrivers().size() + 1)) {
            player.sendMessage("§aYou have signed up for the race!");
            heat.reloadHeat();
            return;
        }

        player.sendMessage("§cYou can not sign up for a race right now.");
    }

    private void deleteEvent() {
        EventDatabase.removeEventHard(event);
        event = null;
        round = null;
        heat = null;
    }
}
