package me.makkuusen.timing.system.track;

import co.aikar.idb.DbRow;
import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.filoghost.holographicdisplays.api.hologram.HologramLines;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class TrackLeaderboard extends TrackLocation {

    Hologram hologram;
    Track track;

    public TrackLeaderboard(int trackId, int index, Location location, TrackLocation.Type locationType) {
        super(trackId, index, location, locationType);
        var maybeTrack = TrackDatabase.getTrackById(getTrackId());
        maybeTrack.ifPresent(value -> this.track = value);
    }

    public TrackLeaderboard(DbRow data) {
        super(data);
        var maybeTrack = TrackDatabase.getTrackById(getTrackId());
        maybeTrack.ifPresent(value -> this.track = value);
    }

    public void createOrUpdateHologram() {
        if (!TimingSystem.enableLeaderboards) {
            return;
        }

        Bukkit.getScheduler().runTask(TimingSystem.getPlugin(), () -> {
            if (!getLocation().isWorldLoaded()) {
                return;
            }

            if (hologram == null) {
                hologram = HolographicDisplaysAPI.get(TimingSystem.getPlugin()).createHologram(getLocation());
            } else if (!hologram.getPosition().isInSameWorld(getLocation())) {
                hologram.delete();
                hologram = HolographicDisplaysAPI.get(TimingSystem.getPlugin()).createHologram(getLocation());
            } else if (hologram.getPosition().distance(getLocation()) > 1) {
                hologram.delete();
                hologram = HolographicDisplaysAPI.get(TimingSystem.getPlugin()).createHologram(getLocation());
            }

            HologramLines hologramLines = hologram.getLines();
            hologramLines.clear();

            for (String line : getHologramLines()) {
                hologramLines.appendText(line);
            }
        });
    }

    public void removeHologram() {
        if (!TimingSystem.enableLeaderboards) {
            return;
        }
        Bukkit.getScheduler().runTask(TimingSystem.getPlugin(), () -> hologram.delete());
    }

    private List<String> getHologramLines() {
        var topTen = track.getTopList(10);
        List<String> textLines = new ArrayList<>();

        for (String line : TimingSystem.configuration.getLeaderboardsFastestTimeLines()) {

            line = line.replace("{mapname}", track.getDisplayName());

            // Replace stuff

            for (int i = 1; i <= 10; i++) {
                String playerName;
                String time;
                try {
                    playerName = topTen.get(i - 1).getPlayer().getName();
                    time = ApiUtilities.formatAsTime(topTen.get(i - 1).getTime());
                } catch (IndexOutOfBoundsException e) {
                    playerName = "Empty";
                    time = "None";
                }
                line = line.replace("{name" + i + "}", playerName);
                line = line.replace("{time" + i + "}", time);
            }
            textLines.add(line);
        }
        return textLines;
    }
}
