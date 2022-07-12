package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.participant.Driver;
import org.bukkit.entity.Player;

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
    public static void onHeatInfo(Player player, Heat heat){
        player.sendMessage("Heatstate: " + heat.getHeatState().name());
        Driver driver = heat.getDrivers().get(player.getUniqueId());
        player.sendMessage("Driver running: " +  driver.isRunning());
        player.sendMessage("Driver finished: " +  driver.isFinished());
    }

    @Subcommand("start")
    @CommandCompletion("@heat")
    public static void onHeatStart(Player player, Heat heat){
        if (heat.startHeat()) {
            player.sendMessage("§aStarted " + heat.getName());
            return;
        }
        player.sendMessage("§cCouldn't start " + heat.getName());
    }

    @Subcommand("finish")
    @CommandCompletion("@heat")
    public static void onHeatFinish(Player player, Heat heat){
        if (heat.finishHeat()) {
            player.sendMessage("§aFinished " + heat.getName());
            return;
        }
        player.sendMessage("§cCouldn't finish" + heat.getName());
        return;
    }

    @Subcommand("load")
    @CommandCompletion("@heat")
    public static void onHeatLoad(Player player, Heat heat){
        heat.loadHeat();
        player.sendMessage("§aLoaded " + heat.getName());
        return;
    }

    @Subcommand("reset")
    @CommandCompletion("@heat")
    public static void onHeatReset(Player player, Heat heat){
        heat.resetHeat();
        player.sendMessage("§aReset " + heat.getName());
        return;
    }
}
