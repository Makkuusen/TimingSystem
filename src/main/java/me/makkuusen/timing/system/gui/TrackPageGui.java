package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.theme.messages.Gui;
import me.makkuusen.timing.system.track.Track;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class TrackPageGui extends BaseGui {

    public static final Integer SORT_SLOT = 45;
    public static final Integer FILTER_SLOT = 46;

    public Integer page;
    public Integer maxPages = 1;
    public Integer TRACKS_PER_PAGE = 45;
    public TPlayer tPlayer;
    public Track.TrackType trackType = Track.TrackType.BOAT;
    public TrackSort trackSort = TrackSort.WEIGHT;
    //public TrackTag filter;
    public TrackFilter filter = new TrackFilter();

    public Comparator<Track> compareTrackPosition = (k1, k2) -> {
        if (k1.getPlayerTopListPosition(tPlayer) == -1 && k2.getPlayerTopListPosition(tPlayer) > 0) {
            return 1;
        } else if (k2.getPlayerTopListPosition(tPlayer) == -1 && k1.getPlayerTopListPosition(tPlayer) > 0) {
            return -1;
        }
        return k1.getPlayerTopListPosition(tPlayer).compareTo(k2.getPlayerTopListPosition(tPlayer));
    };

    public TrackPageGui(TPlayer tPlayer, Component title, int page) {
        super(title, 6);
        this.tPlayer = tPlayer;
        this.page = page;
        update();
    }

    public TrackPageGui(TPlayer tPlayer, Component title, int page, TrackSort trackSort, TrackFilter filter, Track.TrackType trackType) {
        super(title, 6);
        this.trackSort = trackSort;
        this.filter = filter;
        this.tPlayer = tPlayer;
        this.page = page;
        this.trackType = trackType;
        update();
    }

    public Component getTitle() {
        return TimingSystem.getPlugin().getText(tPlayer.getPlayer(), Gui.TRACKS_TITLE);
    }

    public void update() {
        setTrackButtons();
        setBorder();
        setNavigationItems();
        setSortingItem();
        setFilterItems();
    }

    public void clearNavRow() {
        List.of(45, 46, 47, 48, 49, 50, 51, 52, 53).forEach(this::removeItem);
        setBorder();
    }

    public void clearTracks() {
        Arrays.stream(getTrackSlots()).forEach(this::removeItem);
    }

    private void setBorder() {
        Integer[] borderSlots = {45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (Integer slot : borderSlots) {
            setItem(GuiCommon.getBorderGlassButton(), slot);
        }
    }

    private void setSortingItem() {
        ItemStack item;
        if (trackSort == TrackSort.CREATION) {
            item = new ItemBuilder(Material.CLOCK).setName("§eSorted by: Date Created").build();
        } else if (trackSort == TrackSort.POPULARITY) {
            item = new ItemBuilder(Material.SUNFLOWER).setName("§eSorted by: Popularity").build();
        } else if (trackSort == TrackSort.POSITION) {
            item = new ItemBuilder(Material.DRAGON_BREATH).setName("§eSorted by: Position").build();
        } else {
            item = new ItemBuilder(Material.ANVIL).setName("§eSorted by: Custom").build();
        }

        var button = new GuiButton(item);
        button.setAction(() -> {
            GuiCommon.playConfirm(tPlayer);
            clearNavRow();
            setSortingItems();
            show(tPlayer.getPlayer());
        });
        setItem(button, SORT_SLOT);
    }

    private void setSortingItems() {
        setItem(getSortingButton(new ItemBuilder(Material.ANVIL).setName("§eSorted by: Custom").build(), TrackSort.WEIGHT), 45);
        setItem(getSortingButton(new ItemBuilder(Material.DRAGON_BREATH).setName("§eSorted by: Position").build(), TrackSort.POSITION), 46);
        setItem(getSortingButton(new ItemBuilder(Material.SUNFLOWER).setName("§eSorted by: Popularity").build(), TrackSort.POPULARITY), 47);
        setItem(getSortingButton(new ItemBuilder(Material.CLOCK).setName("§eSorted by: Date Created").build(), TrackSort.CREATION), 48);
    }

    private void setFilterItems() {
        setItem(getFilterButtons(filter), FILTER_SLOT);
    }

    private GuiButton getFilterButtons(TrackFilter filter) {
        if (filter.getTags().size() == 0) {
            return getFilterButton(new ItemBuilder(Material.HOPPER).setName("§eFilter by: None").build());
        }
        return getFilterButton(filter.getItem());
    }

    public GuiButton getFilterButton(ItemStack item) {
        var button = new GuiButton(item);
        button.setAction(() -> {
            GuiCommon.playConfirm(tPlayer);
            new FilterGui(this).show(tPlayer.getPlayer());
        });
        return button;
    }

    public GuiButton getSortingButton(ItemStack item, TrackSort trackSort) {
        var button = new GuiButton(item);
        button.setAction(() -> {
            GuiCommon.playConfirm(tPlayer);
            openNewTrackPage(this, tPlayer, title, page, trackSort, filter, trackType);
        });
        return button;
    }

    private void setNavigationItems() {
        int slot = 49;

        ItemStack previous = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setName("§e§lPrevious page").build();
        ItemStack next = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setName("§e§lNext page").build();

        Material trackMaterial = Material.OAK_BOAT;
        if (trackType == Track.TrackType.ELYTRA){
            trackMaterial = Material.ELYTRA;
        } else if (trackType == Track.TrackType.PARKOUR){
            trackMaterial = Material.BIG_DRIPLEAF;
        }
        ItemStack current = new ItemBuilder(trackMaterial).setName("§e§lTrackType").build();
        current.lore(List.of(Component.text("-> Change track type <-")));

        if (page > 0) {
            setItem(getPageButton(previous, page - 1), slot - 1);
        }

        current.setAmount(page + 1);
        setItem(getTrackTypeButton(current),slot);
        setItem(getPageButton(current, page), slot);

        if (page < maxPages - 1) {
            setItem(getPageButton(next, page + 1), slot + 1);
        }
    }

    private void setTrackButtons() {
        List<Track> tempTracks = getTracks();

        //Filter TrackType
        var trackStream = tempTracks.stream().filter(Track::isWeightAboveZero).filter(track -> track.isTrackType(trackType));

        //Filter Tags
        if (filter.isAnyMatch()) {
            tempTracks = trackStream.filter(track -> track.hasAnyTag(filter)).collect(Collectors.toList());
        } else {
            tempTracks = trackStream.filter(track -> track.hasAllTags(filter)).collect(Collectors.toList());
        }

        maxPages = tempTracks.size() % TRACKS_PER_PAGE != 0 ? tempTracks.size() / TRACKS_PER_PAGE + 1 : tempTracks.size() / TRACKS_PER_PAGE;
        sortTracks(tempTracks);
        int start = TRACKS_PER_PAGE * page;

        List<Track> tracks = new ArrayList<>();
        for (int i = start; i < Math.min(start + TRACKS_PER_PAGE, tempTracks.size()); i++) {
            tracks.add(tempTracks.get(i));
        }
        setTracks(tracks, getTrackSlots());
    }

    public GuiButton getPageButton(ItemStack item, int newPage) {
        var button = new GuiButton(item);
        button.setAction(() -> {
            openNewTrackPage(this, tPlayer, title, newPage, trackSort, filter, trackType);
        });
        return button;
    }

    protected Integer[] getTrackSlots() {
        Integer[] slots = new Integer[45];
        int count = 0;
        for (int i = 0; i < slots.length; i++) {
            slots[i] = count++;
        }
        return slots;
    }

    public void setTracks(List<Track> tracks, Integer[] slots) {
        int count = 0;
        for (Track track : tracks) {
            if (count < slots.length) {
                setItem(getTrackButton(tPlayer.getPlayer(), track), slots[count]);
                count++;
            }
        }
    }

    public abstract GuiButton getTrackButton(Player player, Track track);

    public abstract List<Track> getTracks();

    public void sortTracks(List<Track> tracks) {
        if (trackSort == TrackSort.POPULARITY) {
            tracks.sort(Comparator.comparingLong(Track::getTotalTimeSpent).reversed());
        } else if (trackSort == TrackSort.WEIGHT) {
            tracks.sort(Comparator.comparingInt(Track::getWeight).reversed());
        } else if (trackSort == TrackSort.POSITION) {
            tracks.sort(compareTrackPosition);
        }
    }


    public void setTrackTypeButtons() {
        var boatButton = new GuiButton(new ItemBuilder(Material.OAK_BOAT).setName("§e§lBoat Tracks").build());
        boatButton.setAction(() -> {
            openNewTrackPage(this, tPlayer, title, 0, trackSort, filter, Track.TrackType.BOAT);
        });
        setItem(boatButton,48);

        var elytraButton = new GuiButton(new ItemBuilder(Material.ELYTRA).setName("§e§lElytra Tracks").build());
        elytraButton.setAction(() -> {
            openNewTrackPage(this, tPlayer, title, 0, trackSort, filter, Track.TrackType.ELYTRA);
        });
        setItem(elytraButton,49);

        var parkourButton = new GuiButton(new ItemBuilder(Material.BIG_DRIPLEAF).setName("§e§lParkour Tracks").build());
        parkourButton.setAction(() -> {
            openNewTrackPage(this, tPlayer, title, 0, trackSort, filter, Track.TrackType.PARKOUR);
        });
        setItem(parkourButton,50);
    }

    public GuiButton getTrackTypeButton(ItemStack item) {
        var button = new GuiButton(item);
        button.setAction(() -> {
            GuiCommon.playConfirm(tPlayer);
            clearNavRow();
            setTrackTypeButtons();
            show(tPlayer.getPlayer());
        });
        return button;
    }

    public static void openNewTrackPage(TrackPageGui gui, TPlayer tPlayer, Component title, int page, TrackSort trackSort, TrackFilter filter, Track.TrackType trackType) {
        var constructors = gui.getClass().getDeclaredConstructors();
        for (var construct : constructors) {
            if (construct.getParameterCount() == 6) {
                try {
                    var instance = (TrackPageGui) construct.newInstance(tPlayer, title, page, trackSort, filter, trackType);
                    instance.show(tPlayer.getPlayer());
                } catch (Exception e) {
                    //sadge
                }
            }
        }
    }
}


