package me.makkuusen.timing.system.gui;


import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.api.events.MenuOpenTTEvent;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GUITrack {

    public static final HashMap<UUID, Integer> playersPages = new HashMap<>();

    public static final Integer BOATPAGE = 0;
    public static final Integer PARKOURPAGE = 8;
    public static final Integer ELYTRAPAGE = 7;

    public static ItemStack borderGlass;
    public static ItemStack parkourPage;
    public static ItemStack elytraPage;
    public static List<ItemStack> boatPages = new ArrayList<>();
    public static ItemStack elytra;
    public static ItemStack boat;
    public static ItemStack parkour;

    public static void init() {

        borderGlass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName("§r").build();
        for (int i = 0; i < 4; i++){
            boatPages.add(new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setName("§b§lBoat tracks " + (i+1)).build());
        }
        elytraPage = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setName("§c§lElytra tracks").build();
        parkourPage = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setName("§a§lParkour tracks").build();
        elytra = new ItemBuilder(Material.ELYTRA).setName("§e§lElytra").build();
        boat = new ItemBuilder(Material.OAK_BOAT).setName("§e§lBoat").build();
        parkour = new ItemBuilder(Material.BIG_DRIPLEAF).setName("§e§lParkours").build();
    }

    public static void openTrackGUI(Player p) {
        MenuOpenTTEvent menuOpenTTEvent = new MenuOpenTTEvent(p);
        Bukkit.getServer().getPluginManager().callEvent(menuOpenTTEvent);

        if (menuOpenTTEvent.hasMedalsOverride()) {
            return;
        }

        if (!playersPages.containsKey(p.getUniqueId())) {
            playersPages.put(p.getUniqueId(), BOATPAGE);
        }
        openTrackGUI(p, playersPages.get(p.getUniqueId()));
    }

    public static void openTrackGUI(Player p, int page) {
        playersPages.put(p.getUniqueId(), page);
        Inventory inv = createGUI(p, page);
        List<Track> tracks = getTracks(p, page);
        Integer[] slots = getTrackSlots();
        setTracks(tracks, inv, p, slots);
        p.openInventory(inv);
    }

    public static List<Track> getTracks(Player p, int page) {
        List<Track> tracks;
        if (page == ELYTRAPAGE) {
            tracks = TrackDatabase.getAvailableTracks(p).stream().filter(Track::isElytraTrack).collect(Collectors.toList());
        } else if (page == PARKOURPAGE) {
            tracks = TrackDatabase.getAvailableTracks(p).stream().filter(Track::isParkourTrack).collect(Collectors.toList());
        } else {
            List<Track> tempTracks = TrackDatabase.getAvailableTracks(p).stream().filter(Track::isBoatTrack).collect(Collectors.toList());
            int start = 36 * page;
            tracks = new ArrayList<>();
            for (int i = start; i < Math.min(start + 36, tempTracks.size()); i++) {
                tracks.add(tempTracks.get(i));
            }
        }
        return tracks;
    }

    public static Integer[] getTrackSlots(){
        Integer[] slots = new Integer[36];
        int count = 9;
        for (int i = 0; i < slots.length; i++){
            slots[i] = count++;
        }
        return slots;
    }

    public static void setTracks(List<Track> tracks, Inventory inv, Player player, Integer[] slots){

        int count = 0;
        for (Track track : tracks) {
            if (count < slots.length) {
                inv.setItem(slots[count], track.getGuiItem(player.getUniqueId()));
                count++;
            }
        }
    }

    public static Inventory createGUI(Player p, int page) {
        playersPages.put(p.getUniqueId(), page);
        String name;
        if (page == PARKOURPAGE) {
            name = ApiUtilities.color("&2&lParkour tracks");
        } else if (page == ELYTRAPAGE) {
            name = ApiUtilities.color("&c&lElytra tracks");
        } else {
            name = ApiUtilities.color("&3&lBoat tracks " + (page+1));
        }
        Inventory inv = Bukkit.createInventory(null, 54, name);

        Integer[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (Integer slot : borderSlots) {
            inv.setItem(slot, borderGlass);
        }

        if (page == PARKOURPAGE) {
            inv.setItem(4, parkour);
        } else if (page == ELYTRAPAGE) {
            inv.setItem(4, elytra);
        } else {
            inv.setItem(4, boat);
        }

        int slot = 45;
        for (ItemStack item : boatPages){
            inv.setItem(slot, item);
            slot++;
        }
        inv.setItem(52, elytraPage);
        inv.setItem(53, parkourPage);

        return inv;
    }
}
