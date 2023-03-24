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
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.api.events.TimeTrialFinishEvent;
import me.makkuusen.timing.system.timetrial.TimeTrialAttempt;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.timetrial.TimeTrialFinishComparator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.stringtemplate.v4.ST;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class Track {
    private final int id;
    private final long dateCreated;
    private final Set<TrackRegion> regions = new HashSet<>();
    private final Set<TrackLocation> trackLocations = new HashSet<>();
    private Map<TPlayer, List<TimeTrialFinish>> timeTrialFinishes = new HashMap<>();
    private Map<TPlayer, List<TimeTrialAttempt>> timeTrialAttempts = new HashMap<>();
    private TPlayer owner;
    private String displayName;
    private String commandName;
    private ItemStack guiItem;
    private Location spawnLocation;
    private TrackType type;
    private TrackMode mode;
    private int weight;
    private char[] options;
    private boolean open;

    public Track(DbRow data) {
        id = data.getInt("id");
        owner = data.getString("uuid") == null ? null : Database.getPlayer(UUID.fromString(data.getString("uuid")));
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

    }

    public static ContextResolver<TrackType, BukkitCommandExecutionContext> getTrackTypeContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return TrackType.valueOf(name);
            } catch (IllegalArgumentException e) {
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    public static ContextResolver<TrackMode, BukkitCommandExecutionContext> getTrackModeContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return TrackMode.valueOf(name);
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

        String bestTime;

        if (getBestFinish(tPlayer) == null) {
            bestTime = "§7Your best time: §e(none)";
        } else {
            bestTime = "§7Your best time: §e" + ApiUtilities.formatAsTime(getBestFinish(tPlayer).getTime());
        }

        loreToSet.add(Component.text("§7Position: §e" + (getPlayerTopListPosition(tPlayer) == -1 ? "(none)" : getPlayerTopListPosition(tPlayer))));
        loreToSet.add(Component.text(bestTime));
        loreToSet.add(Component.text("§7Total Finishes: §e" + getPlayerTotalFinishes(tPlayer)));
        loreToSet.add(Component.text("§7Total Attempts: §e" + (getPlayerTotalFinishes(tPlayer) + getPlayerTotalAttempts(tPlayer))));
        loreToSet.add(Component.text("§7Time spent: §e" + ApiUtilities.formatAsTimeSpent(getPlayerTotalTimeSpent(tPlayer))));
        loreToSet.add(Component.text("§7Created by: §e" + getOwner().getName()));

        
        ItemMeta im = toReturn.getItemMeta();
        im.displayName(Component.text(getDisplayName()).color(TextColor.color(255, 255, 85)));
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


    public void setMode(TrackMode mode) {
        this.mode = mode;
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `mode` = " + Database.sqlString(mode.toString()) + " WHERE `id` = " + id + ";");
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

    public void addRegion(TrackRegion trackRegion){
        regions.add(trackRegion);
    }

    public boolean hasRegion(TrackRegion.RegionType regionType){
        return regions.stream().anyMatch(trackRegion -> trackRegion.getRegionType().equals(regionType));
    }

    public boolean hasRegion(TrackRegion.RegionType regionType, int index){
        return regions.stream().filter(trackRegion -> trackRegion.getRegionType().equals(regionType)).anyMatch(trackRegion -> trackRegion.getRegionIndex() == index);
    }

    public List<TrackRegion> getRegions(TrackRegion.RegionType regionType) {
        return regions.stream().filter(trackRegion -> trackRegion.getRegionType().equals(regionType)).collect(Collectors.toList());
    }

    public Optional<TrackRegion> getRegion(TrackRegion.RegionType regionType){
        return regions.stream().filter(trackRegion -> trackRegion.getRegionType().equals(regionType)).findFirst();
    }

    public Optional<TrackRegion> getRegion(TrackRegion.RegionType regionType, int index){
        return regions.stream().filter(trackRegion -> trackRegion.getRegionType().equals(regionType)).filter(trackRegion -> trackRegion.getRegionIndex() == index).findFirst();
    }

    public boolean updateRegion(TrackRegion.RegionType regionType, Region selection, Location location) {
        var startRegion = getRegion(regionType).get();
        return updateRegion(startRegion, selection, location);
    }

    public boolean updateRegion(TrackRegion region, Region selection, Location location) {
        if (ApiUtilities.isRegionMatching(region, selection)) {
            region.setMaxP(ApiUtilities.getLocationFromBlockVector3(location.getWorld(), selection.getMaximumPoint()));
            region.setMinP(ApiUtilities.getLocationFromBlockVector3(location.getWorld(), selection.getMinimumPoint()));
            region.setSpawn(location);
            if (region instanceof TrackPolyRegion trackPolyRegion) {
                trackPolyRegion.updateRegion(((Polygonal2DRegion)selection).getPoints());
            }
        } else {
            removeRegion(region);
            return createRegion(region.getRegionType(), region.getRegionIndex(), selection, location);
        }
        return true;
    }

    public boolean createRegion(TrackRegion.RegionType regionType, Region selection, Location location){
        return createRegion(regionType, 0, selection,  location);
    }

    public boolean createRegion(TrackRegion.RegionType regionType, int index, Region selection, Location location){
        try {
            var region = TrackDatabase.trackRegionNew(selection, getId(), index, regionType, location);
            addRegion(region);
            if (regionType.equals(TrackRegion.RegionType.START)) {
                TrackDatabase.addTrackRegion(region);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
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
            return true;
        }
        return false;
    }

    public void addTrackLocation(TrackLocation trackLocation){
        trackLocations.add(trackLocation);
    }

    public boolean hasTrackLocation(TrackLocation.Type locationType){
        return trackLocations.stream().anyMatch(trackLocation -> trackLocation.getLocationType().equals(locationType));
    }

    public boolean hasTrackLocation(TrackLocation.Type locationType, int index) {
        return trackLocations.stream().filter(trackLocation -> trackLocation.getLocationType().equals(locationType)).anyMatch(trackLocation -> trackLocation.getIndex() == index);
    }

    public List<TrackLocation> getTrackLocations(TrackLocation.Type locationType) {
        return trackLocations.stream().filter(trackLocation -> trackLocation.getLocationType().equals(locationType)).collect(Collectors.toList());
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

    public boolean createTrackLocation(TrackLocation.Type type, Location location){
        return createTrackLocation(type, 0, location);
    }

    public boolean createTrackLocation(TrackLocation.Type type, int index, Location location){
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
            DB.executeUpdateAsync("DELETE FROM `ts_locations` WHERE `trackId` = " + getId() + " AND `index` = " + trackLocation.getIndex() + " AND `type` = '" +  trackLocation.getLocationType() + "';");
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

    public List<TimeTrialFinish> getTopList(int limit) {

        List<TimeTrialFinish> bestTimes = new ArrayList<>();
        for (TPlayer player : timeTrialFinishes.keySet()) {
            TimeTrialFinish bestFinish = getBestFinish(player);
            if (bestFinish != null) {
                bestTimes.add(bestFinish);
            }
        }
        bestTimes.sort(new TimeTrialFinishComparator());

        if (limit == -1) {
            return bestTimes;
        }

        return bestTimes.stream().limit(limit).collect(Collectors.toList());
    }

    public List<TimeTrialFinish> getTopList() {
        List<TimeTrialFinish> bestTimes = new ArrayList<>();
        for (TPlayer player : timeTrialFinishes.keySet()) {
            TimeTrialFinish bestFinish = getBestFinish(player);
            if (bestFinish != null) {
                bestTimes.add(bestFinish);
            }
        }
        bestTimes.sort(new TimeTrialFinishComparator());

        return bestTimes;
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

    public int getPlayerTotalFinishes(TPlayer tPlayer){
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

    public int getTotalFinishes(){
        int laps = 0;
        for (List<TimeTrialFinish> l : timeTrialFinishes.values()) {
            laps += l.size();
        }
        return laps;

    }

    public int getTotalAttempts(){
        int laps = 0;
        for (List<TimeTrialAttempt> l : timeTrialAttempts.values()) {
            laps += l.size();
        }
        return laps;
    }

    public long getPlayerTotalTimeSpent(TPlayer tPlayer) {
        long time = 0L;

        if (timeTrialAttempts.containsKey(tPlayer)){
            for (TimeTrialAttempt l : timeTrialAttempts.get(tPlayer)) {
                time += l.getTime();
            }
        }
        if (timeTrialFinishes.containsKey(tPlayer)){
            for (TimeTrialFinish l : timeTrialFinishes.get(tPlayer)) {
                time += l.getTime();
            }
        }
        return time;
    }

    public long getTotalTimeSpent() {
        long time = 0L;
        long bestTime = 0L;
        var topTime = getTopList(1);
        if (topTime.size() != 0) {
            bestTime = topTime.get(0).getTime();

            for (List<TimeTrialFinish> l : timeTrialFinishes.values()) {
                for (TimeTrialFinish ttf : l) {
                    if (ttf.getTime() < (bestTime * 4)) {
                        time += ttf.getTime();
                    }
                }
            }
        }

        for (List<TimeTrialAttempt> l : timeTrialAttempts.values()) {
            for (TimeTrialAttempt ttf : l) {
                if (bestTime != 0) {
                    if (ttf.getTime() < (bestTime * 4)) {
                        time += ttf.getTime();
                    }
                } else {
                    time += ttf.getTime();
                }

            }
        }

        return time;
    }

    public boolean isStage(){
        return hasRegion(TrackRegion.RegionType.END);
    }

    public enum TrackType {
        BOAT, ELYTRA, PARKOUR
    }

    public enum TrackMode {
        TIMETRIAL, RACE
    }
}
