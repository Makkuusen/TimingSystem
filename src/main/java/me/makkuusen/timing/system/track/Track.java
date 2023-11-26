package me.makkuusen.timing.system.track;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import lombok.Getter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.gui.TrackFilter;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Gui;
import me.makkuusen.timing.system.timetrial.TimeTrialAttempt;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.timetrial.TimeTrialFinishComparator;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class Track {
    private final int id;
    private final long dateCreated;

    private final Set<TrackRegion> regions = new HashSet<>();
    private final Set<TrackLocation> trackLocations = new HashSet<>();
    private final Set<TrackTag> tags = new HashSet<>();
    private final Map<TPlayer, List<TimeTrialAttempt>> timeTrialAttempts = new HashMap<>();
    private Map<TPlayer, List<TimeTrialFinish>> timeTrialFinishes = new HashMap<>();
    private List<TPlayer> cachedPositions = new ArrayList<>();
    private TPlayer owner;
    private List<TPlayer> contributors;
    private String displayName;
    private String commandName;
    private ItemStack guiItem;
    private Location spawnLocation;
    private TrackType type;
    private TrackMode mode;
    private BoatUtilsMode boatUtilsMode;
    private int weight;
    private char[] options;
    private boolean open;
    private long dateChanged;
    private long totalTimeSpent = 0;


    public Track(DbRow data) {
        id = data.getInt("id");
        owner = data.getString("uuid") == null ? null : Database.getPlayer(UUID.fromString(data.getString("uuid")));
        contributors = data.getString("contributors") == null ? new ArrayList<>() : ApiUtilities.tPlayersFromUUIDList(ApiUtilities.extractUUIDsFromString(data.getString("contributors")));
        displayName = data.getString("name");
        commandName = displayName.replaceAll(" ", "");
        dateCreated = data.getInt("dateCreated");
        weight = data.getInt("weight");
        guiItem = ApiUtilities.stringToItem(data.getString("guiItem"));
        spawnLocation = ApiUtilities.stringToLocation(data.getString("spawn"));
        type = data.getString("type") == null ? TrackType.BOAT : TrackType.valueOf(data.getString("type"));
        open = data.get("toggleOpen");
        options = data.getString("options") == null ? new char[0] : data.getString("options").toCharArray();
        mode = data.get("mode") == null ? TrackMode.TIMETRIAL : TrackMode.valueOf(data.getString("mode"));
        weight = data.getInt("weight");
        dateChanged = data.get("dateChanged") == null ? 0 : data.getInt("dateChanged");
        boatUtilsMode = data.get("boatUtilsMode") == null ? BoatUtilsMode.VANILLA : BoatUtilsMode.valueOf(data.getString("boatUtilsMode"));
    }

    public static ContextResolver<TrackType, BukkitCommandExecutionContext> getTrackTypeContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return TrackType.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    public static ContextResolver<TrackMode, BukkitCommandExecutionContext> getTrackModeContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return TrackMode.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
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
        TPlayer tPlayer = Database.getPlayer(uuid);

        List<Component> loreToSet = new ArrayList<>();

        loreToSet.add(Text.get(tPlayer, Gui.POSITION, "%pos%", getCachedPlayerPosition(tPlayer) == -1 ? "(-)" : String.valueOf(getCachedPlayerPosition(tPlayer))));
        loreToSet.add(Text.get(tPlayer, Gui.BEST_TIME, "%time%", getBestFinish(tPlayer) == null ? "(-)" : ApiUtilities.formatAsTime(getBestFinish(tPlayer).getTime())));
        loreToSet.add(Text.get(tPlayer, Gui.TOTAL_FINISHES, "%total%", String.valueOf(getPlayerTotalFinishes(tPlayer))));
        loreToSet.add(Text.get(tPlayer, Gui.TOTAL_ATTEMPTS, "%total%", String.valueOf(getPlayerTotalFinishes(tPlayer) + getPlayerTotalAttempts(tPlayer))));
        loreToSet.add(Text.get(tPlayer, Gui.TIME_SPENT, "%time%", ApiUtilities.formatAsTimeSpent(getPlayerTotalTimeSpent(tPlayer))));
        loreToSet.add(Text.get(tPlayer, Gui.CREATED_BY, "%player%", getOwner().getName()));
        if(!getContributorsAsString().isBlank()) loreToSet.add(Text.get(tPlayer, Gui.CONTRIBUTORS, "%contributors%", getContributorsAsString()));

        Component tags = Component.empty();
        boolean notFirst = false;
        for (TrackTag tag : getDisplayTags()) {
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

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `weight` = " + weight + " WHERE `id` = " + id + ";");
    }

    public boolean isWeightAboveZero() {
        return weight > 0;
    }

    public boolean hasTag(TrackTag tag) {
        if (tag == null) {
            return true;
        }
        return tags.contains(tag);
    }

    public boolean hasAnyTag(TrackFilter filter) {
        if (filter.getTags().size() == 0) {
            return true;
        }

        for (TrackTag tag : filter.getTags()) {
            if (tags.contains(tag)){
                return true;
            }
        }
        return false;
    }

    public boolean hasAllTags(TrackFilter filter) {
        if (filter.getTags().size() == 0) {
            return false;
        }

        for (TrackTag tag : filter.getTags()) {
            if (!tags.contains(tag)){
                return false;
            }
        }
        return true;
    }


    public void addTag(TrackTag trackTag) {
        tags.add(trackTag);
    }

    public boolean createTag(TrackTag tag) {
        if (!tags.contains(tag)) {
            DB.executeUpdateAsync("INSERT INTO `ts_tracks_tags` (`trackId`, `tag`) VALUES(" + getId() + ", '" + tag.getValue() + "');");
            tags.add(tag);
            return true;
        }
        return false;
    }

    public boolean removeTag(TrackTag tag) {
        if (tags.contains(tag)) {
            DB.executeUpdateAsync("DELETE FROM `ts_tracks_tags` WHERE `tag` = '" + tag.getValue() + "' AND `trackId` = " + getId() + ";");
            tags.remove(tag);
            return true;
        }
        return false;
    }

    public List<TrackTag> getTags() {
        return tags.stream().sorted(Comparator.comparingInt(TrackTag::getWeight).reversed()).collect(Collectors.toList());
    }

    public List<TrackTag> getDisplayTags() {
        return tags.stream().filter(tag -> tag.getWeight() > 0).sorted(Comparator.comparingInt(TrackTag::getWeight).reversed()).collect(Collectors.toList());
    }


    public void setMode(TrackMode mode) {
        this.mode = mode;
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `mode` = " + Database.sqlString(mode.toString()) + " WHERE `id` = " + id + ";");
    }

    public void setBoatUtilsMode(BoatUtilsMode mode) {
        this.boatUtilsMode = mode;
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `boatUtilsMode` = " + Database.sqlString(mode.toString()) + " WHERE `id` = " + id + ";");
    }

    public void setName(String name) {
        this.displayName = name;
        this.commandName = name.replaceAll(" ", "");
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `name` = '" + name + "' WHERE `id` = " + id + ";");
    }

    public void setGuiItem(ItemStack guiItem) {
        this.guiItem = guiItem;
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `guiItem` = " + Database.sqlString(ApiUtilities.itemToString(guiItem)) + " WHERE `id` = " + id + ";");
    }

    public void setSpawnLocation(Location spawn) {
        this.spawnLocation = spawn;
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `spawn` = '" + ApiUtilities.locationToString(spawn) + "' WHERE `id` = " + id + ";");
    }

    public void setOpen(boolean open) {
        this.open = open;
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `toggleOpen` = " + open + " WHERE `id` = " + id + ";");
    }

    public void addRegion(TrackRegion trackRegion) {
        regions.add(trackRegion);
    }

    public boolean hasRegion(TrackRegion.RegionType regionType) {
        return regions.stream().anyMatch(trackRegion -> trackRegion.getRegionType().equals(regionType));
    }

    public boolean hasRegion(TrackRegion.RegionType regionType, int index) {
        return regions.stream().filter(trackRegion -> trackRegion.getRegionType().equals(regionType)).anyMatch(trackRegion -> trackRegion.getRegionIndex() == index);
    }

    public List<TrackRegion> getRegions(TrackRegion.RegionType regionType) {
        var list = regions.stream().filter(trackRegion -> trackRegion.getRegionType().equals(regionType)).collect(Collectors.toList());
        list.sort(Comparator.comparingInt(trackRegion -> trackRegion.getRegionIndex()));
        return list;
    }

    public Optional<TrackRegion> getRegion(TrackRegion.RegionType regionType) {
        return regions.stream().filter(trackRegion -> trackRegion.getRegionType().equals(regionType)).findFirst();
    }

    public Optional<TrackRegion> getRegion(TrackRegion.RegionType regionType, int index) {
        return regions.stream().filter(trackRegion -> trackRegion.getRegionType().equals(regionType)).filter(trackRegion -> trackRegion.getRegionIndex() == index).findFirst();
    }

    public List<TrackRegion> getCheckpointRegions(int index) {
        return regions.stream().filter(trackRegion -> trackRegion.getRegionType().equals(TrackRegion.RegionType.CHECKPOINT)).filter(trackRegion -> trackRegion.getRegionIndex() == index).toList();
    }

    public int getNumberOfCheckpoints() {
        var checkpoints = regions.stream().filter(trackRegion -> trackRegion.getRegionType().equals(TrackRegion.RegionType.CHECKPOINT)).toList();
        Set<Integer> count = new HashSet<>();
        for (TrackRegion r : checkpoints) {
            count.add(r.getRegionIndex());
        }
        return count.size();
    }

    public Optional<TrackRegion> getStartRegion() {
        return hasRegion(TrackRegion.RegionType.START, 1) ? getRegion(TrackRegion.RegionType.START, 1) : getRegion(TrackRegion.RegionType.START);

    }

    public boolean updateRegion(TrackRegion.RegionType regionType, Region selection, Location location) {
        var region = getRegion(regionType).get();
        return updateRegion(region, selection, location);
    }

    public boolean updateRegion(TrackRegion region, Region selection, Location location) {
        if (ApiUtilities.isRegionMatching(region, selection)) {
            region.setMaxP(ApiUtilities.getLocationFromBlockVector3(location.getWorld(), selection.getMaximumPoint()));
            region.setMinP(ApiUtilities.getLocationFromBlockVector3(location.getWorld(), selection.getMinimumPoint()));
            region.setSpawn(location);
            if (region instanceof TrackPolyRegion trackPolyRegion) {
                trackPolyRegion.updateRegion(((Polygonal2DRegion) selection).getPoints());
            }
            if (isTrackBoundaryChange(region.getRegionType())) {
                setDateChanged();
            }
        } else {
            removeRegion(region);
            return createRegion(region.getRegionType(), region.getRegionIndex(), selection, location);
        }
        return true;
    }

    public boolean createRegion(TrackRegion.RegionType regionType, Region selection, Location location) {
        return createRegion(regionType, 0, selection, location);
    }

    public boolean createRegion(TrackRegion.RegionType regionType, int index, Region selection, Location location) {
        try {
            var region = TrackDatabase.trackRegionNew(selection, getId(), index, regionType, location);
            addRegion(region);
            if (regionType.equals(TrackRegion.RegionType.START)) {
                TrackDatabase.addTrackRegion(region);
            }
            if (isTrackBoundaryChange(regionType)) {
                setDateChanged();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void setDateChanged() {
        dateChanged = ApiUtilities.getTimestamp();
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `dateChanged` = " + dateChanged + " WHERE `id` = " + getId() + ";");
    }

    private boolean isTrackBoundaryChange(TrackRegion.RegionType regionType) {
        if (regionType.equals(TrackRegion.RegionType.START)) {
            return true;
        } else if (regionType.equals(TrackRegion.RegionType.END)) {
            return true;
        } else return regionType.equals(TrackRegion.RegionType.CHECKPOINT);
    }

    public boolean removeRegion(TrackRegion region) {
        if (regions.contains(region)) {
            var regionId = region.getId();
            TrackDatabase.removeTrackRegion(region);
            regions.remove(region);
            DB.executeUpdateAsync("UPDATE `ts_regions` SET `isRemoved` = 1 WHERE `id` = " + regionId + ";");
            if (region instanceof TrackPolyRegion) {
                DB.executeUpdateAsync("DELETE FROM `ts_points` WHERE `regionId` = " + regionId + ";");
            }
            if (isTrackBoundaryChange(region.getRegionType())) {
                setDateChanged();
            }
            return true;
        }
        return false;
    }

    public void addTrackLocation(TrackLocation trackLocation) {
        trackLocations.add(trackLocation);
    }

    public Optional<Location> getFinishTpLocation() {
        if(trackLocations.stream().noneMatch(l -> l.getLocationType() == TrackLocation.Type.FINISH_TP_ALL)) return Optional.empty();
        return Optional.of(trackLocations.stream().filter(l -> l.getLocationType() == TrackLocation.Type.FINISH_TP_ALL).toList().get(0).getLocation());
    }

    public Optional<Location> getFinishTpLocation(int pos) {
        for(TrackLocation finishTp : trackLocations.stream().filter(l -> l.getLocationType() == TrackLocation.Type.FINISH_TP).toList()) {
            if(finishTp.getIndex() == pos)
                return Optional.of(finishTp.getLocation());
        }
        return Optional.empty();
    }

    public boolean hasTrackLocation(TrackLocation.Type locationType) {
        return trackLocations.stream().anyMatch(trackLocation -> trackLocation.getLocationType().equals(locationType));
    }

    public boolean hasTrackLocation(TrackLocation.Type locationType, int index) {
        return trackLocations.stream().filter(trackLocation -> trackLocation.getLocationType().equals(locationType)).anyMatch(trackLocation -> trackLocation.getIndex() == index);
    }

    public List<TrackLocation> getTrackLocations(TrackLocation.Type locationType) {
        var list = trackLocations.stream().filter(trackLocation -> trackLocation.getLocationType().equals(locationType)).collect(Collectors.toList());
        list.sort(Comparator.comparingInt(trackLocation -> trackLocation.getIndex()));
        return list;
    }

    public Optional<TrackLocation> getTrackLocation(TrackLocation.Type locationType) {
        return trackLocations.stream().filter(trackLocation -> trackLocation.getLocationType().equals(locationType)).findFirst();
    }

    public Optional<TrackLocation> getTrackLocation(TrackLocation.Type locationType, int index) {
        return trackLocations.stream().filter(trackLocation -> trackLocation.getLocationType().equals(locationType)).filter(trackLocation -> trackLocation.getIndex() == index).findFirst();
    }

    public void updateTrackLocation(TrackLocation trackLocation, Location location) {
        trackLocation.updateLocation(location);
        if (trackLocation instanceof TrackLeaderboard trackLeaderboard) {
            trackLeaderboard.createOrUpdateHologram();
        }
    }

    public boolean createTrackLocation(TrackLocation.Type type, Location location) {
        return createTrackLocation(type, 0, location);
    }

    public boolean createTrackLocation(TrackLocation.Type type, int index, Location location) {
        try {
            var trackLocation = TrackDatabase.trackLocationNew(getId(), index, type, location);
            addTrackLocation(trackLocation);
            if (trackLocation instanceof TrackLeaderboard trackLeaderboard) {
                trackLeaderboard.createOrUpdateHologram();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean removeTrackLocation(TrackLocation trackLocation) {
        if (trackLocations.contains(trackLocation)) {
            if (trackLocation instanceof TrackLeaderboard trackLeaderboard) {
                trackLeaderboard.removeHologram();
            }
            trackLocations.remove(trackLocation);
            DB.executeUpdateAsync("DELETE FROM `ts_locations` WHERE `trackId` = " + getId() + " AND `index` = " + trackLocation.getIndex() + " AND `type` = '" + trackLocation.getLocationType() + "';");
            return true;
        }
        return false;
    }

    public void addTimeTrialFinish(TimeTrialFinish timeTrialFinish) {
        if (timeTrialFinishes.get(timeTrialFinish.getPlayer()) == null) {
            List<TimeTrialFinish> list = new ArrayList<>();
            list.add(timeTrialFinish);
            timeTrialFinishes.put(timeTrialFinish.getPlayer(), list);
            return;
        }
        if (timeTrialFinishes.get(timeTrialFinish.getPlayer()).contains(timeTrialFinish)) {
            return;
        }
        timeTrialFinishes.get(timeTrialFinish.getPlayer()).add(timeTrialFinish);
    }

    public TimeTrialFinish newTimeTrialFinish(long time, UUID uuid) {
        try {
            long date = ApiUtilities.getTimestamp();
            var finishId = DB.executeInsert("INSERT INTO `ts_finishes` (`trackId`, `uuid`, `date`, `time`, `isRemoved`) VALUES(" + id + ", '" + uuid + "', " + date + ", " + time + ", 0);");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_finishes` WHERE `id` = " + finishId + ";");
            TimeTrialFinish timeTrialFinish = new TimeTrialFinish(dbRow);

            addTimeTrialFinish(timeTrialFinish);
            return timeTrialFinish;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public void addTimeTrialAttempt(TimeTrialAttempt timeTrialAttempt) {
        if (timeTrialAttempts.get(timeTrialAttempt.getPlayer()) == null) {
            List<TimeTrialAttempt> list = new ArrayList<>();
            list.add(timeTrialAttempt);
            timeTrialAttempts.put(timeTrialAttempt.getPlayer(), list);
            return;
        }
        timeTrialAttempts.get(timeTrialAttempt.getPlayer()).add(timeTrialAttempt);
    }

    public TimeTrialAttempt newTimeTrialAttempt(long time, UUID uuid) {
        long date = ApiUtilities.getTimestamp();
        DB.executeUpdateAsync("INSERT INTO `ts_attempts` (`trackId`, `uuid`, `date`, `time`) VALUES(" + id + ", '" + uuid + "', " + date + ", " + time + ");");
        TimeTrialAttempt timeTrialAttempt = new TimeTrialAttempt(getId(), uuid, ApiUtilities.getTimestamp(), time);
        addTimeTrialAttempt(timeTrialAttempt);
        return timeTrialAttempt;
    }

    public TimeTrialFinish getBestFinish(TPlayer player) {
        if (timeTrialFinishes.get(player) == null) {
            return null;
        }
        List<TimeTrialFinish> ttTimes = new ArrayList<>();
        var times = timeTrialFinishes.get(player);
        ttTimes.addAll(times);
        if (ttTimes.isEmpty()) {
            return null;
        }

        ttTimes.sort(new TimeTrialFinishComparator());
        return ttTimes.get(0);
    }

    public boolean hasPlayedTrack(TPlayer tPlayer) {
        return timeTrialFinishes.containsKey(tPlayer) || timeTrialAttempts.containsKey(tPlayer);
    }

    public void deleteBestFinish(TPlayer player, TimeTrialFinish bestFinish) {
        timeTrialFinishes.get(player).remove(bestFinish);
        DB.executeUpdateAsync("UPDATE `ts_finishes` SET `isRemoved` = 1 WHERE `id` = " + bestFinish.getId() + ";");
    }

    public void deleteAllFinishes(TPlayer player) {
        timeTrialFinishes.remove(player);
        DB.executeUpdateAsync("UPDATE `ts_finishes` SET `isRemoved` = 1 WHERE `trackId` = " + getId() + " AND `uuid` = '" + player.getUniqueId() + "';");
    }

    public void deleteAllFinishes() {
        timeTrialFinishes = new HashMap<>();
        DB.executeUpdateAsync("UPDATE `ts_finishes` SET `isRemoved` = 1 WHERE `trackId` = " + getId() + ";");
    }

    public Integer getPlayerTopListPosition(TPlayer TPlayer) {
        var topList = getTopList(-1);
        for (int i = 0; i < topList.size(); i++) {
            if (topList.get(i).getPlayer().equals(TPlayer)) {
                return ++i;
            }
        }
        return -1;
    }

    public Integer getCachedPlayerPosition(TPlayer tPlayer) {
        int pos = cachedPositions.indexOf(tPlayer);
        if (pos != -1) {
            pos++;
        }
        return pos;
    }

    public List<TimeTrialFinish> getTopList(int limit) {

        List<TimeTrialFinish> bestTimes = new ArrayList<>();
        for (TPlayer player : timeTrialFinishes.keySet()) {
            TimeTrialFinish bestFinish = getBestFinish(player);
            if (bestFinish != null) {
                bestTimes.add(bestFinish);
            }
        }
        bestTimes.sort(new TimeTrialFinishComparator());
        cachedPositions = new ArrayList<>();
        bestTimes.forEach(timeTrialFinish -> cachedPositions.add(timeTrialFinish.getPlayer()));

        if (limit == -1) {
            return bestTimes;
        }

        return bestTimes.stream().limit(limit).collect(Collectors.toList());
    }

    public List<TimeTrialFinish> getTopList() {
        return getTopList(-1);
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
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `type` = " + Database.sqlString(type.toString()) + " WHERE `id` = " + id + ";");
    }

    public void setOwner(TPlayer owner) {
        this.owner = owner;
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `uuid` = '" + owner.getUniqueId() + "' WHERE `id` = " + id + ";");
    }

    public void setOptions(String options) {
        this.options = options.toCharArray();
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `options` = " + Database.sqlString(options) + " WHERE `id` = " + id + ";");

    }

    public void addContributor(TPlayer tPlayer) {
        if(contributors.contains(tPlayer)) return;
        contributors.add(tPlayer);
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `contributors` = " + Database.sqlString(ApiUtilities.uuidListToString(ApiUtilities.uuidListFromTPlayersList(contributors))) + " WHERE `id` = " + id + ";");
    }

    public void removeContributor(TPlayer tPlayer) {
        contributors.remove(tPlayer);
        String uuids = ApiUtilities.uuidListToString(ApiUtilities.uuidListFromTPlayersList(contributors));
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `contributors` = " + Database.sqlString(uuids.isEmpty() ? null : uuids) + " WHERE `id` = " + id + ";");
    }

    public String getContributorsAsString() {
        if(contributors.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(contributors.get(0).getName());
        List<TPlayer> rest = List.copyOf(contributors).subList(1, contributors.size());
        rest.forEach(tp -> sb.append(", ").append(tp.getName()));
        return sb.toString();
    }

    public boolean hasOption(char needle) {
        if (this.options == null) {
            return false;
        }

        for (char option : this.options) {
            if (option == needle) {
                return true;
            }
        }
        return false;
    }

    public int getPlayerTotalFinishes(TPlayer tPlayer) {
        if (!timeTrialFinishes.containsKey(tPlayer)) {
            return 0;
        }
        return timeTrialFinishes.get(tPlayer).size();
    }

    public int getPlayerTotalAttempts(TPlayer tPlayer) {
        if (!timeTrialAttempts.containsKey(tPlayer)) {
            return 0;
        }
        return timeTrialAttempts.get(tPlayer).size();
    }

    public int getTotalFinishes() {
        int laps = 0;
        for (List<TimeTrialFinish> l : timeTrialFinishes.values()) {
            laps += l.size();
        }
        return laps;

    }

    public int getTotalAttempts() {
        int laps = 0;
        for (List<TimeTrialAttempt> l : timeTrialAttempts.values()) {
            laps += l.size();
        }
        return laps;
    }

    public long getPlayerTotalTimeSpent(TPlayer tPlayer) {
        long time = 0L;

        if (timeTrialAttempts.containsKey(tPlayer)) {
            for (TimeTrialAttempt l : timeTrialAttempts.get(tPlayer)) {
                time += l.getTime();
            }
        }
        if (timeTrialFinishes.containsKey(tPlayer)) {
            for (TimeTrialFinish l : timeTrialFinishes.get(tPlayer)) {
                time += l.getTime();
            }
        }
        return time;
    }

    public long getTotalTimeSpent() {
        return totalTimeSpent;
    }

    public void setTotalTimeSpent(long time) {
        totalTimeSpent = time;
    }

    public boolean isStage() {
        return hasRegion(TrackRegion.RegionType.END);
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
