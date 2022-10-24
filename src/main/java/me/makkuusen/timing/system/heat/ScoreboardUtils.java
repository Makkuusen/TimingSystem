package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.event.EventDatabase;
import org.bukkit.Bukkit;

public class ScoreboardUtils {
    public static String getDriverLine(String name, int pos) {
        return paddPos(pos) + "§7|           §7| §f" + paddName(name);
    }

    public static String getDriverLineQualyTime(long laptime, String name, int pos) {
        return paddPos(pos) + "§7| §e" + paddTime(ApiUtilities.formatAsTime(laptime)) + "§7| §f"+ paddName(name);
    }

    public static String getDriverLineQualyGap(long timeDiff, String name, int pos) {
        return paddPos(pos) + "§7| §a+" + paddGap(ApiUtilities.formatAsQualyGap(timeDiff)) + "§7| §f"+ paddName(name);
    }

    public static String getDriverLineNegativeQualyGap(long timeDiff, String name, int pos) {
        return paddPos(pos) + "§7| §c-" + paddGap(ApiUtilities.formatAsQualyGap(timeDiff)) + "§7| §f"+ paddName(name);
    }

    public static String getDriverLineRace(String name, int pos){
        return paddPos(pos) + "§7|           §7| §f" + paddName(name) + "§7Pits: §f0 ";
    }
    public static String getDriverLineRace(String name, int pits, int pos){
        return paddPos(pos) + "§7|           §7| §f" + paddName(name) + "§7Pits: " + getPitColour(name, pits) + " ";
    }
    public static String getDriverLineRaceInPit(String name, int pits, int pos){
        return paddPos(pos) + "§7| In Pit   §7| §f" + paddName(name) + "§7Pits: " + getPitColour(name, pits) + " ";
    }

    public static String getDriverLineRaceOffline(String name, int pits, int pos){
        return paddPos(pos) + "§7| Offline  §7| §f" + paddName(name) + "§7Pits: " + getPitColour(name, pits) + " ";
    }

    public static String getDriverLineRaceLaps(int laps, String name, int pits, int pos) {
        return paddPos(pos) + "§7| Lap:§f " + paddLaps(laps) + " §7| §f" + paddName(name) + "§7Pits: " + getPitColour(name, pits) + " ";
    }

    public static String getDriverLineRaceGap(long gap, String name, int pits, int pos) {
        return paddPos(pos) + "§7| §a+" + paddGap(ApiUtilities.formatAsRacingGap(gap)) + "§7| §f"+ paddName(name) + "§7Pits: " + getPitColour(name, pits) + " ";
    }

    public static String getDriverLineNegativeRaceGap(long gap, String name, int pits, int pos) {
        return paddPos(pos) + "§7| §c-" + paddGap(ApiUtilities.formatAsRacingGap(gap)) + "§7| §f"+ paddName(name) + "§7Pits: " + getPitColour(name, pits) + " ";
    }

    public static String paddName(String name){
        StringBuilder sb = new StringBuilder();
        sb.append(getNameColour(name));
        sb.append(name);
        int spaces = 16 - name.length();
        for(int i = 0; i < spaces; i++) {
            sb.append(" ");
        }

        return sb.toString();
    }

    public static String paddPos(int pos) {
        String posColour;
        switch (pos) {
            case 1 -> posColour = "§6";
            case 2 -> posColour = "§f";
            case 3 -> posColour = "§4";
            default -> posColour = "§7";
        }
        if (pos > 9) {
            return posColour + pos;
        }
        return posColour + pos + " ";
    }

    public static String paddGap(String gap) {
        if (gap.length() == 5){
            return gap + "  ";
        }
        return gap;
    }

    public static String paddTime(String time) {
        if (time.length() == 6) {
            return time + "  ";
        }
        return time;
    }

    public static String paddLaps(int laps) {
        if (laps < 10) {
            return "0" + laps;
        }
        return String.valueOf(laps);
    }

    private static String getPitColour(String name, int pits) {
        var driver = EventDatabase.getDriverFromRunningHeat(Bukkit.getPlayer(name).getUniqueId());
        if(!driver.isPresent()) return "§f" + pits;
        if(driver.get().getPits() >= driver.get().getHeat().getTotalPits()) return "§a" + pits;
        else return "§f" + pits;
    }

    private static String getNameColour(String name) {
        var driver = EventDatabase.getDriverFromRunningHeat(Bukkit.getPlayer(name).getUniqueId());
        if(!driver.isPresent()) return "§f";
        if(driver.get().isFinished()) return "§a";
        else return "§f";
    }
}
