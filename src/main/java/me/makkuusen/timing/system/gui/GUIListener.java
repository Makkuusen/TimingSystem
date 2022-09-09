package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory() != null) {

            if (e.getView().getTitle() != null) {
                if (e.getView().getTitle().startsWith(ApiUtilities.color("&3&lBoat")) || e.getView().getTitle().startsWith(ApiUtilities.color("&2&lParkour")) || e.getView().getTitle().startsWith(ApiUtilities.color("&c&lElytra"))) {
                    e.setCancelled(true);
                    Player player = (Player) e.getWhoClicked();

                    if ((!(e.getClickedInventory() == player.getInventory())) && e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) {

                        ItemStack item = e.getCurrentItem();

                        if (item.getItemMeta() == null || item.getType().equals(Material.BLACK_STAINED_GLASS_PANE) || item.getType().equals(Material.GRAY_STAINED_GLASS_PANE)) {
                            return;
                        }

                        if (e.getClick() == ClickType.LEFT || e.getClick() == ClickType.SHIFT_LEFT) {
                            if (item.getItemMeta().getDisplayName().contains("Parkour")) {
                                GUITrack.openTrackGUI(player, GUITrack.PARKOURPAGE);
                                return;
                            } else if (item.getItemMeta().getDisplayName().contains("Elytra")) {
                                GUITrack.openTrackGUI(player, GUITrack.ELYTRAPAGE);
                                return;
                            } else if (item.getItemMeta().getDisplayName().contains("Boat")) {
                                int page = 0;
                                try {
                                    String pageNumber = item.getItemMeta().getDisplayName().substring(item.getItemMeta().getDisplayName().length()-1);
                                    page = Integer.valueOf(pageNumber) - 1;
                                } catch (NumberFormatException exception){
                                    // default to standard
                                    exception.printStackTrace();
                                }
                                GUITrack.openTrackGUI(player, page);
                                return;
                            }

                            TimeTrialController.playerLeavingMap(e.getWhoClicked().getUniqueId());
                            String mapName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                            var maybeTrack = TrackDatabase.getTrack(mapName.replaceAll(" ",""));
                            if (maybeTrack.isPresent()) {
                                var track = maybeTrack.get();
                                if (!track.getSpawnLocation().isWorldLoaded()) {
                                    player.sendMessage("§cWorld is not loaded!");
                                    return;
                                }
                                player.teleport(track.getSpawnLocation());
                                e.getInventory().close();
                            }
                        }
                    }
                }
            }
        }
    }
}
