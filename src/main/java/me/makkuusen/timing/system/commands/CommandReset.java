package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import me.makkuusen.timing.system.ApiUtilities;
import org.bukkit.entity.Player;

@CommandAlias("reset|re")
public class CommandReset extends BaseCommand {

    @Default
    @CommandPermission("%permissiontimingsystem_reset")
    public static void onReset(Player player) {
        ApiUtilities.resetPlayerTimeTrial(player);
    }
}
