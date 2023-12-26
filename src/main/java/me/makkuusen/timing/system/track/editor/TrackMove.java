package me.makkuusen.timing.system.track.editor;

import com.sk89q.worldedit.math.BlockVector2;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.LeaderboardManager;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.logging.track.LogTrackMoved;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.locations.TrackLocation;
import me.makkuusen.timing.system.track.regions.TrackPolyRegion;
import me.makkuusen.timing.system.track.regions.TrackRegion;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class TrackMove {

    public static Component move(Player player, Track track) {
        var moveTo = player.getLocation().toBlockLocation();
        var moveFrom = track.getSpawnLocation().toBlockLocation();
        World newWorld = moveTo.getWorld();
        track.setSpawnLocation(moveTo);
        var offset = getOffset(moveFrom, moveTo);

        var trackLocations = track.getTrackLocations().getLocations();
        for (TrackLocation tl : trackLocations) {
            Location loc = tl.getLocation();
            var newLoc = getNewLocation(newWorld, loc, offset);
            track.getTrackLocations().update(tl, newLoc);
        }

        var regions = track.getTrackRegions().getRegions();
        for (TrackRegion region : regions) {
            if (region.getSpawnLocation() != null) {
                region.setSpawn(getNewLocation(newWorld, region.getSpawnLocation(), offset));
            }

            if (region.getMaxP() != null) {
                region.setMaxP(getNewLocation(newWorld, region.getMaxP(), offset));
            }

            if (region.getMinP() != null) {
                region.setMinP(getNewLocation(newWorld, region.getMinP(), offset));
            }

            if (region instanceof TrackPolyRegion polyRegion) {
                var oldPoints = polyRegion.getPolygonal2DRegion().getPoints();
                List<BlockVector2> newPoints = new ArrayList<>();
                for (BlockVector2 b : oldPoints) {
                    newPoints.add(getNewBlockVector2(b, offset));
                }
                polyRegion.updateRegion(newPoints);
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), LeaderboardManager::updateAllFastestTimeLeaderboard);
        LogTrackMoved.create(TSDatabase.getPlayer(player), track, moveFrom, moveTo).save();
        return Text.get(player, Success.TRACK_MOVED, "%to%", ApiUtilities.niceLocation(moveTo), "%from%", ApiUtilities.niceLocation(moveFrom));
    }

    public static Vector getOffset(Location moveFrom, Location moveTo) {
        var vector = new Vector();
        vector.setX(moveFrom.getX() - moveTo.getX());
        vector.setY(moveFrom.getY() - moveTo.getY());
        vector.setZ(moveFrom.getZ() - moveTo.getZ());
        return vector;
    }

    public static Location getNewLocation(World newWorld, Location oldLocation, Vector offset) {
        var referenceNewWorld = new Location(newWorld, oldLocation.getX(), oldLocation.getY(), oldLocation.getZ(), oldLocation.getYaw(), oldLocation.getPitch());
        referenceNewWorld.subtract(offset);
        return referenceNewWorld;
    }

    public static BlockVector2 getNewBlockVector2(BlockVector2 old, Vector offset) {
        return BlockVector2.at(old.getX() - offset.getX(), old.getZ() - offset.getZ());
    }
}
