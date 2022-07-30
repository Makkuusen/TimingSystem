package me.makkuusen.timing.system.gui;


import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.DatabaseTrack;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GUITrack {

    private static final HashMap<UUID, Integer> playersPages = new HashMap<>();

    public static final Integer BOATPAGE = 0;
    public static final Integer PARKOURPAGE = 1;

    private static ItemStack borderGlass;
    private static ItemStack lightBorderGlass;
    private static ItemStack parkourPage;
    private static ItemStack boatPage;
    private static ItemStack elytra;
    private static ItemStack boat;
    private static ItemStack parkour;

    public static void init() {

        borderGlass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName("§r").build();
        lightBorderGlass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§r").build();
        boatPage = new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setName("§b§lBoat tracks").build();
        parkourPage = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setName("§a§lParkour tracks").build();
        elytra = new ItemBuilder(Material.ELYTRA).setName("§e§lElytra").build();
        boat = new ItemBuilder(Material.OAK_BOAT).setName("§e§lBoat").build();
        parkour = new ItemBuilder(Material.BIG_DRIPLEAF).setName("§e§lParkours").build();
    }

    public static void openTrackGUI(Player p) {
        if (!playersPages.containsKey(p.getUniqueId())) {
            playersPages.put(p.getUniqueId(), BOATPAGE);
        }
        openTrackGUI(p, playersPages.get(p.getUniqueId()));
    }

    public static void openTrackGUI(Player p, int page) {
        playersPages.put(p.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, (ApiUtilities.color(page == BOATPAGE ? "&3&lBoat" : "&2&lParkour") + " tracks"));

        Integer[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (Integer slot : borderSlots) {
            inv.setItem(slot, borderGlass);
        }

        if (page == BOATPAGE) {
            inv.setItem(4, boat);
        } else if (page == 1) {
            inv.setItem(4, parkour);
        }

        inv.setItem(45, boatPage);
        inv.setItem(53, parkourPage);

        List<Track> tracks;
        if (page == BOATPAGE) {
            tracks = DatabaseTrack.getAvailableTracks(p).stream().filter(Track::isBoatTrack).collect(Collectors.toList());
        } else {
            tracks = DatabaseTrack.getAvailableTracks(p).stream().filter(Track::isParkourTrack).collect(Collectors.toList());
        }

        Integer[] slots = new Integer[36];
        int count = 9;
        for (int i = 0; i < slots.length; i++){
            slots[i] = count++;
        }

        count = 0;

        for (Track track : tracks) {
            if (count < slots.length) {
                inv.setItem(slots[count], track.getGuiItem(p.getUniqueId()));
                count++;
            }
        }

        p.openInventory(inv);
    }
}
