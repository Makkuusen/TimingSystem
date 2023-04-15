package me.makkuusen.timing.system;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

@CommandAlias("boat|b")
public class CommandBoat extends BaseCommand {

    @Default
    public static void onBoat(Player player){
        if (isPlayerInBoat(player)) {
            return;
        }
        ApiUtilities.spawnBoatAndAddPlayer(player, player.getLocation());
    }

    private static boolean isPlayerInBoat(Player p) {
        Entity v = p.getVehicle();
        if (v != null && v.getType() != null) {
            if (v.getType().equals(EntityType.BOAT) || v.getType().equals(EntityType.CHEST_BOAT)) {
                return true;
            }
        }

        return false;
    }
}
