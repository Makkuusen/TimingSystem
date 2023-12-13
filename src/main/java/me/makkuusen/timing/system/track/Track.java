package me.makkuusen.timing.system.track;

import co.aikar.idb.DbRow;
import lombok.Getter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Gui;
import me.makkuusen.timing.system.track.locations.TrackLocations;
import me.makkuusen.timing.system.track.options.TrackOptions;
import me.makkuusen.timing.system.track.regions.TrackRegion;
import me.makkuusen.timing.system.track.regions.TrackRegions;
import me.makkuusen.timing.system.track.tags.TrackTag;
import me.makkuusen.timing.system.track.tags.TrackTags;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
@Getter
public class Track {
    private final int id;
    private final long dateCreated;
    private final TrackOptions trackOptions;
    private final TrackLocations trackLocations;
    private final TrackRegions trackRegions;
    private final TrackTags trackTags;
    private final TimeTrials timeTrials;
    private TPlayer owner;
    private List<TPlayer> contributors;
    private String displayName;
    private String commandName;
    private ItemStack guiItem;
    private Location spawnLocation;
    private TrackType type;
    private TrackMode mode;
    private BoatUtilsMode boatUtilsMode;
    @Getter
    private int weight;
    private boolean open;
    private long dateChanged;


    public Track(DbRow data) {
        id = data.getInt("id");
        owner = data.getString("uuid") == null ? null : TSDatabase.getPlayer(UUID.fromString(data.getString("uuid")));
        contributors = data.getString("contributors") == null ? new ArrayList<>() : ApiUtilities.tPlayersFromUUIDList(ApiUtilities.extractUUIDsFromString(data.getString("contributors")));
        displayName = data.getString("name");
        commandName = displayName.replaceAll(" ", "");
        dateCreated = data.getInt("dateCreated");
        weight = data.getInt("weight");
        guiItem = ApiUtilities.stringToItem(data.getString("guiItem"));
        spawnLocation = ApiUtilities.stringToLocation(data.getString("spawn"));
        type = data.getString("type") == null ? TrackType.BOAT : TrackType.valueOf(data.getString("type"));
        open = data.get("toggleOpen") instanceof Boolean ? data.get("toggleOpen") : data.get("toggleOpen").equals(1);
        mode = data.get("mode") == null ? TrackMode.TIMETRIAL : TrackMode.valueOf(data.getString("mode"));
        weight = data.getInt("weight");
        dateChanged = data.get("dateChanged") == null ? 0 : data.getInt("dateChanged");
        boatUtilsMode = data.get("boatUtilsMode") == null ? BoatUtilsMode.VANILLA : BoatUtilsMode.getMode(data.getInt("boatUtilsMode"));
        trackRegions = new TrackRegions(this);
        timeTrials = new TimeTrials(id);
        trackOptions = new TrackOptions(id);
        trackLocations = new TrackLocations(id);
        trackTags = new TrackTags(id);
    }

    public ItemStack getGuiItem(UUID uuid) {
        ItemStack toReturn;
        if (guiItem == null) {
            if (isBoatTrack()) {
                toReturn = new ItemBuilder(Material.PACKED_ICE).setName(getDisplayName()).build();
            } else if (isElytraTrack()) {
                toReturn = new ItemBuilder(Material.ELYTRA).setName(getDisplayName()).build();
            } else {
                toReturn = new ItemBuilder(Material.BIG_DRIPLEAF).setName(getDisplayName()).build();
            }
        } else {
            toReturn = guiItem.clone();
        }

        if (toReturn == null) {
            return null;
        }
        TPlayer tPlayer = TSDatabase.getPlayer(uuid);

        List<Component> loreToSet = new ArrayList<>();

        loreToSet.add(Text.get(tPlayer, Gui.POSITION, "%pos%", getTimeTrials().getCachedPlayerPosition(tPlayer) == -1 ? "(-)" : String.valueOf(getTimeTrials().getCachedPlayerPosition(tPlayer))));
        loreToSet.add(Text.get(tPlayer, Gui.BEST_TIME, "%time%", getTimeTrials().getBestFinish(tPlayer) == null ? "(-)" : ApiUtilities.formatAsTime(getTimeTrials().getBestFinish(tPlayer).getTime())));
        loreToSet.add(Text.get(tPlayer, Gui.TOTAL_FINISHES, "%total%", String.valueOf(getTimeTrials().getPlayerTotalFinishes(tPlayer))));
        loreToSet.add(Text.get(tPlayer, Gui.TOTAL_ATTEMPTS, "%total%", String.valueOf(getTimeTrials().getPlayerTotalFinishes(tPlayer) + getTimeTrials().getPlayerTotalAttempts(tPlayer))));
        loreToSet.add(Text.get(tPlayer, Gui.TIME_SPENT, "%time%", ApiUtilities.formatAsTimeSpent(getTimeTrials().getPlayerTotalTimeSpent(tPlayer))));
        loreToSet.add(Text.get(tPlayer, Gui.CREATED_BY, "%player%", getOwner().getName()));
        if(!getContributorsAsString().isBlank()) loreToSet.add(Text.get(tPlayer, Gui.CONTRIBUTORS, "%contributors%", getContributorsAsString()));

        Component tags = Component.empty();
        boolean notFirst = false;
        for (TrackTag tag : trackTags.getDisplayTags()) {
            if (notFirst) {
                tags = tags.append(Component.text(", ").color(tPlayer.getTheme().getSecondary()));
            }
            tags = tags.append(Component.text(tag.getValue()).color(tag.getColor()));
            notFirst = true;
        }
        loreToSet.add(Text.get(tPlayer.getPlayer(), Gui.TAGS).append(tags));


        ItemMeta im = toReturn.getItemMeta();
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        im.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
        im.addItemFlags(ItemFlag.HIDE_DYE);
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        im.displayName(Component.text(getDisplayName()).color(tPlayer.getTheme().getSecondary()));
        im.lore(loreToSet);
        toReturn.setItemMeta(im);

        return toReturn;
    }

