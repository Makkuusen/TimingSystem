package me.makkuusen.timing.system;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MainGUIListener implements Listener
{

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e)
    {
        if (e.getInventory() != null) {

            if (e.getView().getTitle() != null)
            {
                if (e.getView().getTitle().startsWith(ApiUtilities.color("&3&lPublic")) || e.getView().getTitle().startsWith(ApiUtilities.color("&2&lPrivate")))
                {
                    e.setCancelled(true);
                    Player player = (Player) e.getWhoClicked();

                    if ((!(e.getClickedInventory() == player.getInventory())) && e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR)
                    {

                        ItemStack item = e.getCurrentItem();

                        if (item.getItemMeta() == null || item.getType().equals(Material.BLACK_STAINED_GLASS_PANE) || item.getType().equals(Material.GRAY_STAINED_GLASS_PANE))
                        {
                            return;
                        }

                        if (e.getClick() == ClickType.LEFT || e.getClick() == ClickType.SHIFT_LEFT)
                        {

                            if (item.getItemMeta().getDisplayName().contains("Private"))
                            {
                                GUIManager.openMainGUI(player, 1);
                                return;
                            }
                            else if (item.getItemMeta().getDisplayName().contains("Public"))
                            {
                                GUIManager.openMainGUI(player, 0);
                                return;
                            }

                            TimeTrialsController.playerLeavingMap(e.getWhoClicked().getUniqueId());
                            String mapName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                            var maybeTrack = TrackDatabase.getRaceTrack(mapName);
                            if (maybeTrack.isPresent())
                            {
                                var raceTrack = maybeTrack.get();
                                raceTrack.teleportPlayer(((Player) e.getWhoClicked()).getPlayer());
                                e.getInventory().close();
                            }
                        }
                    }
                }
            }
        }
    }
}
