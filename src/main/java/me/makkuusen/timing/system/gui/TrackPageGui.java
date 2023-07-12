package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TrackTagManager;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackTag;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;

public abstract class TrackPageGui extends BaseGui {

    public static final List<Integer> BOAT_PAGES = List.of(0, 1, 2, 3, 4, 5, 6);
    public static final Integer PARKOUR_PAGE = 8;
    public static final Integer ELYTRA_PAGE = 7;

    public Integer page;
    public TrackSort trackSort = TrackSort.WEIGHT;
    public TrackTag filter;
    public TPlayer tPlayer;
    public Comparator<Track> compareTrackPosition = (k1, k2) -> {
        if (k1.getPlayerTopListPosition(tPlayer) == -1 && k2.getPlayerTopListPosition(tPlayer) > 0) {
            return 1;
        } else if (k2.getPlayerTopListPosition(tPlayer) == -1 && k1.getPlayerTopListPosition(tPlayer) > 0) {
            return -1;
        }
        return k1.getPlayerTopListPosition(tPlayer).compareTo(k2.getPlayerTopListPosition(tPlayer));
    };

    public TrackPageGui(TPlayer tPlayer, String title, int rows, int page) {
        super(title, rows);
        this.tPlayer = tPlayer;
        this.page = page;
        update();
    }

    public TrackPageGui(TPlayer tPlayer, String title, int rows, int page, TrackSort trackSort, TrackTag filter) {
        super(title, rows);
        this.trackSort = trackSort;
        this.filter = filter;
        this.tPlayer = tPlayer;
        this.page = page;
        update();
    }

    public void update() {
        setBorder();
        setPageItem();
        setNavigationItems();
        setTrackButtons();
        setSortingItems();
        setFilterItems();
    }

    private void setBorder() {
        Integer[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (Integer slot : borderSlots) {
            setItem(ButtonUtilities.getBorderGlassButton(), slot);
        }
    }

    private void setPageItem() {
        if (page == PARKOUR_PAGE) {
            setItem(ButtonUtilities.getParkourButton(), 4);
        } else if (page == ELYTRA_PAGE) {
            setItem(ButtonUtilities.getElytraButton(), 4);
        } else {
            setItem(ButtonUtilities.getBoatButton(), 4);
        }
    }

    private void setSortingItems() {
        if (trackSort == TrackSort.CREATION) {
            setItem(getSortingButtons(TrackSort.POPULARITY), 0);
        } else if (trackSort == TrackSort.POPULARITY) {
            setItem(getSortingButtons(TrackSort.WEIGHT), 0);
        } else if (trackSort == TrackSort.WEIGHT) {
            setItem(getSortingButtons(TrackSort.POSITION), 0);
        } else {
            setItem(getSortingButtons(TrackSort.CREATION), 0);
        }
    }

    private void setFilterItems() {
        setItem(getFilterButtons(TrackTagManager.getNext(filter)), 2);
    }

    private GuiButton getFilterButtons(TrackTag next) {
        if (filter == null) {
            return getFilterButton(new ItemBuilder(Material.HOPPER).setName("§eFilter by: None").build(), tPlayer, page, trackSort, next);
        }
        return getFilterButton(new ItemBuilder(Material.HOPPER).setName("§eFilter by: " + filter.getValue()).build(), tPlayer, page, trackSort, next);
    }

    public abstract GuiButton getFilterButton(ItemStack itemStack, TPlayer tPlayer, int page, TrackSort trackSort, TrackTag tag);

    private GuiButton getSortingButtons(TrackSort trackSort) {
        if (trackSort == TrackSort.POPULARITY) {
            return getSortingButton(new ItemBuilder(Material.CLOCK).setName("§eSorted by: Date Created").build(), tPlayer, page, trackSort, filter);
        } else if (trackSort == TrackSort.WEIGHT) {
            return getSortingButton(new ItemBuilder(Material.SUNFLOWER).setName("§eSorted by: Popularity").build(), tPlayer, page, trackSort, filter);
        } else if (trackSort == TrackSort.CREATION) {
            return getSortingButton(new ItemBuilder(Material.DRAGON_BREATH).setName("§eSorted by: Position").build(), tPlayer, page, trackSort, filter);
        } else {
            return getSortingButton(new ItemBuilder(Material.ANVIL).setName("§eSorted by: Custom").build(), tPlayer, page, trackSort, filter);
        }
    }

    public abstract GuiButton getSortingButton(ItemStack itemStack, TPlayer tPlayer, int page, TrackSort trackSort, TrackTag tag);

    private void setNavigationItems() {
        int slot = 49;

        ItemStack previous = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setName("§e§lPrevious page").build();
        ItemStack next = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setName("§e§lNext page").build();
        ItemStack boat = new ItemBuilder(Material.PAPER).setName("§e§lCurrent page").build();

        if (page > 0) {
            setItem(getPageButton(previous, tPlayer, page - 1), slot - 1);
        }

        boat.setAmount(page + 1);
        setItem(getPageButton(boat, tPlayer, page), slot);

        if (page < 9) {
            setItem(getPageButton(next, tPlayer, page + 1), slot + 1);
        }
    }

    private void setTrackButtons() {
        List<Track> tracks = getTracks(page, trackSort);
        setTracks(tracks, tPlayer, getTrackSlots());
    }

    public abstract GuiButton getPageButton(ItemStack item, TPlayer tPlayer, int page);

    protected Integer[] getTrackSlots() {
        Integer[] slots = new Integer[36];
        int count = 9;
        for (int i = 0; i < slots.length; i++) {
            slots[i] = count++;
        }
        return slots;
    }

    public void setTracks(List<Track> tracks, TPlayer tPlayer, Integer[] slots) {
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
        } else if (trackSort == TrackSort.WEIGHT) {
            tracks.sort(Comparator.comparingInt(Track::getWeight).reversed());
        } else if (trackSort == TrackSort.POSITION) {
            tracks.sort(compareTrackPosition);
        }
    }

}


