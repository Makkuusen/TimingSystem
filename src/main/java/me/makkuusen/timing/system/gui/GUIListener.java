package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.TPlayer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        TPlayer tPlayer;
        if (e.getWhoClicked() instanceof Player player) {
            tPlayer = Database.getPlayer(player.getUniqueId());
        } else {
            return;
        }

        if (tPlayer.getOpenGui() != null) {
            if (e.getCurrentItem() != null) {
                if (tPlayer.getOpenGui().handleButton(e.getCurrentItem())) {
                    e.setCancelled(true);
                } else if (tPlayer.getOpenGui().equalsInv(e.getInventory())) {
                    e.setCancelled(true);
                }
            }
        }
    }
}
