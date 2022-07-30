package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.ApiUtilities;

public class ScoreboardUtils {
    public static String getDriverLine(String name, int pos) {
        //return "§f          §7| §f" + paddName(name) + " §7| §6" + pos;
        return "§6" + paddPos(pos) + "§7|           §7| §f" + paddName(name);
    }

    public static String getDriverLineQualyTime(long laptime, String name, int pos) {
        //return "§f " + ApiUtilities.formatAsTime(laptime) + " §7| §f" + paddName(name) + " §7| §6" + pos;
        return "§6" + paddPos(pos) + "§7| §e" + paddTime(ApiUtilities.formatAsTime(laptime)) + "§7| §f"+ paddName(name);
    }

    public static String getDriverLineQualyGap(long timeDiff, String name, int pos) {
        //return "§f   §a+" + ApiUtilities.formatAsQualyGap(timeDiff) + " §7| §f" + paddName(name) + " §7| §6" + pos;
        return "§6" + paddPos(pos) + "§7| §a+" + paddGap(ApiUtilities.formatAsQualyGap(timeDiff)) + "§7| §f"+ paddName(name);
    }

    public static String getDriverLineNegativeQualyGap(long timeDiff, String name, int pos) {
        //return "§f   §c-" + ApiUtilities.formatAsQualyGap(timeDiff) + " §7| §f" + paddName(name) + " §7| §6" + pos;
        return "§6" + paddPos(pos) + "§7| §c-" + paddGap(ApiUtilities.formatAsQualyGap(timeDiff)) + "§7| §f"+ paddName(name);
    }

    public static String getDriverLineRace(String name, int pos){
        //return "          §7| §f" + paddName(name) + " §7| §7Pits: §f0 §7| §6" + pos;
        return "§6" + paddPos(pos) + "§7|           §7| §f" + paddName(name) + "§7Pits: §f0 ";
    }
    public static String getDriverLineRace(String name, int pits, int pos){
        //return "          §7| §f" + paddName(name) + " §7| §7Pits: §f" + pits + " §7| §6" + pos;
        return "§6" + paddPos(pos) + "§7|           §7| §f" + paddName(name) + "§7Pits: §f" + pits + " ";
    }

    public static String getDriverLineRaceLaps(int laps, String name, int pits, int pos) {
        return "§6" + paddPos(pos) + "§7| Lap:§f " + paddLaps(laps) + " §7| §f" + paddName(name) + "§7Pits: §f" + pits + " ";
        //return "§7 Lap: §f" + laps + " §7| §f" + paddName(name) + " §7| §7Pits: §f" + pits + "§7| §6" + pos;
    }

    public static String getDriverLineRaceGap(long gap, String name, int pits, int pos) {
        //return "§f +" + ApiUtilities.formatAsRacingGap(gap) + " §7| §f" + paddName(name) + " §7| §7Pits: §f" + pits + " §7| §6" + pos;
        return "§6" + paddPos(pos) + "§7| §a+" + paddGap(ApiUtilities.formatAsRacingGap(gap)) + "§7| §f"+ paddName(name) + "§7Pits: §f" + pits + " ";
    }

    public static String getDriverLineNegativeRaceGap(long gap, String name, int pits, int pos) {
        return "§6" + paddPos(pos) + "§7| §c-" + paddGap(ApiUtilities.formatAsRacingGap(gap)) + "§7| §f"+ paddName(name) + "§7Pits: §f" + pits + " ";
    }

    public static String paddName(String name){
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        int spaces = 16 - name.length();
        for(int i = 0; i < spaces; i++) {
            sb.append(" ");
        }

        return sb.toString();
    }

    public static String paddPos(int pos) {
        if (pos > 9) {
            return String.valueOf(pos);
        }
        return pos + " ";
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
        if (laps < 9) {
            return "0" + laps;
        }
        return String.valueOf(laps);
    }
}
