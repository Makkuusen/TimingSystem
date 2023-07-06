package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import me.makkuusen.timing.system.TrackTagManager;
import org.bukkit.command.CommandSender;

@CommandAlias("timingsystem|ts")
@CommandPermission("timingsystem.admin")
public class CommandTimingSystem extends BaseCommand {

    @CommandAlias("tag create")
    @CommandCompletion("<tag>")
    public void onCreateTag(CommandSender commandSender, String value) {

        if (!value.matches("[A-Za-zÅÄÖåäöØÆøæ0-9]+")) {
            commandSender.sendMessage("§cBad Formatting");
            return;
        }

        if (TrackTagManager.createTrackTag(value)) {
            commandSender.sendMessage("§aTrack tag '" + value + "' has been created.");
            return;
        }

        commandSender.sendMessage("§cTrack tag '" + value + "' could not be created.");
    }

}
