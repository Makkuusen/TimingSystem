package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.event.EventResults;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.round.FinalRound;
import me.makkuusen.timing.system.round.QualificationRound;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("round")
public class CommandRound extends BaseCommand {

    @Default
    @Subcommand("list")
    public static void onRounds(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        var messages = event.eventSchedule.getRoundList(event);
        messages.forEach(message -> player.sendMessage(message));
        return;
    }

    @Subcommand("create")
    @CommandCompletion("@roundType")
    @CommandPermission("event.admin")
    public static void onCreate(Player player, RoundType roundType, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }

        if (event.getTrack() == null) {
            player.sendMessage("§cYou need to select a track first");
            return;
        }
        if (event.getTrack().isStage() && roundType.equals(RoundType.QUALIFICATION)) {
            player.sendMessage("§cThis track does not support qualification");
            return;
        }

        if (EventDatabase.roundNew(event, roundType, event.getEventSchedule().getRounds().size() + 1)) {
            player.sendMessage("§aCreated " + roundType.name() + " round.");
            return;
        }
        player.sendMessage("§cCould not create new round");
    }

    @Subcommand("delete")
    @CommandCompletion("@round")
    public static void onDelete(Player player, Round round) {
        if (EventDatabase.removeRound(round)){
            player.sendMessage("§a" + round.getDisplayName() + " was removed.");
            return;
        }
        player.sendMessage("§c" + round.getDisplayName() + " could not be removed");
    }

    @Subcommand("info")
    @CommandCompletion("@round")
    public static void onHeatInfo(Player player, Round round) {
        player.sendMessage("§2Round: §a" + round.getDisplayName());
        player.sendMessage("§2Roundtype: §a" + round.getType().name());
        player.sendMessage("§2Roundstate: §a" + round.getState().name());
        player.sendMessage("§2Heats: " + round.getHeats().size());
    }


    @Subcommand("finish")
    @CommandPermission("event.admin")
    public static void onRoundFinish(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        if (event.eventSchedule.getRound().get().finish(event)) {
            player.sendMessage("§aRound has been finished!");
        } else {
            player.sendMessage("§cRound could not be finished");
        }
    }

    @Subcommand("results")
    @CommandCompletion("@round")
    public static void onRoundResults(Player player, Round round, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        List<Driver> results = EventResults.generateRoundResults(round.getHeats());

        if (results.size() != 0) {
            player.sendMessage("§2Round results for event §a" + event.getDisplayName());
            int pos = 1;
            if (round instanceof FinalRound){
                for (Driver d : results) {
                    player.sendMessage("§2" + pos++ + ". §a" + d.getTPlayer().getName() + "§2 - §a" + d.getLaps().size() + " §2laps in §a" + ApiUtilities.formatAsTime(d.getFinishTime()));
                }
            } else {
                for (Driver d : results) {
                    player.sendMessage("§2" + pos++ + ". §a" + d.getTPlayer().getName() + "§2 - §a" + (d.getBestLap().isPresent() ? ApiUtilities.formatAsTime(d.getBestLap().get().getLapTime()) : "0"));
                }
            }
        } else {
            player.sendMessage("§cRound has not been finished");
        }
    }

}
