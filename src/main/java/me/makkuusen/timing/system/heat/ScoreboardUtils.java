package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.theme.Theme;
import org.bukkit.ChatColor;

public class ScoreboardUtils {
    public static String getDriverLine(Driver driver, int pos, boolean compact, Theme theme) {
        return paddPos(pos, driver) + (compact ? "" : getDivider(theme)) + "           " + getTeamIcon(driver) + paddName(driver, compact);
    }

    public static String getDriverLineQualyTime(long laptime, Driver driver, int pos, boolean compact, Theme theme) {
        return paddPos(pos, driver) + (compact ? "" : getDivider(theme)) + " " + getSecondaryColor(theme) + paddTime(ApiUtilities.formatAsTime(laptime)) + getTeamIcon(driver) + paddName(driver, compact);
    }

    public static String getDriverLineQualyGap(long timeDiff, Driver driver, int pos, boolean compact, Theme theme) {
        return paddPos(pos, driver) + (compact ? "" : getDivider(theme)) + " §a+" + paddGap(ApiUtilities.formatAsQualificationGap(timeDiff)) + getTeamIcon(driver) + paddName(driver, compact);
    }

    public static String getDriverLineNegativeQualyGap(long timeDiff, Driver driver, int pos, boolean compact, Theme theme) {
        return paddPos(pos, driver) + (compact ? "" : getDivider(theme)) + " §c-" + paddGap(ApiUtilities.formatAsQualificationGap(timeDiff)) + getTeamIcon(driver) + paddName(driver, compact);
    }

    public static String getDriverLineRace(Driver driver, int pos, boolean compact, Theme theme) {
        return paddPos(pos, driver) + (compact ? "" : getDivider(theme)) + "           " + getTeamIcon(driver) + paddName(driver, compact) + getPits(compact, theme) + "§f0";
    }

    public static String getDriverLineRace(Driver driver, int pits, int pos, boolean compact, Theme theme) {
        return paddPos(pos, driver) + (compact ? "" : getDivider(theme)) + "           " + getTeamIcon(driver) + paddName(driver, compact) + getPits(compact, theme) + getPitColour(driver, pits);
    }

    public static String getDriverLineRaceInPit(Driver driver, int pits, int pos, boolean compact, Theme theme) {
        return paddPos(pos, driver) + (compact ? "" : getDivider(theme)) + " In Pit   " + getTeamIcon(driver) + paddName(driver, compact) + getPits(compact, theme) + getPitColour(driver, pits);
    }

    public static String getDriverLineRaceOffline(Driver driver, int pits, int pos, boolean compact, Theme theme) {
        return paddPos(pos, driver) + (compact ? "" : getDivider(theme)) + " Offline  " + getTeamIcon(driver) + paddName(driver, compact) + getPits(compact, theme) + getPitColour(driver, pits);
    }

    public static String getDriverLineRaceLaps(int laps, Driver driver, int pits, int pos, boolean compact, Theme theme) {
        return paddPos(pos, driver) + (compact ? "" : getDivider(theme)) + " Lap: " + getSecondaryColor(theme) + paddLaps(laps) + " " + getTeamIcon(driver) + paddName(driver, compact) + getPits(compact, theme) + getPitColour(driver, pits);
    }

    public static String getDriverLineRaceGap(long gap, Driver driver, int pits, int pos, boolean compact, Theme theme) {
        return paddPos(pos, driver) + (compact ? "" : getDivider(theme)) + " §a+" + paddGap(ApiUtilities.formatAsRacingGap(gap)) + getTeamIcon(driver) + paddName(driver, compact) + getPits(compact, theme) + getPitColour(driver, pits);
    }

    public static String getDriverLineNegativeRaceGap(long gap, Driver driver, int pits, int pos, boolean compact, Theme theme) {
        return paddPos(pos, driver) + (compact ? "" : getDivider(theme)) + " §c-" + paddGap(ApiUtilities.formatAsRacingGap(gap)) + getTeamIcon(driver) + paddName(driver, compact) + getPits(compact, theme) + getPitColour(driver, pits);
    }

    private static String getDivider(Theme theme) {
        return getPrimaryColor(theme) + "|";
    }

    public static String paddName(Driver driver, boolean compact) {

        String name = driver.getTPlayer().getName();
        StringBuilder sb = new StringBuilder();
        if (compact) {
            if (name.length() > 3) {
                sb.append(name, 0, 4);
            } else {
                sb.append(name);
                int spaces = 4 - name.length();
                sb.append(" ".repeat(Math.max(0, spaces)));
            }
        } else {
            sb.append(name + ChatColor.RESET);
            int spaces = 16 - name.length();
            sb.append(" ".repeat(Math.max(0, spaces)));
        }
        return sb.toString();
    }

    public static String paddPos(int pos, Driver driver) {
        if (pos > 9) {
            return getPosFormat(pos, driver) + pos;
        }
        return getPosFormat(pos, driver) + pos + "§r ";
    }

    public static String paddGap(String gap) {
        if (gap.length() == 5) {
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

    private static String getPitColour(Driver driver, int pits) {
        if (driver.getPits() >= driver.getHeat().getTotalPits()) return "§a" + pits;
        else if (driver.getPits() > 0) return "§6" + pits;
        else return "§c" + pits;
    }

    public static String getSecondaryColor(Theme theme) {
        return String.valueOf(net.md_5.bungee.api.ChatColor.of(theme.getSecondary().asHexString()));
    }

    public static String getPrimaryColor(Theme theme) {
        return String.valueOf(net.md_5.bungee.api.ChatColor.of(theme.getPrimary().asHexString()));
    }

    private static String getPosFormat(int pos, Driver driver) {

        net.md_5.bungee.api.ChatColor posColour;

        switch (pos) {
            case 1 -> posColour = net.md_5.bungee.api.ChatColor.of("#f6f31a");
            case 2 -> posColour = net.md_5.bungee.api.ChatColor.of("#c3c3c3");
            case 3 -> posColour = net.md_5.bungee.api.ChatColor.of("#CD7F32");
            default -> posColour = net.md_5.bungee.api.ChatColor.of("#ffffff");
        }

        boolean hasFastestLap = driver.getHeat().getFastestLapUUID() == driver.getTPlayer().getUniqueId();

        if (driver.isFinished() && hasFastestLap) {
            return posColour + String.valueOf(ChatColor.UNDERLINE) + ChatColor.ITALIC;

        } else if (driver.isFinished()) {
            return posColour + String.valueOf(ChatColor.ITALIC);

        } else if (hasFastestLap) {
            return posColour + String.valueOf(ChatColor.UNDERLINE);

        } else {
            return String.valueOf(posColour);
        }
    }

    private static String getTeamIcon(Driver driver) {
        return driver.getTPlayer().getColorCode() + "§l§o||§r ";
    }

    private static String getPits(boolean compact, Theme theme) {
        if (compact) {
            return " ";
        }
        return getPrimaryColor(theme) + "Pits: ";
    }

}
