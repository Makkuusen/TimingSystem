package me.makkuusen.timing.system.track;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.DatabaseTrack;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.gui.ItemBuilder;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.timetrial.TimeTrialFinishComparator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Track {
    private final int id;
    private TPlayer owner;
    private String name;
    private final long dateCreated;
    private ItemStack guiItem;
    private Location spawnLocation;
    private Location leaderboardLocation;
    private TrackType type;
    private TrackMode mode;
    private char[] options;
    private boolean toggleOpen;
    private boolean toggleGovernment;
    private TrackRegion startRegion;
    private TrackRegion endRegion;
    private TrackRegion pitRegion;
    private final Map<Integer, TrackRegion> checkpoints = new HashMap<>();
    private final Map<Integer, TrackRegion> resetRegions = new HashMap<>();
    private final Map<Integer, TrackRegion> gridRegions = new HashMap<>();
    private final Map<TPlayer, List<TimeTrialFinish>> timeTrialFinishes = new HashMap<>();

    public enum TrackType {
        BOAT, ELYTRA, PARKOUR
    }

    public enum TrackMode {
        TIMETRIAL, RACE
    }

    public Track(DbRow data) {
        id = data.getInt("id");
        owner = data.getString("uuid") == null ? null : Database.getPlayer(UUID.fromString(data.getString("uuid")));
        name = data.getString("name");
        dateCreated = data.getInt("dateCreated");
        guiItem = ApiUtilities.stringToItem(data.getString("guiItem"));
        spawnLocation = ApiUtilities.stringToLocation(data.getString("spawn"));
        leaderboardLocation = ApiUtilities.stringToLocation(data.getString("leaderboard"));
        type = data.getString("type") == null ? TrackType.BOAT : TrackType.valueOf(data.getString("type"));
        toggleOpen = data.get("toggleOpen");
        toggleGovernment = data.get("toggleGovernment");
        options = data.getString("options") == null ? new char[0] : data.getString("options").toCharArray();

        mode = data.get("mode") == null ? TrackMode.TIMETRIAL : TrackMode.valueOf(data.getString("mode"));

    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TrackRegion getEndRegion() {
        return endRegion;
    }

    public TrackRegion getStartRegion() {
        return startRegion;
    }

    public TrackRegion getPitRegion() {
        return pitRegion;
    }

    public ItemStack getGuiItem(UUID uuid) {
        ItemStack toReturn;
        if (guiItem == null) {
            if (isBoatTrack()) {
                toReturn = new ItemBuilder(Material.PACKED_ICE).setName(getName()).build();
            } else if (isElytraTrack()) {
                toReturn = new ItemBuilder(Material.ELYTRA).setName(getName()).build();
            } else {
                toReturn = new ItemBuilder(Material.BIG_DRIPLEAF).setName(getName()).build();
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
        im.displayName(Component.text(getName()).color(TextColor.color(255, 255, 85)));
        im.lore(loreToSet);
        toReturn.setItemMeta(im);

        return toReturn;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public Location getLeaderboardLocation() {
        return leaderboardLocation;
    }

    public TrackType getType() {
        return type;
    }

    public TrackMode getMode() {
        return mode;
    }

    public boolean isOpen() {
        return toggleOpen;
    }

    public void setMode(TrackMode mode) {
        this.mode = mode;

        DB.executeUpdateAsync("UPDATE `tracks` SET `mode` = " + Database.sqlString(mode.toString()) + " WHERE `id` = " + id + ";");
    }

    public void setName(String name) {
        this.name = name;

        DB.executeUpdateAsync("UPDATE `tracks` SET `name` = '" + name + "' WHERE `id` = " + id + ";");
    }

    public void setGuiItem(ItemStack guiItem) {
        this.guiItem = guiItem;

        DB.executeUpdateAsync("UPDATE `tracks` SET `guiItem` = " + Database.sqlString(ApiUtilities.itemToString(guiItem)) + " WHERE `id` = " + id + ";");
    }

    public void setSpawnLocation(Location spawn) {
        this.spawnLocation = spawn;

        DB.executeUpdateAsync("UPDATE `tracks` SET `spawn` = '" + ApiUtilities.locationToString(spawn) + "' WHERE `id` = " + id + ";");
    }

    public void setLeaderboardLocation(Location leaderboard) {
        this.leaderboardLocation = leaderboard;

        DB.executeUpdateAsync("UPDATE `tracks` SET `leaderboard` = '" + ApiUtilities.locationToString(leaderboard) + "' WHERE `id` = " + id + ";");
    }

    public void setToggleOpen(boolean toggleOpen) {
        this.toggleOpen = toggleOpen;

        DB.executeUpdateAsync("UPDATE `tracks` SET `toggleOpen` = " + toggleOpen + " WHERE `id` = " + id + ";");

    }

    public void setStartRegion(Location minP, Location maxP) {
        startRegion.setMinP(minP);
        startRegion.setMaxP(maxP);

        DB.executeUpdateAsync("UPDATE `ts_regions` SET `minP` = '" + ApiUtilities.locationToString(minP) + "', `maxP` = '" + ApiUtilities.locationToString(maxP) + "' WHERE `id` = " + startRegion.getId() + ";");
    }

    public void setEndRegion(Location minP, Location maxP) {
        endRegion.setMinP(minP);
        endRegion.setMaxP(maxP);

        DB.executeUpdateAsync("UPDATE `ts_regions` SET `minP` = '" + ApiUtilities.locationToString(minP) + "', `maxP` = '" + ApiUtilities.locationToString(maxP) + "' WHERE `id` = " + endRegion.getId() + ";");
    }

    public void setPitRegion(Location minP, Location maxP, Location spawn) {
        if (pitRegion == null) {

            try {
                var regionId = DB.executeInsert("INSERT INTO `ts_regions` (`trackId`, `regionIndex`, `regionType`, `minP`, `maxP`, `spawn`, `isRemoved`) VALUES(" + id + ", 0, " + Database.sqlString(TrackRegion.RegionType.PIT.toString()) + ", '" + ApiUtilities.locationToString(minP) + "', '" + ApiUtilities.locationToString(maxP) + "', '" + ApiUtilities.locationToString(spawn) + "', 0);");

                var dbRow = DB.getFirstRow("SELECT * FROM `ts_regions` WHERE `id` = " + regionId + ";");
                TrackRegion pitRegion = new TrackRegion(dbRow);
                newPitRegion(pitRegion);
                return;
            } catch (SQLException exception) {
                exception.printStackTrace();
                return;
            }
        }

        pitRegion.setMinP(minP);
        pitRegion.setMaxP(maxP);

        DB.executeUpdateAsync("UPDATE `ts_regions` SET `minP` = '" + ApiUtilities.locationToString(minP) + "', `maxP` = '" + ApiUtilities.locationToString(maxP) + "' WHERE `id` = " + pitRegion.getId() + ";");
    }


    public void newStartRegion(TrackRegion region) {
        this.startRegion = region;
    }

    public void newEndRegion(TrackRegion region) {
        this.endRegion = region;
    }

    public void newPitRegion(TrackRegion region) {
        this.pitRegion = region;
    }

    public Map<Integer, TrackRegion> getCheckpoints() {
        return checkpoints;
    }

    public void addCheckpoint(TrackRegion region) {
        checkpoints.put(region.getRegionIndex(), region);
    }

    public void setCheckpoint(Location minP, Location maxP, Location spawn, int index) {
        setTrackRegions(checkpoints, TrackRegion.RegionType.CHECKPOINT, minP, maxP, spawn, index);
    }

    public boolean removeCheckpoint(int index) {
        return removeTrackRegions(checkpoints, index);
    }

    public void setResetRegion(Location minP, Location maxP, Location spawn, int index) {
        setTrackRegions(resetRegions, TrackRegion.RegionType.RESET, minP, maxP, spawn, index);
    }

    public boolean removeResetRegion(int index) {
        return removeTrackRegions(resetRegions, index);
    }

    public void addResetRegion(TrackRegion region) {
        resetRegions.put(region.getRegionIndex(), region);
    }

    public Map<Integer, TrackRegion> getResetRegions() {
        return resetRegions;
    }

    public void setGridRegion(Location minP, Location maxP, Location spawn, int index) {
        setTrackRegions(gridRegions, TrackRegion.RegionType.GRID, minP, maxP, spawn, index);
    }

    public boolean removeGridRegion(int index) {
        return removeTrackRegions(gridRegions, index);
    }

    public void addGridRegion(TrackRegion region) {
        gridRegions.put(region.getRegionIndex(), region);
    }

    public Map<Integer, TrackRegion> getGridRegions() {
        return gridRegions;
    }


    private void setTrackRegions(Map<Integer, TrackRegion> map, TrackRegion.RegionType regionType, Location minP, Location maxP, Location spawn, int index) {

        if (map.containsKey(index)) {
            // Modify checkpoint
            TrackRegion region = map.get(index);
            region.setMinP(minP);
            region.setMaxP(maxP);
            region.setSpawnLocation(spawn);

            DB.executeUpdateAsync("UPDATE `ts_regions` SET `minP` = '" + ApiUtilities.locationToString(minP) + "', `maxP` = '" + ApiUtilities.locationToString(maxP) + "', `spawn` = '" + ApiUtilities.locationToString(spawn) + "' WHERE `id` = " + region.getId() + ";");

        } else {
            try {

                var regionId = DB.executeInsert("INSERT INTO `ts_regions` (`trackId`, `regionIndex`, `regionType`, `minP`, `maxP`, `spawn`, `isRemoved`) VALUES(" + id + ", " + index + ", " + Database.sqlString(regionType.toString()) + ", '" + ApiUtilities.locationToString(minP) + "', '" + ApiUtilities.locationToString(maxP) + "', '" + ApiUtilities.locationToString(spawn) + "', 0);");
                var dbRow = DB.getFirstRow("SELECT * FROM `ts_regions` WHERE `id` = " + regionId + ";");

                TrackRegion region = new TrackRegion(dbRow);
                if (regionType.equals(TrackRegion.RegionType.CHECKPOINT)) {
                    addCheckpoint(region);
                } else if (regionType.equals(TrackRegion.RegionType.GRID)) {
                    addGridRegion(region);
                } else if (regionType.equals(TrackRegion.RegionType.RESET)) {
                    addResetRegion(region);
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
    }

    private boolean removeTrackRegions(Map<Integer, TrackRegion> map, int index) {
        if (map.containsKey(index)) {
            var gridRegion = map.get(index);
            var gridRegionId = gridRegion.getId();
            DatabaseTrack.removeTrackRegion(gridRegion);
            map.remove(index);

            DB.executeUpdateAsync("UPDATE `ts_regions` SET `isRemoved` = 1 WHERE `id` = " + gridRegionId + ";");
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

    public void teleportPlayer(Player player) {
        player.teleport(spawnLocation);
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public boolean isGovernment() {
        return toggleGovernment;
    }

    public boolean isPersonal() {
        return !toggleGovernment;
    }

    public void setToggleGovernment(boolean toggleGovernment) {
        this.toggleGovernment = toggleGovernment;

        DB.executeUpdateAsync("UPDATE `tracks` SET `toggleGovernment` = " + toggleGovernment + " WHERE `id` = " + id + ";");
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

    public TrackType getTypeFromString(String type) {
        if (type.equalsIgnoreCase("parkour")) {
            return Track.TrackType.PARKOUR;
        } else if (type.equalsIgnoreCase("elytra")) {
            return Track.TrackType.ELYTRA;
        } else if (type.equalsIgnoreCase("boat")) {
            return Track.TrackType.BOAT;
        }
        return null;
    }

    public TrackMode getModeFromString(String mode) {
        if (mode.equalsIgnoreCase("race")) {
            return TrackMode.RACE;
        } else if (mode.equalsIgnoreCase("timetrial")) {
            return TrackMode.TIMETRIAL;
        } else
            return null;
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

        DB.executeUpdateAsync("UPDATE `tracks` SET `type` = " + Database.sqlString(type.toString()) + " WHERE `id` = " + id + ";");
    }

    public TPlayer getOwner() {
        return owner;
    }

    public void setOwner(TPlayer owner) {
        this.owner = owner;

        DB.executeUpdateAsync("UPDATE `tracks` SET `uuid` = '" + owner.getUniqueId() + "' WHERE `id` = " + id + ";");
    }

    public void setOptions(String options) {

        this.options = options.toCharArray();
        DB.executeUpdateAsync("UPDATE `tracks` SET `options` = " + Database.sqlString(options) + " WHERE `id` = " + id + ";");

    }

    public char[] getOptions() {
        return this.options;
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
}
