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
import me.makkuusen.timing.system.heat.FinalHeat;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.HeatState;
import me.makkuusen.timing.system.heat.QualifyHeat;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.track.TrackRegion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("heat")
public class CommandHeat extends BaseCommand {

    @Default
    @Subcommand("list")
    public static void onHeats(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected");
                return;
            }
        }
        var messages = event.getHeatList();
        messages.forEach(message -> player.sendMessage(message));
        return;
    }

    @Subcommand("info")
    @CommandCompletion("@heat")
    public static void onHeatInfo(Player player, Heat heat) {
        player.sendMessage("§2Heat: §a" + heat.getName());
        player.sendMessage("§2Heatstate: §a" + heat.getHeatState().name());
        if (heat instanceof QualifyHeat qualifyHeat) {
            player.sendMessage("§2TimeLimit: §a" + (qualifyHeat.getTimeLimit() / 1000) + "s");
            player.sendMessage("§2StartDelay: §a" + (qualifyHeat.getStartDelay()) + "s");
        } else if (heat instanceof FinalHeat finalHeat) {
            player.sendMessage("§2Laps: §a" + finalHeat.getTotalLaps());
            player.sendMessage("§2Pits: §a" + finalHeat.getTotalPits());
        }
        if (heat.getFastestLapUUID() != null) {
            Driver d = heat.getDrivers().get(heat.getFastestLapUUID());
            player.sendMessage("§2Fastest lap: §a" + ApiUtilities.formatAsTime(d.getBestLap().get().getLapTime()) + " §2by §a" + d.getTPlayer().getName());
        }
        player.sendMessage("§2MaxDrivers: §a" + heat.getMaxDrivers());
        player.sendMessage("§2Drivers:");
        for (Driver d : heat.getStartPositions()) {
            player.sendMessage("  " + d.getStartPosition() + ": "+ d.getTPlayer().getName());
        }
    }

    @Subcommand("start")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat")
    public static void onHeatStart(Player player, Heat heat) {
        if (heat.startCountdown()) {
            player.sendMessage("§aStarted countdown for " + heat.getName());
            return;
        }
        player.sendMessage("§cCouldn't start " + heat.getName());
    }

    @Subcommand("finish")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat")
    public static void onHeatFinish(Player player, Heat heat) {
        if (heat.finishHeat()) {
            player.sendMessage("§aFinished " + heat.getName());
            return;
        }
        player.sendMessage("§cCouldn't finish " + heat.getName());
        return;
    }

    @Subcommand("load")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat")
    public static void onHeatLoad(Player player, Heat heat) {
        if (heat.loadHeat()) {
            player.sendMessage("§aLoaded " + heat.getName());
            return;
        }
        player.sendMessage("§cCouldn't load " + heat.getName());

    }

    @Subcommand("reset")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat")
    public static void onHeatReset(Player player, Heat heat) {
        if (heat.resetHeat()){
            player.sendMessage("§aReset " + heat.getName());
            return;
        }
        player.sendMessage("§cCould not reset " + heat.getName());
        return;
    }

    @Subcommand("delete")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat")
    public static void onHeatRemove(Player player, Heat heat) {
        if (EventDatabase.removeHeat(heat)){
            player.sendMessage("§aHeat was removed");
            return;
        }
        player.sendMessage("§cHeat could not be removed. Is the event already finished?");
    }

    @Subcommand("create qualy")
    @CommandPermission("event.admin")
    public static void onHeatCreateQualy(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected, /event select <name>");
                return;
            }
        }
        if (event.getTrack() == null) {
            player.sendMessage("§cYour event needs a track, /event set track <name>");
            return;
        }
        if (event.getState() != Event.EventState.SETUP && event.getState() != Event.EventState.QUALIFICATION) {
            player.sendMessage("§cYour event is already in finals");
            return;
        }
        int size = event.getEventSchedule().getQualifyHeatList().size();
        if (EventDatabase.qualifyHeatNew(event, size + 1, TimingSystem.configuration.getTimeLimit() * 1000)) {
            player.sendMessage("§aHeat has been created");
            return;
        }

        player.sendMessage("§cHeat could not be created");
    }

    @Subcommand("create final")
    @CommandPermission("event.admin")
    public static void onHeatCreateFinal(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected, /event select <name>");
                return;
            }
        }
        if (event.getTrack() == null) {
            player.sendMessage("§cYour event needs a track, /event set track <name>");
            return;
        }
        if (event.getState() == Event.EventState.FINISHED) {
            player.sendMessage("§cYour event is already finished");
            return;
        }

        int size = event.getEventSchedule().getFinalHeatList().size();
        int pits = 0;
        var maybePit = event.getTrack().getRegion(TrackRegion.RegionType.PIT);
        if (maybePit.isPresent() && maybePit.get().isDefined()) {
            pits = TimingSystem.configuration.getPits();
        }
        if (EventDatabase.finalHeatNew(event, size + 1, TimingSystem.configuration.getLaps(), pits)) {
            player.sendMessage("§aHeat has been created");
            return;
        }
        player.sendMessage("§cHeat could not be created");
    }

    @Subcommand("set laps")
    @CommandPermission("event.admin")
    @CommandCompletion("<laps> @heat")
    public static void onHeatSetLaps(Player player, Integer laps, Heat heat) {
        if (heat instanceof FinalHeat finalHeat) {
                finalHeat.setTotalLaps(laps);
                player.sendMessage("§aLaps has been updated");
        } else {
            player.sendMessage("§cYou can only modify total laps of a final heat.");
        }
    }

    @Subcommand("set pits")
    @CommandPermission("event.admin")
    @CommandCompletion("<pits> @heat")
    public static void onHeatSetPits(Player player, Integer pits, Heat heat) {
        if (heat instanceof FinalHeat finalHeat) {
            finalHeat.setTotalPits(pits);
            player.sendMessage("§aPits has been updated");
        } else {
            player.sendMessage("§cYou can only modify total pits of a final heat.");
        }
    }

    @Subcommand("set startdelay")
    @CommandPermission("event.admin")
    @CommandCompletion("<startdelay> @heat")
    public static void onHeatStartDelay(Player player, Integer startDelay, Heat heat) {
        if (heat instanceof QualifyHeat) {
            heat.setStartDelay(startDelay);
            player.sendMessage("§aStart delay has been updated");
        } else {
            player.sendMessage("§cYou can only modify the start delay of a qualifying heat.");
        }
    }

    @Subcommand("set timeLimit")
    @CommandPermission("event.admin")
    @CommandCompletion("<seconds> @heat")
    public static void onHeatSetTime(Player player, Integer seconds, Heat heat) {
        if (heat instanceof QualifyHeat qualifyHeat) {
            qualifyHeat.setTimeLimit(seconds * 1000);
            player.sendMessage("§aTime limit has been updated");
        } else {
            player.sendMessage("§cYou can only modify time limit of a qualy heat.");
        }
    }

    @Subcommand("set maxDrivers")
    @CommandPermission("event.admin")
    @CommandCompletion("<max> @heat")
    public static void onHeatMaxDrivers(Player player, Integer maxDrivers, Heat heat) {
        heat.setMaxDrivers(maxDrivers);
        player.sendMessage("§aMax drivers has been updated");
    }

    @Subcommand("add driver")
    @CommandPermission("event.admin")
    @CommandCompletion("@players @heat")
    public static void onHeatAddDriver(Player sender, String playerName, Heat heat) {
        if (heat.getMaxDrivers() <= heat.getDrivers().size()) {
            sender.sendMessage("§cMax allowed amount of drivers have been added");
            return;
        }
        TPlayer tPlayer = Database.getPlayer(playerName);
        if (tPlayer == null) {
            sender.sendMessage("§cCould not find player");
            return;
        }


        if (heat instanceof QualifyHeat) {
            for (Heat h : heat.getEvent().getEventSchedule().getQualifyHeatList())
                if (h.getDrivers().get(tPlayer.getUniqueId()) != null) {
                    sender.sendMessage("§cPlayer is already in a qualification heat!");
                    return;
                }
        } else if (heat instanceof FinalHeat finalHeat) {
            for (Heat h : heat.getEvent().getEventSchedule().getFinalHeatList())
                if (h.getDrivers().get(tPlayer.getUniqueId()) != null) {
                    sender.sendMessage("§cPlayer is already in another final heat!");
                    return;
                }
        }
        if (heat instanceof QualifyHeat qualifyHeat) {
            if (EventDatabase.qualyDriverNew(tPlayer.getUniqueId(), heat, heat.getDrivers().size() + 1)) {
                sender.sendMessage("§aAdded driver");
                return;
            }
        } else if (heat instanceof FinalHeat finalHeat) {
            if (EventDatabase.finalDriverNew(tPlayer.getUniqueId(), heat, heat.getDrivers().size() + 1)) {
                sender.sendMessage("§aAdded driver");
                return;
            }
        }
        sender.sendMessage("§cCould not add driver to heat");
    }

    @Subcommand("delete driver")
    @CommandPermission("event.admin")
    @CommandCompletion("@players @heat")
    public static void onHeatRemoveDriver(Player sender, String playerName, Heat heat){
        TPlayer tPlayer = Database.getPlayer(playerName);
        if (tPlayer == null) {
            sender.sendMessage("§cCould not find player");
            return;
        }
        if (heat.getDrivers().get(tPlayer.getUniqueId()) == null) {
            sender.sendMessage("§cPlayer is not in heat!");
            return;
        }
        if (heat.removeDriver(heat.getDrivers().get(tPlayer.getUniqueId()))) {
            sender.sendMessage("§aDriver has been removed");
            return;
        }
        sender.sendMessage("§cDriver could not be removed");
    }


    @Subcommand("add alldrivers")
    @CommandPermission("event.admin")
    @CommandCompletion("@heat")
    public static void onHeatAddDriver(Player sender, Heat heat) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (heat.getMaxDrivers() <= heat.getDrivers().size()) {
                sender.sendMessage("§cMax allowed amount of drivers have been added");
                return;
            }
            if (heat instanceof QualifyHeat) {
                for (Heat h : heat.getEvent().getEventSchedule().getQualifyHeatList())
                    if (h.getDrivers().get(player.getUniqueId()) != null) {
                        continue;
                    }
            } else if (heat instanceof FinalHeat finalHeat) {
                for (Heat h : heat.getEvent().getEventSchedule().getFinalHeatList())
                    if (h.getDrivers().get(player.getUniqueId()) != null) {
                        continue;
                    }
            }
            if (heat.getDrivers().get(player.getUniqueId()) != null) {
                continue;
            }
            if (heat instanceof QualifyHeat qualifyHeat) {
                if (EventDatabase.qualyDriverNew(player.getUniqueId(), heat, heat.getDrivers().size() + 1)) {
                    continue;
                }
            } else if (heat instanceof FinalHeat finalHeat) {
                if (EventDatabase.finalDriverNew(player.getUniqueId(), heat, heat.getDrivers().size() + 1)) {
                    continue;
                }
            }
        }
        sender.sendMessage("§aAll online players has been added");
    }

    @Subcommand("results")
    @CommandCompletion("@heat")
    public static void onHeatResults(Player sender, Heat heat) {
        if (heat.getHeatState() == HeatState.FINISHED) {
            sender.sendMessage("§aResults for heat " + heat.getName());
            if (heat instanceof FinalHeat finalHeat){
                List<Driver> result = EventResults.generateFinalHeatResults(finalHeat);
                for (Driver d : result) {
                    sender.sendMessage("§2" + d.getPosition() + ". §a" + d.getTPlayer().getName() + "§2 - §a" + d.getLaps().size() + " §2laps in §a" + ApiUtilities.formatAsTime(d.getFinishTime()));
                }
            } else {
                List<Driver> result = EventResults.generateQualyHeatResults((QualifyHeat) heat);
                for (Driver d : result) {
                    sender.sendMessage("§2" + d.getPosition() + ". §a" + d.getTPlayer().getName() + "§2 - §a"  + (d.getBestLap().isPresent() ? ApiUtilities.formatAsTime(d.getBestLap().get().getLapTime()) : "0"));
                }
            }
        } else {
            sender.sendMessage("§cHeat has not been finished");
        }
    }
}

