package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.Database;
import me.makkuusen.timing.system.event.EventDatabase;
import org.bukkit.ChatColor;

public class ScoreboardUtils {
    public static String getDriverLine(String name, int pos) {
        return paddPos(pos, name) + "§7|           " + getTeamIcon(name) + paddName(name);
    }

    public static String getDriverLineQualyTime(long laptime, String name, int pos) {
        return paddPos(pos, name) + "§7| §e" + paddTime(ApiUtilities.formatAsTime(laptime)) + getTeamIcon(name) + paddName(name);
    }

    public static String getDriverLineQualyGap(long timeDiff, String name, int pos) {
        return paddPos(pos, name) + "§7| §a+" + paddGap(ApiUtilities.formatAsQualyGap(timeDiff)) + getTeamIcon(name) + paddName(name);
    }

    public static String getDriverLineNegativeQualyGap(long timeDiff, String name, int pos) {
        return paddPos(pos, name) + "§7| §c-" + paddGap(ApiUtilities.formatAsQualyGap(timeDiff)) + getTeamIcon(name) + paddName(name);
    }

    public static String getDriverLineRace(String name, int pos){
        return paddPos(pos, name) + "§7|           " + getTeamIcon(name) + paddName(name) + "§7Pits: §f0 ";
    }
    public static String getDriverLineRace(String name, int pits, int pos){
        return paddPos(pos, name) + "§7|           " + getTeamIcon(name) + paddName(name) + "§7Pits: " + getPitColour(name, pits) + " ";
    }
    public static String getDriverLineRaceInPit(String name, int pits, int pos){
        return paddPos(pos, name) + "§7| In Pit   " + getTeamIcon(name) + paddName(name) + "§7Pits: " + getPitColour(name, pits) + " ";
    }

    public static String getDriverLineRaceOffline(String name, int pits, int pos){
        return paddPos(pos, name) + "§7| Offline  " + getTeamIcon(name) + paddName(name) + "§7Pits: " + getPitColour(name, pits) + " ";
    }

    public static String getDriverLineRaceLaps(int laps, String name, int pits, int pos) {
        return paddPos(pos, name) + "§7| Lap:§f " + paddLaps(laps) + " " + getTeamIcon(name) + paddName(name) + "§7Pits: " + getPitColour(name, pits) + " ";
    }

    public static String getDriverLineRaceGap(long gap, String name, int pits, int pos) {
        return paddPos(pos, name) + "§7| §a+" + paddGap(ApiUtilities.formatAsRacingGap(gap)) + getTeamIcon(name) + paddName(name) + "§7Pits: " + getPitColour(name, pits) + " ";
    }

    public static String getDriverLineNegativeRaceGap(long gap, String name, int pits, int pos) {
        return paddPos(pos, name) + "§7| §c-" + paddGap(ApiUtilities.formatAsRacingGap(gap)) + getTeamIcon(name) + paddName(name) + "§7Pits: " + getPitColour(name, pits) + " ";
    }

    public static String paddName(String name){
        StringBuilder sb = new StringBuilder();
        sb.append(name + ChatColor.RESET);
        int spaces = 16 - name.length();
        sb.append(" ".repeat(Math.max(0, spaces)));

        return sb.toString();
    }

    public static String paddPos(int pos, String name) {
        if (pos > 9) {
            return getPosFormat(pos, name) + pos;
        }
        return getPosFormat(pos, name) + pos + "§r ";
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
        var driver = EventDatabase.getDriverFromRunningHeat(Database.getPlayer(name).getUniqueId());
        if(driver.isEmpty()) return "§f" + pits;
        if(driver.get().getPits() >= driver.get().getHeat().getTotalPits()) return "§a" + pits;
        else return "§f" + pits;
    }

    private static String getPosFormat(int pos, String name) {
        // Pigalala's Messiest code
        var driver = EventDatabase.getDriverFromRunningHeat(Database.getPlayer(name).getUniqueId());

        net.md_5.bungee.api.ChatColor posColour;
        boolean hasFastestLap = false;
        boolean isFinished = false;

        switch (pos) {
            case 1 -> posColour = net.md_5.bungee.api.ChatColor.of("#f6f31a");
            case 2 -> posColour = net.md_5.bungee.api.ChatColor.of("#c3c3c3");
            case 3 -> posColour = net.md_5.bungee.api.ChatColor.of("#CD7F32");
            default -> posColour = net.md_5.bungee.api.ChatColor.of("#ffffff");
        }

        if(driver.isEmpty()) return posColour + "";

        if(driver.get().isFinished()) isFinished = true;
        if(driver.get().getHeat().getFastestLapUUID() == driver.get().getTPlayer().getUniqueId()) hasFastestLap = true;

        if(isFinished && hasFastestLap) return posColour + "" + ChatColor.UNDERLINE + ChatColor.ITALIC;
        else if(isFinished) return posColour + "" + ChatColor.ITALIC;
        else if(hasFastestLap) return posColour + "" + ChatColor.UNDERLINE;
        else return posColour + "";
    }

    private static String getColor(String name){
        var maybeDriver = EventDatabase.getDriverFromRunningHeat(Database.getPlayer(name).getUniqueId());

        if (maybeDriver.isEmpty()) {
            return net.md_5.bungee.api.ChatColor.of("#ffffff") + "";
        }

        return maybeDriver.get().getTPlayer().getColorCode();
    }

    private static String getTeamIcon(String name){
        return getColor(name) + "§l§o||§r ";
    }

}
