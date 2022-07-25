package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.heat.FinalHeat;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.QualifyHeat;
import me.makkuusen.timing.system.participant.Driver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("heat")
@CommandPermission("event.admin")
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
        player.sendMessage("§aHeat: " + heat.getName());
        player.sendMessage("§aHeatstate: " + heat.getHeatState().name());
        if (heat instanceof QualifyHeat qualifyHeat) {
            player.sendMessage("§aTimeLimit: " + (qualifyHeat.getTimeLimit() / 1000) + " seconds.");
        } else if (heat instanceof FinalHeat finalHeat) {
            player.sendMessage("§aLaps: " + finalHeat.getTotalLaps());
            player.sendMessage("§aPits: " + finalHeat.getTotalPits());
        }
        player.sendMessage("§aFastest lap:" + ApiUtilities.formatAsTime(heat.getFastestLap()));
        player.sendMessage("§aDrivers:");
        for (Driver d : heat.getDrivers().values()) {
            player.sendMessage("  " + d.getTPlayer().getName());
        }
    }

    @Subcommand("start")
    @CommandCompletion("@heat")
    public static void onHeatStart(Player player, Heat heat) {
        if (heat.startCountdown()) {
            player.sendMessage("§aStarted countdown for " + heat.getName());
            return;
        }
        player.sendMessage("§cCouldn't start " + heat.getName());
    }

    @Subcommand("finish")
    @CommandCompletion("@heat")
    public static void onHeatFinish(Player player, Heat heat) {
        if (heat.finishHeat()) {
            player.sendMessage("§aFinished " + heat.getName());
            return;
        }
        player.sendMessage("§cCouldn't finish" + heat.getName());
        return;
    }

    @Subcommand("load")
    @CommandCompletion("@heat")
    public static void onHeatLoad(Player player, Heat heat) {
        if (heat.loadHeat()) {
            player.sendMessage("§aLoaded " + heat.getName());
            return;
        }
        player.sendMessage("§cCouldn't load" + heat.getName());

    }

    @Subcommand("reset")
    @CommandCompletion("@heat")
    public static void onHeatReset(Player player, Heat heat) {
        heat.resetHeat();
        player.sendMessage("§aReset " + heat.getName());
        return;
    }

    @Subcommand("create qualy")
    public static void onHeatCreateQualy(Player player, @Optional Event event) {
        if (event == null) {
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(player.getUniqueId());
            if (maybeEvent.isPresent()) {
                event = maybeEvent.get();
            } else {
                player.sendMessage("§cYou have no event selected,/event select <name>");
                return;
            }
        }
        int size = event.getEventSchedule().getQualifyHeatList().size();
        if (EventDatabase.qualifyHeatNew(event, "Q" + ++size, 60000)) {
            player.sendMessage("§aHeat has been created");
            return;
        }

        player.sendMessage("§cHeat could not be created");
    }

    @Subcommand("create final")
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
        int size = event.getEventSchedule().getFinalHeatList().size();
        if (EventDatabase.finalHeatNew(event, "F" + ++size, 5, 2)) {
            player.sendMessage("§aHeat has been created");
            return;
        }
        player.sendMessage("§cHeat could not be created");
    }

    @Subcommand("set laps")
    @CommandCompletion("<laps> @heat")
    public static void onHeatSetLaps(Player player, String laps, Heat heat) {
        if (heat instanceof FinalHeat finalHeat) {
            try {
                int lap = Integer.valueOf(laps);
                finalHeat.setTotalLaps(lap);
                player.sendMessage("§aLaps has been updated");
            } catch (NumberFormatException e) {
                player.sendMessage("§cYou must provide a valid integer as laps.");
            }
        } else {
            player.sendMessage("§cYou can only modify total laps of a final heat.");
        }
    }

    @Subcommand("set pits")
    @CommandCompletion("<pits> @heat")
    public static void onHeatSetPits(Player player, String pits, Heat heat) {
        if (heat instanceof FinalHeat finalHeat) {
            try {
                int pit = Integer.valueOf(pits);
                finalHeat.setTotalPits(pit);
                player.sendMessage("§aPits has been updated");
            } catch (NumberFormatException e) {
                player.sendMessage("§cYou must provide a valid integer as pits");
            }
        } else {
            player.sendMessage("§cYou can only modify total pits of a final heat.");
        }
    }

    @Subcommand("set time")
    @CommandCompletion("<seconds> @heat")
    public static void onHeatSetTime(Player player, String seconds, Heat heat) {
        if (heat instanceof QualifyHeat qualifyHeat) {
            try {
                int time = Integer.valueOf(seconds);
                qualifyHeat.setTimeLimit(time * 1000);
                player.sendMessage("§aTime limit has been updated");
            } catch (NumberFormatException e) {
                player.sendMessage("§cYou must provide a valid integer as seconds");
            }
        } else {
            player.sendMessage("§cYou can only modify time limit of a qualy heat.");
        }
    }

    @Subcommand("add driver")
    @CommandCompletion("* @heat")
    public static void onHeatAddDriver(Player sender, OnlinePlayer onlinePlayer, Heat heat) {
        if (heat.getDrivers().get(onlinePlayer.getPlayer().getUniqueId()) != null) {
            sender.sendMessage("§cPlayer is already in heat!");
            return;
        }
        if (heat instanceof QualifyHeat qualifyHeat) {
            if (EventDatabase.qualyDriverNew(onlinePlayer.getPlayer().getUniqueId(), heat, heat.getDrivers().size() + 1)) {
                sender.sendMessage("§aAdded driver");
                return;
            }
        } else if (heat instanceof FinalHeat finalHeat) {
            if (EventDatabase.finalDriverNew(onlinePlayer.getPlayer().getUniqueId(), heat, heat.getDrivers().size() + 1)) {
                sender.sendMessage("§aAdded driver");
                return;
            }
        }
        sender.sendMessage("§cCould not add driver to heat");
    }


    @Subcommand("add alldrivers")
    @CommandCompletion("@heat")
    public static void onHeatAddDriver(Player sender,  Heat heat) {

        for (Player player : Bukkit.getOnlinePlayers()) {
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
        sender.sendMessage("§cAll online players has been added");
    }

    @Subcommand("results")
    @CommandCompletion("@heat")
    public static void onHeatResults(Player sender, Heat heat) {
        if (heat.getResults().size() != 0) {
            sender.sendMessage("§a Results for heat " + heat.getName());
            int pos = 1;
            for (Driver d : heat.getResults()) {
                sender.sendMessage("§a" + pos++ + ". " + d.getTPlayer().getName());
            }
        } else {
            sender.sendMessage("§cHeat has not been finished");
        }
    }
}

