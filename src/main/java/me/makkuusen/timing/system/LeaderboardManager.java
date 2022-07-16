package me.makkuusen.timing.system;

import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.filoghost.holographicdisplays.api.hologram.HologramLines;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LeaderboardManager
{

    private static final HashMap<Integer, Hologram> fastestHolograms = new HashMap<>();


    public static void updateFastestTimeLeaderboard(int id)
    {
        if (!TimingSystem.enableLeaderboards)
        {
            return;
        }
        var maybeTrack = TrackDatabase.getTrackById(id);
		if (maybeTrack.isEmpty())
		{
			ApiUtilities.msgConsole("Leaderboard couldn't update, track not found");
			return;
		}
		Track Track = maybeTrack.get();

        var topTen = Track.getTopList(10);
        List<String> textLines = new ArrayList<>();

        for (String line : TimingSystem.configuration.leaderboardsFastestTimeLines())
        {

            line = line.replace("{mapname}", Track.getName());

            // Replace stuff

            for (int i = 1; i <= 10; i++)
            {
                String playerName;
                String time;
                try
                {
                    playerName = topTen.get(i - 1).getPlayer().getName();
                    time = ApiUtilities.formatAsTime(topTen.get(i - 1).getTime());
                } catch (IndexOutOfBoundsException e)
                {
                    playerName = "Empty";
                    time = "None";
                }
                line = line.replace("{name" + i + "}", playerName);
                line = line.replace("{time" + i + "}", time);
            }
            textLines.add(line);
        }
        Bukkit.getScheduler().runTask(TimingSystem.getPlugin(), () -> {
			Hologram holo;
			Location leaderBoardLocation = Track.getLeaderboardLocation();

			if (fastestHolograms.get(id) == null)
			{
				holo = HolographicDisplaysAPI.get(TimingSystem.getPlugin()).createHologram(leaderBoardLocation);
				fastestHolograms.put(id, holo);
			}
			else if (fastestHolograms.get(id).getPosition().distance(leaderBoardLocation) > 1)
			{
				fastestHolograms.get(id).delete();
				holo = HolographicDisplaysAPI.get(TimingSystem.getPlugin()).createHologram(leaderBoardLocation);
				fastestHolograms.put(id, holo);
			}
			else
			{
				holo = fastestHolograms.get(id);
			}

			HologramLines hologramLines = holo.getLines();
			hologramLines.clear();

			for (String line : textLines)
			{
				hologramLines.appendText(line);
			}
		});

    }

    public static void updateAllFastestTimeLeaderboard()
    {
        if (!TimingSystem.enableLeaderboards)
        {
            return;
        }
        for (Track t : TrackDatabase.getTracks())
        {
            updateFastestTimeLeaderboard(t.getId());
        }
    }

    public static void updateAllFastestTimeLeaderboard(CommandSender toNotify)
    {
        if (!TimingSystem.enableLeaderboards)
        {
            return;
        }
        for (Track rTrack : TrackDatabase.getTracks())
        {
            updateFastestTimeLeaderboard(rTrack.getId());
        }
        toNotify.sendMessage("§aFinished updating all of the fastest time leaderboards.");
    }

    public static void removeLeaderboard(int id)
    {
        if (!TimingSystem.enableLeaderboards)
        {
            return;
        }
        Bukkit.getScheduler().runTask(TimingSystem.getPlugin(), () -> {
			fastestHolograms.get(id).delete();
			fastestHolograms.remove(id);
		});
    }

    public static void startUpdateTask()
    {
        Bukkit.getScheduler().runTaskTimerAsynchronously(TimingSystem.getPlugin(), (@NotNull Runnable) LeaderboardManager::updateAllFastestTimeLeaderboard, 30 * 20, TimingSystem.configuration.leaderboardsUpdateTick());
    }
}
