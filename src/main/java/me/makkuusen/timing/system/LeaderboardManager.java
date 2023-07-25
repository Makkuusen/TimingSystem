package me.makkuusen.timing.system;

import co.aikar.taskchain.TaskChain;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackLeaderboard;
import me.makkuusen.timing.system.track.TrackLocation;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LeaderboardManager {

    public static void updateFastestTimeLeaderboard(Track track) {
        if (!TimingSystem.enableLeaderboards) {
            return;
        }

        List<TrackLocation> trackLeaderboards = track.getTrackLocations(TrackLocation.Type.LEADERBOARD);

        for (TrackLocation tl : trackLeaderboards) {
            if (tl instanceof TrackLeaderboard trackLeaderboard) {
                trackLeaderboard.createOrUpdateHologram();
            }
        }

    }

    public static void updateAllFastestTimeLeaderboard() {
        if (!TimingSystem.enableLeaderboards) {
            return;
        }

        TaskChain<?> chain = TimingSystem.newChain();
        for (Track t : TrackDatabase.getTracks()) {
            chain.sync(() -> {
                updateFastestTimeLeaderboard(t);
            }).delay(1);
        }
        chain.execute();
    }

    public static void removeAllLeaderboards() {
        if (!TimingSystem.enableLeaderboards) {
            return;
        }
        for (Track t : TrackDatabase.getTracks()) {
            removeLeaderboards(t);
        }
    }

    public static void removeLeaderboards(Track track) {
        if (!TimingSystem.enableLeaderboards) {
            return;
        }
        Bukkit.getScheduler().runTask(TimingSystem.getPlugin(), () -> {

            List<TrackLocation> trackLeaderboards = track.getTrackLocations(TrackLocation.Type.LEADERBOARD);

            for (TrackLocation tl : trackLeaderboards) {
                if (tl instanceof TrackLeaderboard trackLeaderboard) {
                    trackLeaderboard.removeHologram();
                }
            }
        });
    }

    public static void startUpdateTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(TimingSystem.getPlugin(), (@NotNull Runnable) LeaderboardManager::updateAllFastestTimeLeaderboard, 30 * 20, TimingSystem.configuration.getLeaderboardsUpdateTick());
    }
}
