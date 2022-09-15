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
import me.makkuusen.timing.system.gui.ItemBuilder;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.timetrial.TimeTrialFinishComparator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
    private final Map<Integer, Location> grids = new HashMap<>();
    private Map<TPlayer, List<TimeTrialFinish>> timeTrialFinishes = new HashMap<>();
    private TPlayer owner;
    private String displayName;
    private String commandName;
    private ItemStack guiItem;
    private Location spawnLocation;
    private Location leaderboardLocation;
    private TrackType type;
    private TrackMode mode;
    private char[] options;
    private boolean open;

    public Track(DbRow data) {
        id = data.getInt("id");
        owner = data.getString("uuid") == null ? null : Database.getPlayer(UUID.fromString(data.getString("uuid")));
        displayName = data.getString("name");
        commandName = displayName.replaceAll(" ", "");
        dateCreated = data.getInt("dateCreated");
        guiItem = ApiUtilities.stringToItem(data.getString("guiItem"));
        spawnLocation = ApiUtilities.stringToLocation(data.getString("spawn"));
        leaderboardLocation = ApiUtilities.stringToLocation(data.getString("leaderboard"));
        type = data.getString("type") == null ? TrackType.BOAT : TrackType.valueOf(data.getString("type"));
        open = data.get("toggleOpen");
        options = data.getString("options") == null ? new char[0] : data.getString("options").toCharArray();
        mode = data.get("mode") == null ? TrackMode.TIMETRIAL : TrackMode.valueOf(data.getString("mode"));

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
        TPlayer TPlayer = Database.getPlayer(uuid);

        List<Component> loreToSet = new ArrayList<>();

        String bestTime;

        if (getBestFinish(TPlayer) == null) {
            bestTime = "§7Your best time: §e(none)";
        } else {
            bestTime = "§7Your best time: §e" + ApiUtilities.formatAsTime(getBestFinish(TPlayer).getTime());
        }

        loreToSet.add(Component.text("§7Your position: §e" + (getPlayerTopListPosition(TPlayer) == -1 ? "(none)" : getPlayerTopListPosition(TPlayer))));
        loreToSet.add(Component.text(bestTime));
        loreToSet.add(Component.text("§7Created by: §e" + getOwner().getName()));
        loreToSet.add(Component.text("§7Type: §e" + getTypeAsString()));

        ItemMeta im = toReturn.getItemMeta();
        im.displayName(Component.text(getDisplayName()).color(TextColor.color(255, 255, 85)));
        im.lore(loreToSet);
        toReturn.setItemMeta(im);

        return toReturn;
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

    public void setLeaderboardLocation(Location leaderboard) {
        this.leaderboardLocation = leaderboard;
        DB.executeUpdateAsync("UPDATE `ts_tracks` SET `leaderboard` = '" + ApiUtilities.locationToString(leaderboard) + "' WHERE `id` = " + id + ";");
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

    public void setGridLocation(Location loc, int index) {
        if (grids.containsKey(index)) {
            grids.put(index, loc);
            DB.executeUpdateAsync("UPDATE `ts_locations` SET `location` = '" + ApiUtilities.locationToString(loc) + "' WHERE `trackId` = " + getId() + " AND `index` = " + index + " AND `type` = 'GRID';");
        } else {
            try {
                DB.executeInsert("INSERT INTO `ts_locations` (`trackId`, `index`, `type`, `location`) VALUES(" + getId() +  ", "  + index + ", 'GRID', '" + ApiUtilities.locationToString(loc) + "');");
                grids.put(index, loc);
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
    }

    public void addGridLocation(Location location, int index) {
        grids.put(index, location);
    }

    public boolean removeGridLocation(int index) {
        if (grids.containsKey(index)) {
            grids.remove(index);
            DB.executeUpdateAsync("DELETE FROM `ts_locations` WHERE `trackId` = " + getId() + " AND `index` = " + index + " AND `type` = 'GRID';");
            return true;
        }
        return false;
    }

    public Map<Integer, Location> getGridLocations() {
        return grids;
    }

    public Location getGridLocation(int index){
        return grids.get(index);
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

    public void newTimeTrialFinish(long time, UUID uuid) {
        try {

            long date = ApiUtilities.getTimestamp();
            var finishId = DB.executeInsert("INSERT INTO `ts_finishes` (`trackId`, `uuid`, `date`, `time`, `isRemoved`) VALUES(" + id + ", '" + uuid + "', " + date + ", " + time + ", 0);");
            var dbRow = DB.getFirstRow("SELECT * FROM `ts_finishes` WHERE `id` = " + finishId + ";");

            TimeTrialFinish timeTrialFinish = new TimeTrialFinish(dbRow);
            addTimeTrialFinish(timeTrialFinish);

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public TimeTrialFinish getBestFinish(TPlayer player) {
        if (timeTrialFinishes.get(player) == null) {
            return null;
        }
        var times = timeTrialFinishes.get(player);
        if (times.isEmpty()) {
            return null;
        }
        times.sort(new TimeTrialFinishComparator());
        return times.get(0);
    }

    public void deleteBestFinish(TPlayer player, TimeTrialFinish bestFinish) {
        try {
            timeTrialFinishes.get(player).remove(bestFinish);
            DB.executeUpdate("UPDATE `ts_finishes` SET `isRemoved` = 1 WHERE `id` = " + bestFinish.getId() + ";");

            var dbRows = DB.getResults("SELECT * FROM `ts_finishes` WHERE (`uuid`,`time`) IN (SELECT `uuid`, min(`time`) FROM `ts_finishes` WHERE `trackId` = " + id + " AND `uuid` = '" + player.getUniqueId() + "' AND `isRemoved` = 0 GROUP BY `uuid`) AND `isRemoved` = 0 ORDER BY `time`;");
            for (DbRow dbRow : dbRows) {
                var rf = new TimeTrialFinish(dbRow);
                if (timeTrialFinishes.get(player).stream().noneMatch(timeTrialFinish -> timeTrialFinish.equals(rf))) {
                    addTimeTrialFinish(rf);
                }
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public boolean deleteAllFinishes() {
        try {
            DB.executeUpdate("UPDATE `ts_finishes` SET `isRemoved` = 1 WHERE `trackId` = " + getId() + ";");
            timeTrialFinishes = new HashMap<>();
            return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
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

    public enum TrackType {
        BOAT, ELYTRA, PARKOUR
    }

    public enum TrackMode {
        TIMETRIAL, RACE
    }
}
