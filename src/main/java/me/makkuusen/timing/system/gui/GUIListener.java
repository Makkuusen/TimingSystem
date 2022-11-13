package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.events.MenuClickTTEvent;
import me.makkuusen.timing.system.api.events.MenuOpenTTEvent;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Boat;
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
                Player player = (Player) e.getWhoClicked();
                if (e.getView().getTitle().startsWith(ApiUtilities.color("&3&lBoat")) || e.getView().getTitle().startsWith(ApiUtilities.color("&2&lParkour")) || e.getView().getTitle().startsWith(ApiUtilities.color("&c&lElytra"))) {
                    MenuClickTTEvent menuClickTTEvent = new MenuClickTTEvent(player);
                    Bukkit.getServer().getPluginManager().callEvent(menuClickTTEvent);

                    if (menuClickTTEvent.isCancelled()) {
                        return;
                    }
                    e.setCancelled(true);

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
                } else if (e.getView().getTitle().startsWith(ApiUtilities.color("§2§lSettings")) || e.getView().getTitle().startsWith(ApiUtilities.color("§2§lBoat Settings"))) {
                    e.setCancelled(true);

                    if ((!(e.getClickedInventory() == player.getInventory())) && e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) {

                        ItemStack item = e.getCurrentItem();

                        if (item.getItemMeta() == null || item.getType().equals(Material.LIME_STAINED_GLASS_PANE) || item.getType().equals(Material.RED_STAINED_GLASS_PANE)) {
                            return;
                        }

                        var tPlayer = Database.getPlayer(player.getUniqueId());
                        if (e.getClick() == ClickType.LEFT || e.getClick() == ClickType.SHIFT_LEFT) {
                            if (item.getItemMeta().getDisplayName().contains("Sound")) {
                                tPlayer.switchToggleSound();
                                if (tPlayer.isSound()) {
                                    playConfirm(player);
                                }
                                GUISettings.openSettingsGui(player);
                                return;
                            } else if (item.getItemMeta().getDisplayName().contains("Verbose")) {
                                tPlayer.toggleVerbose();
                                if (tPlayer.isSound()) {
                                    playConfirm(player);
                                }
                                GUISettings.openSettingsGui(player);
                                return;
                            } else if (item.getItemMeta().getDisplayName().contains("Timetrial")){
                                tPlayer.toggleTimeTrial();
                                if(tPlayer.isSound()) {
                                    playConfirm(player);
                                }
                                GUISettings.openSettingsGui(player);
                                return;
                            } else if (item.getItemMeta().getDisplayName().contains("BoatType")) {
                                if(tPlayer.isSound()) {
                                    playConfirm(player);
                                }
                                GUISettings.openSettingsBoatGui(player);
                                return;
                            } else if (item.getItemMeta().getDisplayName().contains("Override")) {
                                tPlayer.toggleOverride();
                                if(tPlayer.isSound()) {
                                    playConfirm(player);
                                }
                                GUISettings.openSettingsGui(player);
                                return;
                            } else if (item.getItemMeta().getDisplayName().contains("Boat")) {
                                tPlayer.setBoat(ApiUtilities.getBoatType(item.getType()));
                                if (player.getVehicle() instanceof Boat boat) {
                                    boat.setBoatType(tPlayer.getBoat());
                                }
                                if(tPlayer.isSound()) {
                                    playConfirm(player);
                                }
                                GUISettings.openSettingsGui(player);
                                return;
                            } else if (item.getItemMeta().getDisplayName().contains("Return")) {
                                GUISettings.openSettingsGui(player);
                                if(tPlayer.isSound()) {
                                    playConfirm(player);
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private static void playConfirm(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1, 1);
    }
}
