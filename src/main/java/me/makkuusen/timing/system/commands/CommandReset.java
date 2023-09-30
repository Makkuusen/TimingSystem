package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.permissions.PermissionTimingSystem;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import org.bukkit.entity.Player;

@CommandAlias("reset|re")
public class CommandReset extends BaseCommand {

    @Default
    public static void onReset(Player player) {
        if(!player.hasPermission(PermissionTimingSystem.RESET.getNode())) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }
        ApiUtilities.resetPlayerTimeTrial(player);
    }
}
