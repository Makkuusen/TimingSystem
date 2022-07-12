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

    @Subcommand("start")
    @CommandCompletion("@heat")
    public static void onHeatStart(Player player, Heat heat){
        if (heat.startHeat()) {
            player.sendMessage("§aStarted " + heat.getName());
            return;
        }
        player.sendMessage("§cCouldn't start " + heat.getName());
    }

    @Subcommand("reset")
    @CommandCompletion("@heat")
    public static void onHeatReset(Player player, Heat heat){
        heat.resetHeat();
        player.sendMessage("§aReset " + heat.getName());
        return;
    }
}
