package me.makkuusen.timing.system.api;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class TimingSystemAPI {

    public static Optional<Driver> getDriverFromRunningHeat(UUID uuid) {
        return EventDatabase.getDriverFromRunningHeat(uuid);
    }

    public static Optional<Track> getTrack(String name) {
        return TrackDatabase.getTrack(name);
    }

    public static Optional<Track> getTrackById(int id) {
        return TrackDatabase.getTrackById(id);
    }

    public static List<Track> getTracks() {
        return TrackDatabase.getTracks();
    }

    public static List<Track> getAvailableTracks(Player player) {
        return TrackDatabase.getAvailableTracks(player);
    }

    public static List<Track> getOpenTracks() {
        return TrackDatabase.getOpenTracks();
    }

    public static Optional<TimeTrialFinish> getBestTime(UUID uuid, int trackId) {
        var track = TrackDatabase.getTrackById(trackId);
        if (track.isEmpty()){
            return Optional.empty();
        }
        var tPlayer = Database.getPlayer(uuid);
        var bestTime = track.get().getBestFinish(tPlayer);
        if (bestTime == null) {
            return Optional.empty();
        }
        return Optional.of(bestTime);
    }

    public static TPlayer getTPlayer(UUID uuid) {
        return Database.getPlayer(uuid);
    }

    public static void teleportPlayerAndSpawnBoat(Player player, Track track, Location location){
        ApiUtilities.teleportPlayerAndSpawnBoat(player, track, location);
    }

    public static List<Heat> getHeats(){
        return EventDatabase.getHeats();
    }

    public static List<Heat> getRunningHeats() {
        return EventDatabase.getHeats().stream().filter(Heat::isActive).collect(Collectors.toList());
    }

    public static List<DriverDetails> getAllDriverDetailsFromHeat(Heat heat) {

        List<DriverDetails> driverDetails = new ArrayList<>();
        Driver first = null;
        Driver previous = null;
        for (Driver d : heat.getLivePositions()) {
            driverDetails.add(getDriverDetailsFromHeatWithTimes(heat, d.getTPlayer().getUniqueId(), first, previous));
            if (first == null) {
                first = d;
            }
            previous = d;
        }

        return driverDetails;
    }

    private static DriverDetails getDriverDetailsFromHeatWithTimes(Heat heat, UUID driverUuid, Driver previousDriverCompare, Driver leaderDriverCompare) {
        var driverDetails = getDriverDetailsFromHeat(heat, driverUuid, previousDriverCompare.getTPlayer().getUniqueId());
        var driver = heat.getDrivers().get(driverUuid);

        if (leaderDriverCompare != null) {
            driverDetails.setGapFromLeader(driver.getTimeGap(leaderDriverCompare));
        }

        return driverDetails;
    }

    public static DriverDetails getDriverDetailsFromHeat(Heat heat, UUID driverUuid, UUID driverCompare) {
        var driver = heat.getDrivers().get(driverUuid);
        var driverDetails = new DriverDetails();

        driverDetails.setName(driver.getTPlayer().getName());
        driverDetails.setUuid(driverUuid.toString());
        driverDetails.setTeamColor(driver.getTPlayer().getHexColor());
        driverDetails.setOffline(driver.getTPlayer().getPlayer() == null);
        if (!driverDetails.isOffline()) {
            driverDetails.setInpit(driver.isInPit(driver.getTPlayer().getPlayer().getLocation()));
        }
        driverDetails.setLaps(driver.getLaps().size());
        driverDetails.setPits(driver.getPits());
        driverDetails.setPosition(driver.getPosition());
        driverDetails.setStartPosition(driver.getStartPosition());

        if (driverCompare != null) {
            var previousDriverCompare = heat.getDrivers().get(driverCompare);
            driverDetails.setGap(driver.getTimeGap(previousDriverCompare));
        }

        return driverDetails;
    }

    public static DriverDetails getDriverDetailsOfClosestPlayerFromHeat(Heat heat, UUID playerToCheckClosestTo) {
        Player player = Database.getPlayer(playerToCheckClosestTo).getPlayer();
        if (player == null) {
            return new DriverDetails();
        }
        var driver = EventDatabase.getClosestDriverForSpectator(player);

        if (driver.isEmpty()) {
            return new DriverDetails();
        }

        return getDriverDetailsFromHeat(heat, driver.get().getTPlayer().getUniqueId(), null);

    }
}
