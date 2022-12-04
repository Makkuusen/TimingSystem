package me.makkuusen.timing.system.api;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.TPlayer;
import me.makkuusen.timing.system.event.EventDatabase;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    public static void teleportPlayerAndSpawnBoat(Player player, boolean isBoatTrack, Location location){
        ApiUtilities.teleportPlayerAndSpawnBoat(player, isBoatTrack, location);
    }
}
