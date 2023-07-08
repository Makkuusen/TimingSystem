package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.TrackTagManager;
import me.makkuusen.timing.system.text.Error;
import me.makkuusen.timing.system.text.Success;
import org.bukkit.command.CommandSender;

@CommandAlias("timingsystem|ts")
@CommandPermission("timingsystem.admin")
public class CommandTimingSystem extends BaseCommand {
    public static TimingSystem plugin;
    @CommandAlias("tag create")
    @CommandCompletion("<tag>")
    public void onCreateTag(CommandSender commandSender, String value) {

        if (!value.matches("[A-Za-zÅÄÖåäöØÆøæ0-9]+")) {
            plugin.sendMessage(commandSender, Error.INVALID_NAME);
            return;
        }

        if (TrackTagManager.createTrackTag(value)) {
            plugin.sendMessage(commandSender, Success.CREATED_TAG, "%tag%", value);
            return;
        }

        plugin.sendMessage(commandSender, Error.FAILED_TO_CREATE_TAG);
    }

}
