package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.boatutils.BoatUtilsManager;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.sounds.PlaySound;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

@CommandAlias("boat|b")
public class CommandBoat extends BaseCommand {

    @Default
    @CommandPermission("%permissiontimingsystem_boat")
    public static void onBoat(Player player) {
        if (isPlayerInBoat(player)) {
            return;
        }

        if (!player.isOnGround()) {
            Text.send(player, Error.NOT_NOW);
            return;
        }

        if (TimeTrialController.lastTimeTrialTrack.containsKey(player.getUniqueId())) {
            Track track = TimeTrialController.lastTimeTrialTrack.get(player.getUniqueId());
            ApiUtilities.spawnBoatAndAddPlayerWithBoatUtils(player, player.getLocation(), track, true);
            if (track.isBoatUtils()) {
                PlaySound.boatUtilsEffect(TSDatabase.getPlayer(player.getUniqueId()));
            }
            return;
        }
        ApiUtilities.spawnBoatAndAddPlayer(player, player.getLocation());
    }

    private static boolean isPlayerInBoat(Player p) {
        Entity v = p.getVehicle();
        if (v != null) {
            v.getType();
            return v.getType().equals(EntityType.BOAT) || v.getType().equals(EntityType.CHEST_BOAT);
        }

        return false;
    }

    @Subcommand("mode")
    @CommandCompletion("@boatUtilsMode")
    @CommandPermission("%permissiontimingsystem_boat_mode")
    public static void onBoatWithMode(Player player, BoatUtilsMode mode) {
        if (isPlayerInBoat(player)) {
            return;
        }

        if (!player.isOnGround()) {
            Text.send(player, Error.NOT_NOW);
            return;
        }

        ApiUtilities.spawnBoatAndAddPlayer(player, player.getLocation());
        BoatUtilsManager.sendBoatUtilsModePluginMessage(player, mode, null, false);
    }
}
