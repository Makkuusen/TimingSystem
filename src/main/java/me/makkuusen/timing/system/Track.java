package me.makkuusen.timing.system;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Track
{
    private final int id;
    private TSPlayer owner;
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
    private final Map<Integer, TrackRegion> checkpoints = new HashMap<>();
    private final Map<Integer, TrackRegion> resetRegions = new HashMap<>();
    private final Map<TSPlayer, List<TimeTrialFinish>> raceFinishes = new HashMap<>();

    public enum TrackType
    {
        BOAT, ELYTRA, PARKOUR
    }
    public enum TrackMode
    {
        TIMETRIAL, PRACTICE, QUALIFICATION, RACE
    }

    public Track(ResultSet data) throws SQLException
    {
        id = data.getInt("id");
        owner = data.getString("uuid") == null ? null : ApiDatabase.getPlayer(UUID.fromString(data.getString("uuid")));
        name = data.getString("name");
        dateCreated = data.getInt("dateCreated");
        guiItem = ApiUtilities.stringToItem(data.getString("guiItem"));
        spawnLocation = ApiUtilities.stringToLocation(data.getString("spawn"));
        leaderboardLocation = ApiUtilities.stringToLocation(data.getString("leaderboard"));
        type = data.getString("type") == null ? null : TrackType.valueOf(data.getString("type"));
        toggleOpen = data.getBoolean("toggleOpen");
        toggleGovernment = data.getBoolean("toggleGovernment");
        options = data.getString("options") == null ? new char[0] : data.getString("options").toCharArray();
        mode = TrackMode.RACE;
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public TrackRegion getEndRegion()
    {
        return endRegion;
    }

    public TrackRegion getStartRegion()
    {
        return startRegion;
    }

    public ItemStack getGuiItem(UUID uuid)
    {
        ItemStack toReturn;
        if (guiItem == null)
        {
            if (isBoatTrack())
            {
                toReturn = new ItemBuilder(Material.PACKED_ICE).setName(getName()).build();
            }
            else if (isElytraTrack())
            {
                toReturn = new ItemBuilder(Material.ELYTRA).setName(getName()).build();
            }
            else
            {
                toReturn = new ItemBuilder(Material.BIG_DRIPLEAF).setName(getName()).build();
            }
        }
        else
        {
            toReturn = guiItem.clone();
        }

        if (toReturn == null){
            return null;
        }
        TSPlayer TSPlayer = ApiDatabase.getPlayer(uuid);

        List<Component> loreToSet = new ArrayList<>();

        String bestTime;

        if (getBestFinish(TSPlayer) == null)
        {
            bestTime = "§7Your best time: §e(none)";
        }
        else
        {
            bestTime = "§7Your best time: §e" + ApiUtilities.formatAsTime(getBestFinish(TSPlayer).getTime());
        }

        loreToSet.add(Component.text("§7Your position: §e" + (getPlayerTopListPosition(TSPlayer) == -1 ? "(none)" : getPlayerTopListPosition(TSPlayer))));
        loreToSet.add(Component.text(bestTime));
        loreToSet.add(Component.text("§7Created by: §e" + getOwner().getName()));
        loreToSet.add(Component.text("§7Type: §e" + getTypeAsString()));

        ItemMeta im = toReturn.getItemMeta();
        im.displayName(Component.text(getName()).color(TextColor.color(255,255,85)));
        im.lore(loreToSet);
        toReturn.setItemMeta(im);

        return toReturn;
    }

    public Location getSpawnLocation()
    {
        return spawnLocation;
    }

    public Location getLeaderboardLocation()
    {
        return leaderboardLocation;
    }

    public TrackType getType()
    {
        return type;
    }

    public TrackMode getMode()
    {
        return mode;
    }

    public boolean isOpen()
    {
        return toggleOpen;
    }

    public void setMode(TrackMode mode)
    {
        this.mode = mode;
    }

    public void setName(String name)
    {
        this.name = name;

        ApiDatabase.asynchronousQuery(new String[]{"UPDATE `tracks` SET `name` = '" + name + "' WHERE `id` = " + id + ";"});
    }

    public void setGuiItem(ItemStack guiItem)
    {
        this.guiItem = guiItem;

        ApiDatabase.asynchronousQuery(new String[]{"UPDATE `tracks` SET `guiItem` = " + ApiDatabase.sqlString(ApiUtilities.itemToString(guiItem)) + " WHERE `id` = " + id + ";"});
    }

    public void setSpawnLocation(Location spawn)
    {
        this.spawnLocation = spawn;

        ApiDatabase.asynchronousQuery(new String[]{"UPDATE `tracks` SET `spawn` = '" + ApiUtilities.locationToString(spawn) + "' WHERE `id` = " + id + ";"});
    }

    public void setLeaderboardLocation(Location leaderboard)
    {
        this.leaderboardLocation = leaderboard;

        ApiDatabase.asynchronousQuery(new String[]{"UPDATE `tracks` SET `leaderboard` = '" + ApiUtilities.locationToString(leaderboard) + "' WHERE `id` = " + id + ";"});
    }

    public void setToggleOpen(boolean toggleOpen)
    {
        this.toggleOpen = toggleOpen;

        ApiDatabase.asynchronousQuery(new String[]{"UPDATE `tracks` SET `toggleOpen` = " + toggleOpen + " WHERE `id` = " + id + ";"});

    }

    public void setStartRegion(Location minP, Location maxP)
    {
        startRegion.setMinP(minP);
        startRegion.setMaxP(maxP);

        ApiDatabase.asynchronousQuery(new String[]{"UPDATE `tracksRegions` SET `minP` = '" + ApiUtilities.locationToString(minP) + "', `maxP` = '" + ApiUtilities.locationToString(maxP) + "' WHERE `id` = " + startRegion.getId() + ";"});
    }

    public void setEndRegion(Location minP, Location maxP)
    {
        endRegion.setMinP(minP);
        endRegion.setMaxP(maxP);

        ApiDatabase.asynchronousQuery(new String[]{"UPDATE `tracksRegions` SET `minP` = '" + ApiUtilities.locationToString(minP) + "', `maxP` = '" + ApiUtilities.locationToString(maxP) + "' WHERE `id` = " + endRegion.getId() + ";"});
    }

    public void newStartRegion(TrackRegion region)
    {
        this.startRegion = region;
    }

    public void newEndRegion(TrackRegion region)
    {
        this.endRegion = region;
    }

    public Map<Integer, TrackRegion> getCheckpoints()
    {
        return checkpoints;
    }

    public void addCheckpoint(TrackRegion region)
    {
        checkpoints.put(region.getRegionIndex(), region);
    }

    public void setCheckpoint(Location minP, Location maxP, Location spawn, int index)
    {
        if (checkpoints.containsKey(index))
        {
            // Modify checkpoint
            TrackRegion checkpoint = checkpoints.get(index);
            checkpoint.setMinP(minP);
            checkpoint.setMaxP(maxP);
            checkpoint.setSpawnLocation(spawn);

            ApiDatabase.asynchronousQuery(new String[]{"UPDATE `tracksRegions` SET `minP` = '" + ApiUtilities.locationToString(minP) + "', `maxP` = '" + ApiUtilities.locationToString(maxP) + "', `spawn` = '" + ApiUtilities.locationToString(spawn) + "' WHERE `id` = " + checkpoint.getId() + ";"});

        }
        else
        {
            try
            {

                Connection connection = ApiDatabase.getConnection();
                Statement statement = connection.createStatement();
                statement.executeUpdate("INSERT INTO `tracksRegions` (`trackId`, `regionIndex`, `regionType`, `minP`, `maxP`, `spawn`, `isRemoved`) VALUES(" + id + ", " + index + ", " + ApiDatabase.sqlString(TrackRegion.RegionType.CHECKPOINT.toString()) + ", '" + ApiUtilities.locationToString(minP) + "', '" + ApiUtilities.locationToString(maxP) + "', '" + ApiUtilities.locationToString(spawn) + "', 0);", Statement.RETURN_GENERATED_KEYS);
                ResultSet keys = statement.getGeneratedKeys();

                keys.next();
                int regionId = keys.getInt(1);
                ResultSet result = statement.executeQuery("SELECT * FROM `tracksRegions` WHERE `id` = " + regionId + ";");
                result.next();


                TrackRegion checkpoint = new TrackRegion(result);
                addCheckpoint(checkpoint);
                TrackDatabase.addRaceRegion(checkpoint);

                statement.close();
                result.close();
                connection.close();

            } catch (SQLException exception)
            {
                exception.printStackTrace();
            }
        }
    }

    public boolean removeCheckpoint(int index)
    {
        if (checkpoints.containsKey(index))
        {
            var checkpoint = checkpoints.get(index);
            var checkpointId = checkpoint.getId();
            TrackDatabase.removeRaceRegion(checkpoint);
            checkpoints.remove(index);

            ApiDatabase.asynchronousQuery(new String[]{"UPDATE `tracksRegions` SET `isRemoved` = 1 WHERE `id` = " + checkpointId + ";"});
            return true;
        }
        return false;
    }

    public void setResetRegion(Location minP, Location maxP, Location spawn, int index)
    {
        if (resetRegions.containsKey(index))
        {
            // Modify checkpoint
            TrackRegion resetRegion = resetRegions.get(index);
            resetRegion.setMinP(minP);
            resetRegion.setMaxP(maxP);
            resetRegion.setSpawnLocation(spawn);

            ApiDatabase.asynchronousQuery(new String[]{"UPDATE `tracksRegions` SET `minP` = '" + ApiUtilities.locationToString(minP) + "', `maxP` = '" + ApiUtilities.locationToString(maxP) + "', `spawn` = '" + ApiUtilities.locationToString(spawn) + "' WHERE `id` = " + resetRegion.getId() + ";"});

        }
        else
        {
            try
            {

                Connection connection = ApiDatabase.getConnection();
                Statement statement = connection.createStatement();
                statement.executeUpdate("INSERT INTO `tracksRegions` (`trackId`, `regionIndex`, `regionType`, `minP`, `maxP`, `spawn`, `isRemoved`) VALUES(" + id + ", " + index + ", " + ApiDatabase.sqlString(TrackRegion.RegionType.RESET.toString()) + ", '" + ApiUtilities.locationToString(minP) + "', '" + ApiUtilities.locationToString(maxP) + "', '" + ApiUtilities.locationToString(spawn) + "', 0);", Statement.RETURN_GENERATED_KEYS);
                ResultSet keys = statement.getGeneratedKeys();

                keys.next();
                int regionId = keys.getInt(1);
                ResultSet result = statement.executeQuery("SELECT * FROM `tracksRegions` WHERE `id` = " + regionId + ";");
                result.next();


                TrackRegion resetRegion = new TrackRegion(result);
                addResetRegion(resetRegion);
                TrackDatabase.addRaceRegion(resetRegion);

                statement.close();
                result.close();
                connection.close();

            } catch (SQLException exception)
            {
                exception.printStackTrace();
            }
        }
    }

    public boolean removeResetRegion(int index)
    {
        if (resetRegions.containsKey(index))
        {
            var resetRegion = resetRegions.get(index);
            var resetRegionId = resetRegion.getId();
            TrackDatabase.removeRaceRegion(resetRegion);
            resetRegions.remove(index);

            ApiDatabase.asynchronousQuery(new String[]{"UPDATE `tracksRegions` SET `isRemoved` = 1 WHERE `id` = " + resetRegionId + ";"});
            return true;
        }
        return false;
    }

    public void addResetRegion(TrackRegion region)
    {
        resetRegions.put(region.getRegionIndex(), region);
    }

    public Map<Integer, TrackRegion> getResetRegions()
    {
        return resetRegions;
    }

    public void addRaceFinish(TimeTrialFinish timeTrialFinish)
    {
        if (raceFinishes.get(timeTrialFinish.getPlayer()) == null)
        {
            List<TimeTrialFinish> list = new ArrayList<>();
            list.add(timeTrialFinish);
            raceFinishes.put(timeTrialFinish.getPlayer(), list);
            return;
        }
        raceFinishes.get(timeTrialFinish.getPlayer()).add(timeTrialFinish);
    }

    public void newRaceFinish(long time, UUID uuid)
    {
        try
        {

            Connection connection = ApiDatabase.getConnection();
            Statement statement = connection.createStatement();

            long date = ApiUtilities.getTimestamp();

            statement.executeUpdate("INSERT INTO `tracksFinishes` (`trackId`, `uuid`, `date`, `time`, `isRemoved`) VALUES(" + id + ", '" + uuid + "', " + date + ", " + time + ", 0);", Statement.RETURN_GENERATED_KEYS);
            ResultSet keys = statement.getGeneratedKeys();

            keys.next();
            int finishId = keys.getInt(1);
            ResultSet result = statement.executeQuery("SELECT * FROM `tracksFinishes` WHERE `id` = " + finishId + ";");
            result.next();

            TimeTrialFinish timeTrialFinish = new TimeTrialFinish(result);
            addRaceFinish(timeTrialFinish);

            statement.close();
            result.close();
            connection.close();

        } catch (SQLException exception)
        {
            exception.printStackTrace();
        }
    }

    public TimeTrialFinish getBestFinish(TSPlayer player)
    {
        if (raceFinishes.get(player) == null)
        {
            return null;
        }
        var times = raceFinishes.get(player);
        if (times.isEmpty())
        {
            return null;
        }
        times.sort(new TimeTrialFinishComparator());
        return times.get(0);
    }

    public void deleteBestFinish(TSPlayer player, TimeTrialFinish bestFinish)
    {
        try
        {
            raceFinishes.get(player).remove(bestFinish);
            Connection connection = ApiDatabase.getConnection();
            Statement statement = connection.createStatement();
            statement.executeUpdate("UPDATE `tracksFinishes` SET `isRemoved` = 1 WHERE `id` = " + bestFinish.getId() + ";");

            var result = statement.executeQuery("SELECT * FROM `tracksFinishes` WHERE (`uuid`,`time`) IN (SELECT `uuid`, min(`time`) FROM `tracksFinishes` WHERE `trackId` = " + id + " AND `uuid` = '" + player.getUniqueId() + "' AND `isRemoved` = 0 GROUP BY `uuid`) AND `isRemoved` = 0 ORDER BY `time`;");
            while (result.next())
            {
                var rf = new TimeTrialFinish(result);
                if (raceFinishes.get(player).stream().noneMatch(raceFinish -> raceFinish.equals(rf)))
                {
                    addRaceFinish(rf);
                }
            }

            statement.close();
            result.close();
            connection.close();

        } catch (SQLException exception)
        {
            exception.printStackTrace();
        }
    }

    public Integer getPlayerTopListPosition(TSPlayer TSPlayer)
    {
        var topList = getTopList(-1);
        for (int i = 0; i < topList.size(); i++)
        {
            if (topList.get(i).getPlayer().equals(TSPlayer))
            {
                return ++i;
            }
        }
        return -1;
    }

    public List<TimeTrialFinish> getTopList(int limit)
    {

        List<TimeTrialFinish> bestTimes = new ArrayList<>();
        for (TSPlayer player : raceFinishes.keySet())
        {
            TimeTrialFinish bestFinish = getBestFinish(player);
            if (bestFinish != null)
            {
                bestTimes.add(bestFinish);
            }
        }
        bestTimes.sort(new TimeTrialFinishComparator());

        if (limit == -1)
        {
            return bestTimes;
        }

        return bestTimes.stream().limit(limit).collect(Collectors.toList());
    }

    public void teleportPlayer(Player player)
    {
        player.teleport(spawnLocation);
    }

    public void spawnBoat(Player player)
    {
        if (getType().equals(Track.TrackType.BOAT))
        {
            boolean nearest = player.getLocation().distance(spawnLocation) < 5;
            if (nearest)
            {
                Boat boat = spawnLocation.getWorld().spawn(spawnLocation, Boat.class);
                boat.setMetadata("spawned", new FixedMetadataValue(TimingSystem.getPlugin(), null));
                if (player.getName().equalsIgnoreCase("Hogge72"))
                {
                    boat.setWoodType(TreeSpecies.ACACIA);
                }
                else if (player.getName().equalsIgnoreCase("Makkuusen"))
                {
                    boat.setWoodType(TreeSpecies.JUNGLE);
                }
                boat.addPassenger(player);
            }
        }
    }

    public long getDateCreated()
    {
        return dateCreated;
    }

    public boolean isGovernment()
    {
        return toggleGovernment;
    }

    public boolean isPersonal()
    {
        return !toggleGovernment;
    }

    public void setToggleGovernment(boolean toggleGovernment)
    {
        this.toggleGovernment = toggleGovernment;

        ApiDatabase.asynchronousQuery(new String[]{"UPDATE `tracks` SET `toggleGovernment` = " + toggleGovernment + " WHERE `id` = " + id + ";"});
    }

    public boolean isElytraTrack()
    {
        return getType().equals(TrackType.ELYTRA);
    }

    public boolean isBoatTrack()
    {
        return getType().equals(TrackType.BOAT);
    }

    public boolean isParkourTrack()
    {
        return getType().equals(TrackType.PARKOUR);
    }

    public TrackType getTypeFromString(String type)
    {
        if (type.equalsIgnoreCase("parkour"))
        {
            return Track.TrackType.PARKOUR;
        }
        else if (type.equalsIgnoreCase("elytra"))
        {
            return Track.TrackType.ELYTRA;
        }
        else if (type.equalsIgnoreCase("boat"))
        {
            return Track.TrackType.BOAT;
        }
        return null;
    }

    public TrackMode getModeFromString(String mode)
    {
        if (mode.equalsIgnoreCase("race"))
        {
            return TrackMode.RACE;
        }
        else if (mode.equalsIgnoreCase("timetrial"))
        {
            return TrackMode.TIMETRIAL;
        }
        else
        return null;
    }

    public String getTypeAsString()
    {
        if (isBoatTrack())
        {
            return "Boat";
        }
        else if (isParkourTrack())
        {
            return "Parkour";
        }
        else if (isElytraTrack())
        {
            return "Elytra";
        }

        return "Unknown";
    }

    public String getModeAsString()
    {
        if (mode.equals(TrackMode.RACE))
        {
            return "Race";
        }
        else if (mode.equals(TrackMode.TIMETRIAL))
        {
            return "Timetrial";
        }
        else if (mode.equals(TrackMode.QUALIFICATION))
        {
            return "Qualification";
        }
        else if (mode.equals(TrackMode.PRACTICE))
        {
            return "Practice";
        }

        return "Unknown";
    }

    public void setTrackType(TrackType type)
    {
        this.type = type;

        ApiDatabase.asynchronousQuery(new String[]{"UPDATE `tracks` SET `type` = " + ApiDatabase.sqlString(type.toString()) + " WHERE `id` = " + id + ";"});
    }

    public TSPlayer getOwner()
    {
        return owner;
    }

    public void setOwner(TSPlayer owner)
    {
        this.owner = owner;

        ApiDatabase.asynchronousQuery(new String[]{"UPDATE `tracks` SET `uuid` = '" + owner.getUniqueId() + "' WHERE `id` = " + id + ";"});
    }

    public void setOptions(String options)
    {

        this.options = options.toCharArray();
        ApiDatabase.asynchronousQuery(new String[]{"UPDATE `tracks` SET `options` = " + ApiDatabase.sqlString(options) + " WHERE `id` = " + id + ";"});

    }

    public char[] getOptions()
    {
        return this.options;
    }

    public boolean hasOption(char needle)
    {
        if (this.options == null) { return false; }

        for (char option : this.options)
        {
            if (option == needle) { return true; }
        }

        return false;
    }
}
