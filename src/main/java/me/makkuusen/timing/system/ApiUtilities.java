package me.makkuusen.timing.system;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiUtilities {

    static Race plugin;
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
}
