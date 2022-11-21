package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;

public abstract class TrackPageGui extends BaseGui {

    public static final List<Integer> BOATPAGES = List.of(0,1,2,3);
    public static final Integer PARKOURPAGE = 8;
    public static final Integer ELYTRAPAGE = 7;
    public TrackSort trackSort = TrackSort.CREATION;

    public TrackPageGui(TPlayer tPlayer, String title, int rows, int page) {
        super(title, rows);
        setBorder();
        setPageItem(page);
        setNavigationItems(tPlayer, page);
        setTrackButtons(tPlayer, page);
        setSortingItems(tPlayer, page);
    }

    public TrackPageGui(TPlayer tPlayer, String title, int rows, int page, TrackSort trackSort) {
        super(title, rows);
        this.trackSort = trackSort;
        setBorder();
        setPageItem(page);
        setNavigationItems(tPlayer, page);
        setTrackButtons(tPlayer, page);
        setSortingItems(tPlayer, page);
    }

    private void setBorder() {
        Integer[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (Integer slot : borderSlots) {
            setItem(ButtonUtilities.getBorderGlassButton(), slot);
        }
    }

    private void setPageItem(int page){
        if (page == PARKOURPAGE) {
            setItem(ButtonUtilities.getParkourButton(), 4);
        } else if (page == ELYTRAPAGE) {
            setItem(ButtonUtilities.getElytraButton(), 4);
        } else {
            setItem(ButtonUtilities.getBoatButton(), 4);
        }
    }

    private void setSortingItems(TPlayer tPlayer, int page){
        if (trackSort == TrackSort.CREATION) {
            setItem(getSortingButtons(tPlayer, page, TrackSort.POPULARITY), 0);
        } else {
            setItem(getSortingButtons(tPlayer, page, TrackSort.CREATION), 0);
        }
    }

    private GuiButton getSortingButtons(TPlayer tPlayer, int page, TrackSort trackSort){
        if (trackSort == TrackSort.POPULARITY) {
             return getSortingButton(new ItemBuilder(Material.SUNFLOWER).setName("§eSort by: Popularity").build(), tPlayer, page, trackSort);
        } else {
            return getSortingButton(new ItemBuilder(Material.CLOCK).setName("§eSort by: Date Created").build(), tPlayer, page, trackSort);
        }
    }

    public abstract GuiButton getSortingButton(ItemStack itemStack, TPlayer tPlayer, int page, TrackSort trackSort);

    private void setNavigationItems(TPlayer tPlayer, int page){
        int slot = 45;
        for (Integer boatPage : BOATPAGES) {
            if (boatPage != page) {
                setItem(getPageButton(ButtonUtilities.boatPages.get(boatPage), tPlayer, boatPage), slot);
            } else {
                setItem(getPageButton(new ItemBuilder(Material.PAPER).setName("§e§lCurrent page").build(),tPlayer, page), slot);
            }
            slot++;
        }
        if (ELYTRAPAGE != page) {
            setItem(getPageButton(ButtonUtilities.elytraPage, tPlayer, ELYTRAPAGE), 52);
        } else {
            setItem(getPageButton(new ItemBuilder(Material.PAPER).setName("§e§lCurrent page").build(),tPlayer, page), 52);
        }

        if (PARKOURPAGE != page) {
            setItem(getPageButton(ButtonUtilities.parkourPage, tPlayer, PARKOURPAGE), 53);
        } else {
            setItem(getPageButton(new ItemBuilder(Material.PAPER    ).setName("§e§lCurrent page").build(),tPlayer, page), 53);
        }
    }

    private void setTrackButtons(TPlayer tPlayer, int page) {
        List<Track> tracks = getTracks(page, trackSort);
        setTracks(tracks, tPlayer, getTrackSlots());
    }

    public abstract GuiButton getPageButton(ItemStack item, TPlayer tPlayer, int page);

    protected Integer[] getTrackSlots(){
        Integer[] slots = new Integer[36];
        int count = 9;
        for (int i = 0; i < slots.length; i++){
            slots[i] = count++;
        }
        return slots;
    }

    public void setTracks(List<Track> tracks, TPlayer tPlayer, Integer[] slots){
        int count = 0;
        for (Track track : tracks) {
            if (count < slots.length) {
                setItem(getTrackButton(tPlayer.getPlayer(), track), slots[count]);
                count++;
            }
        }
    }

    public abstract GuiButton getTrackButton(Player player, Track track);

    public abstract List<Track> getTracks(int page, TrackSort trackSort);

    public void sortTracks(List<Track> tracks, TrackSort trackSort) {
        if (trackSort == TrackSort.POPULARITY) {
            tracks.sort(Comparator.comparingLong(Track::getTotalTimeSpent).reversed());
        }
    }



}
