package me.makkuusen.timing.system;


import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GUIManager
{

    private static final HashMap<UUID, Integer> playersPages = new HashMap<>();

    public static final Integer GOVERNMENTPAGE = 0;
    public static final Integer PERSONALPAGE = 1;

    private static ItemStack borderGlass;
    private static ItemStack lightBorderGlass;
    private static ItemStack personalPage;
    private static ItemStack governmentPage;
    private static ItemStack elytra;
    private static ItemStack boat;
    private static ItemStack parkour;

    public static Component governmentTitle;
    public static Component personalTitle;

    public static void init()
    {

        borderGlass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName("§r").build();
        lightBorderGlass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§r").build();
        governmentPage = new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setName("§b§lPublic tracks").build();
        personalPage = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setName("§a§lPrivate tracks").build();
        elytra = new ItemBuilder(Material.ELYTRA).setName("§e§lElytra").build();
        boat = new ItemBuilder(Material.OAK_BOAT).setName("§e§lBoat").build();
        parkour = new ItemBuilder(Material.BIG_DRIPLEAF).setName("§e§lParkours").build();
    }

    public static void openMainGUI(Player p)
    {
        if (!playersPages.containsKey(p.getUniqueId()))
        {
            playersPages.put(p.getUniqueId(), GOVERNMENTPAGE);
        }
        openMainGUI(p, playersPages.get(p.getUniqueId()));
    }

    public static void openMainGUI(Player p, int page)
    {
        playersPages.put(p.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, (ApiUtilities.color(page == GOVERNMENTPAGE ? "&3&lPublic" : "&2&lPrivate") + " tracks"));

        Integer[] borderSlots = {0, 2, 3, 5, 6, 8, 45, 46, 47, 51, 52, 53};
        for (Integer slot : borderSlots)
        {
            inv.setItem(slot, borderGlass);
        }

        Integer[] lightBorderSlots = {3, 5, 48, 49, 50};
        for (Integer slot : lightBorderSlots)
        {
            inv.setItem(slot, lightBorderGlass);
        }

        inv.setItem(1, boat);
        inv.setItem(4, parkour);
        inv.setItem(7, elytra);

        inv.setItem(45, governmentPage);
        inv.setItem(53, personalPage);

        List<TSTrack> tracks;
        if (page == GOVERNMENTPAGE)
        {
            tracks = TrackDatabase.getAvailableRaceTracks(p).stream().filter(TSTrack::isGovernment).collect(Collectors.toList());
        }
        else
        {
            tracks = TrackDatabase.getAvailableRaceTracks(p).stream().filter(TSTrack::isPersonal).collect(Collectors.toList());
        }

        Integer[] boatSlots = {9, 10, 11, 18, 19, 20, 27, 28, 29, 36, 37, 38};
        Integer[] parkourSlots = {12, 13, 14, 21, 22, 23, 30, 31, 32, 39, 40, 41};
        Integer[] elytraSlots = {15, 16, 17, 24, 25, 26, 33, 34, 35, 42, 43, 44};

        int boatCount = 0;
        int parkourCount = 0;
        int elytraCount = 0;
        for (TSTrack track : tracks)
        {
            if (track.isBoatTrack() && boatCount < boatSlots.length)
            {
                inv.setItem(boatSlots[boatCount], track.getGuiItem(p.getUniqueId()));
                boatCount++;
            }
            else if (track.isElytraTrack() && elytraCount < elytraSlots.length)
            {
                inv.setItem(elytraSlots[elytraCount], track.getGuiItem(p.getUniqueId()));
                elytraCount++;
            }
            else if (track.isParkourTrack() && parkourCount < parkourSlots.length)
            {
                inv.setItem(parkourSlots[parkourCount], track.getGuiItem(p.getUniqueId()));
                parkourCount++;
            }
        }

        p.openInventory(inv);
    }
}
