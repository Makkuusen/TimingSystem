package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.database.TSDatabase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;

public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        TPlayer tPlayer;
        if (e.getWhoClicked() instanceof Player player) {
            tPlayer = TSDatabase.getPlayer(player.getUniqueId());
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

    @EventHandler
    public void onInventoryDragEvent(InventoryDragEvent e) {
        TPlayer tPlayer;
        if (e.getWhoClicked() instanceof Player player) {
            tPlayer = TSDatabase.getPlayer(player.getUniqueId());
        } else {
            return;
        }

        if (tPlayer.getOpenGui() != null) {
            e.setCancelled(true);
        }
    }

}
