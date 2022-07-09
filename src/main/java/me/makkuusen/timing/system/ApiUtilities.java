package me.makkuusen.timing.system;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiUtilities {

    static TimingSystem plugin;
    private static String[] months = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };
    private static Pattern niceLocation = Pattern.compile("^\\(\\[([A-Za-z0-9_]+)\\]([\\-]{0,1}[0-9]+),[ ]{0,1}([\\-]{0,1}[0-9]+),[ ]{0,1}([\\-]{0,1}[0-9]+)\\)$");

    public static long getTimestamp()
    {
        return (long) (System.currentTimeMillis() / 1000L);
    }

    public static String concat(String[] arguments, int startIndex)
    {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = startIndex; i < arguments.length; i++)
        {
            stringBuilder.append(" ");
            stringBuilder.append(arguments[i]);
        }

        return stringBuilder.substring(1);
    }

    public static String locationToString(Location location)
    {
        if (location == null) { return null; }

        return location.getWorld().getName() + " " + location.getX() + " " + location.getY() + " " + location.getZ() + " " + location.getYaw() + " " + location.getPitch();
    }

    public static Location stringToLocation(String string)
    {
        if (string == null || string.length() == 0) { return null; }

        String[] split = string.split(" ");
        return new Location(Bukkit.getWorld(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Float.parseFloat(split[4]), Float.parseFloat(split[5]));
    }

    public static Location niceStringToLocation(String string)
    {
        Matcher m = niceLocation.matcher(string);

        if (!m.find()) { return null; }

        return new Location(Bukkit.getWorld(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3)), Double.parseDouble(m.group(4)));
    }

    public static String niceDate(long timestamp)
    {
        if (timestamp == 0) { return "unknown"; }

        long rightNow = getTimestamp();

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timestamp * 1000);

        Calendar dateNow = Calendar.getInstance();
        dateNow.setTimeInMillis(rightNow * 1000);

        String day;

        if (date.get(Calendar.DAY_OF_MONTH) == dateNow.get(Calendar.DAY_OF_MONTH) && date.get(Calendar.MONTH) == dateNow.get(Calendar.MONTH) && date.get(Calendar.YEAR) == dateNow.get(Calendar.YEAR))
        {
            day = "today";
        }

        else
        {
            Calendar dateYesterday = Calendar.getInstance();
            dateYesterday.setTimeInMillis((rightNow - 86400) * 1000);

            day = date.get(Calendar.DAY_OF_MONTH) == dateYesterday.get(Calendar.DAY_OF_MONTH) && date.get(Calendar.MONTH) == dateYesterday.get(Calendar.MONTH) && date.get(Calendar.YEAR) == dateYesterday.get(Calendar.YEAR) ?  "yesterday" : date.get(Calendar.DAY_OF_MONTH) + " " + months[date.get(Calendar.MONTH)] + (dateNow.get(Calendar.YEAR) != date.get(Calendar.YEAR) ? " " + date.get(Calendar.YEAR) : "");
        }

        return day + ", " + String.format("%02d", date.get(Calendar.HOUR_OF_DAY)) + ":" + String.format("%02d", date.get(Calendar.MINUTE));
    }

    public static String niceLocation(Location location)
    {
        return location == null ? "unknown" : "([" + location.getWorld().getName() + "]" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }

    public static String parseFlagChange(char[] flagsOriginal, String change)
    {
        String flagsRaw = new String(flagsOriginal);
        change = change.replace("*", "bcgeptsu");

        boolean isAdding = true;

        for (int i = 0; i < change.length(); i++)
        {
            char currentChar = change.charAt(i);

            // the first character must be either a + or a -
            if (i == 0 && currentChar != '+' && currentChar != '-') { return null; }

            if (currentChar == '+')
            {
                isAdding = true;
                continue;
            }

            else if (currentChar == '-')
            {
                isAdding = false;
                continue;
            }

            // check for invalid flags
            if (currentChar != 'b' && currentChar != 'c' && currentChar != 'g' && currentChar != 'e' && currentChar != 'p' && currentChar != 't' && currentChar != 's'  && currentChar != 'u') { return null; }

            flagsRaw = isAdding ? flagsRaw + currentChar : flagsRaw.replace(String.valueOf(currentChar), "");
        }

        StringBuilder flagsNew = new StringBuilder();

        for (char flag : flagsRaw.toCharArray())
        {
            boolean exists = false;

            for (int j = 0; j < flagsNew.length(); j++)
            {
                if (flagsNew.charAt(j) == flag)
                {
                    exists = true;
                    break;
                }
            }

            if (!exists) { flagsNew.append(flag); }
        }

        char[] flagsNewArray = flagsNew.toString().toCharArray();

        Arrays.sort(flagsOriginal);
        Arrays.sort(flagsNewArray);

        flagsNew = new StringBuilder(new String(flagsNewArray));

        // don't change if there's nothing to change
        if (flagsNew.toString().equals(new String(flagsOriginal))) { return null; }

        return flagsNew.toString();
    }

    public static String formatPermissions(char[] permissions)
    {
        if (permissions.length == 0) { return "(inga)"; }

        StringBuilder output = new StringBuilder();

        for (char permission : permissions)
        {
            if (permission == 0) { continue; }
            output.append(permission);
        }

        return output.toString();
    }

    public static ItemStack stringToItem(String string)
    {
        try
        {
            YamlConfiguration yamlConfig = new YamlConfiguration();
            yamlConfig.loadFromString(string);

            return yamlConfig.getItemStack("item");
        } catch (Exception exception)
        {
            TimingSystem.getPlugin().getLogger().warning("Failed to translate from String to ItemStack: " + string);
            return null;
        }
    }

    public static String itemToString(ItemStack item)
    {
        YamlConfiguration yamlConfig = new YamlConfiguration();
        yamlConfig.set("item", item);

        return yamlConfig.saveToString();
    }

    public static void sendActionBar(String msg, Player player)
    {
        player.sendActionBar(Component.text(msg));
    }

    public static void msgConsole(String msg)
    {
        TimingSystem.getPlugin().logger.info(msg);
    }

    public static String color(String uncolored)
    {
        return ChatColor.translateAlternateColorCodes('&', uncolored);
    }

    public static String formatAsTime(long timeInMillis)
    {
        String toReturn;

        long hours = TimeUnit.MILLISECONDS.toHours(timeInMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % TimeUnit.HOURS.toMinutes(1);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % TimeUnit.MINUTES.toSeconds(1);
        String milis = String.format("%03d", (timeInMillis % 1000));

        if (hours == 0 && minutes == 0)
        {
            toReturn = String.format("%02d", seconds) + "." + milis;
        }
        else if (hours == 0)
        {
            toReturn = String.format("%02d:%02d", minutes, seconds) + "." + milis;
        }
        else
        {
            toReturn = String.format("%d:%02d:%02d", hours, minutes, seconds) + "." + milis;
        }
        return toReturn;
    }

    // Used by scoreboard and bossbar
    public static String formatAsRacingGap(long timeInMillis)
    {
        String toReturn;

        long hours = TimeUnit.MILLISECONDS.toHours(timeInMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % TimeUnit.HOURS.toMinutes(1);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % TimeUnit.MINUTES.toSeconds(1);
        String milis = String.format("%02d", (timeInMillis % 1000) / 10);

        if (hours == 0 && minutes == 0)
        {
            toReturn = String.format("%02d", seconds) + "." + milis;
        }
        else if (hours == 0)
        {
            toReturn = String.format("%d:%02d", minutes, seconds) + "." + milis;
        }
        else
        {
            toReturn = String.format("%d:%02d:%02d", hours, minutes, seconds) + "." + milis;
        }
        return toReturn;
    }

    public static long roundToTick(long timeInMillis)
    {
        return Math.round(timeInMillis/50) * 50;
    }
}
