package me.makkuusen.timing.system;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class RaceUtilities
{

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
        String milis = String.format("%03d", timeInMillis % 1000);

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
}
