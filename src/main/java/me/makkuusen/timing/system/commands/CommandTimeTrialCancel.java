package me.makkuusen.timing.system.commands;


import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import org.bukkit.entity.Player;

@CommandAlias("timetrialcancel|ttcancel|cancel|ttc")
public class CommandTimeTrialCancel extends BaseCommand {

    @Default
    @CommandPermission("%permissiontimetrial_cancel")
    public static void onCancel(Player player) {
        if (!TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
            Text.send(player, Error.NOT_NOW);
            return;
        }
        TimeTrialController.playerCancelMap(player);
        Text.send(player, Success.TIME_TRIAL_CANCELLED);
    }
}