    public void setWeight(int weight) {
        this.weight = weight;
        TimingSystem.getTrackDatabase().trackSet(id, "weight", weight);
    }

    public boolean isWeightAboveZero() {
        return weight > 0;
    }

    public void setDateChanged() {
        dateChanged = ApiUtilities.getTimestamp();
        TimingSystem.getTrackDatabase().trackSet(getId(), "dateChanged", String.valueOf(dateChanged));
    }

    public void setMode(TrackMode mode) {
        this.mode = mode;
        TimingSystem.getTrackDatabase().trackSet(id, "mode", mode.toString());
    }

    public void setBoatUtilsMode(BoatUtilsMode mode) {
        this.boatUtilsMode = mode;
        TimingSystem.getTrackDatabase().trackSet(id, "boatUtilsMode", (int) mode.getId());
    }

    public void setName(String name) {
        this.displayName = name;
        this.commandName = name.replaceAll(" ", "");
        TimingSystem.getTrackDatabase().trackSet(id, "name", name);
    }

    public void setGuiItem(ItemStack guiItem) {
        this.guiItem = guiItem;
        TimingSystem.getTrackDatabase().trackSet(id, "guiItem", ApiUtilities.itemToString(guiItem));
    }

    public void setSpawnLocation(Location spawn) {
        this.spawnLocation = spawn;
        TimingSystem.getTrackDatabase().trackSet(id, "spawn", ApiUtilities.locationToString(spawn));
    }

    public void setOpen(boolean open) {
        this.open = open;
        TimingSystem.getTrackDatabase().trackSet(id, "toggleOpen", open);
    }

    public int getNumberOfCheckpoints() {
        var checkpoints = getTrackRegions().getRegions().stream().filter(trackRegion -> trackRegion.getRegionType().equals(TrackRegion.RegionType.CHECKPOINT)).toList();
        Set<Integer> count = new HashSet<>();
        for (TrackRegion r : checkpoints) {
            count.add(r.getRegionIndex());
        }
        return count.size();
    }

    public long getTotalTimeSpent() {
        return getTimeTrials().getTotalTimeSpent();
    }

    public long getPlayerTotalTimeSpent(TPlayer tPlayer) {
        return getTimeTrials().getPlayerTotalTimeSpent(tPlayer);
    }


    public boolean isElytraTrack() {
        return getType().equals(TrackType.ELYTRA);
    }

    public boolean isBoatTrack() {
        return getType().equals(TrackType.BOAT);
    }

    public boolean isParkourTrack() {
        return getType().equals(TrackType.PARKOUR);
    }

    public boolean isTrackType(TrackType trackType) {
        return getType().equals(trackType);
    }

    public String getTypeAsString() {
        if (isBoatTrack()) {
            return "Boat";
        } else if (isParkourTrack()) {
            return "Parkour";
        } else if (isElytraTrack()) {
            return "Elytra";
        }
        return "Unknown";
    }

    public String getModeAsString() {
        if (mode.equals(TrackMode.RACE)) {
            return "Race";
        } else if (mode.equals(TrackMode.TIMETRIAL)) {
            return "Timetrial";
        }
        return "Unknown";
    }

    public void setTrackType(TrackType type) {
        this.type = type;
        TimingSystem.getTrackDatabase().trackSet(id, "type", type.toString());
    }

    public void setOwner(TPlayer owner) {
        this.owner = owner;
        TimingSystem.getTrackDatabase().trackSet(id, "uuid", owner.getUniqueId().toString());
    }

    public void addContributor(TPlayer tPlayer) {
        if(contributors.contains(tPlayer)) return;
        contributors.add(tPlayer);
        TimingSystem.getTrackDatabase().trackSet(id, "contributors", ApiUtilities.uuidListToString(ApiUtilities.uuidListFromTPlayersList(contributors))); // wow
    }

    public void removeContributor(TPlayer tPlayer) {
        contributors.remove(tPlayer);
        String uuids = ApiUtilities.uuidListToString(ApiUtilities.uuidListFromTPlayersList(contributors));
        TimingSystem.getTrackDatabase().trackSet(id, "contributors", uuids.isEmpty() ? "NULL" : uuids);
    }

    public String getContributorsAsString() {
        if(contributors.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(contributors.get(0).getName());
        List<TPlayer> rest = List.copyOf(contributors).subList(1, contributors.size());
        rest.forEach(tp -> sb.append(", ").append(tp.getName()));
        return sb.toString();
    }

    public boolean isStage() {
        return getTrackRegions().hasRegion(TrackRegion.RegionType.END);
    }

    public boolean isBoatUtils() {
        return boatUtilsMode != BoatUtilsMode.VANILLA;
    }

    public enum TrackType {
        BOAT, ELYTRA, PARKOUR
    }

    public enum TrackMode {
        TIMETRIAL, RACE
    }
}
