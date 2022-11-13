package me.makkuusen.timing.system;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import me.makkuusen.timing.system.track.TrackCuboidRegion;
import me.makkuusen.timing.system.track.TrackPolyRegion;
import me.makkuusen.timing.system.track.TrackRegion;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiUtilities {

    static TimingSystem plugin;
    private static final String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    private static final Pattern niceLocation = Pattern.compile("^\\(\\[([A-Za-z0-9_]+)\\]([\\-]{0,1}[0-9]+),[ ]{0,1}([\\-]{0,1}[0-9]+),[ ]{0,1}([\\-]{0,1}[0-9]+)\\)$");

    public static long getTimestamp() {
        return System.currentTimeMillis() / 1000L;
    }

    public static String concat(String[] arguments, int startIndex) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = startIndex; i < arguments.length; i++) {
            stringBuilder.append(" ");
            stringBuilder.append(arguments[i]);
        }

        return stringBuilder.substring(1);
    }

    public static String locationToString(Location location) {
        if (location == null) {
            return null;
        }

        return location.getWorld().getName() + " " + location.getX() + " " + location.getY() + " " + location.getZ() + " " + location.getYaw() + " " + location.getPitch();
    }

    public static Location stringToLocation(String string) {
        if (string == null || string.length() == 0) {
            return null;
        }

        String[] split = string.split(" ");
        return new Location(Bukkit.getWorld(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Float.parseFloat(split[4]), Float.parseFloat(split[5]));
    }

    public static Location stringToBlockVector3(String string) {
        if (string == null || string.length() == 0) {
            return null;
        }

        String[] split = string.split(" ");
        return new Location(Bukkit.getWorld(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Float.parseFloat(split[4]), Float.parseFloat(split[5]));
    }

    public static Location niceStringToLocation(String string) {
        Matcher m = niceLocation.matcher(string);

        if (!m.find()) {
            return null;
        }

        return new Location(Bukkit.getWorld(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3)), Double.parseDouble(m.group(4)));
    }

    public static String niceDate(long timestamp) {
        if (timestamp == 0) {
            return "unknown";
        }

        long rightNow = getTimestamp();

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timestamp * 1000);

        Calendar dateNow = Calendar.getInstance();
        dateNow.setTimeInMillis(rightNow * 1000);

        String day;

        if (date.get(Calendar.DAY_OF_MONTH) == dateNow.get(Calendar.DAY_OF_MONTH) && date.get(Calendar.MONTH) == dateNow.get(Calendar.MONTH) && date.get(Calendar.YEAR) == dateNow.get(Calendar.YEAR)) {
            day = "today";
        } else {
            Calendar dateYesterday = Calendar.getInstance();
            dateYesterday.setTimeInMillis((rightNow - 86400) * 1000);

            day = date.get(Calendar.DAY_OF_MONTH) == dateYesterday.get(Calendar.DAY_OF_MONTH) && date.get(Calendar.MONTH) == dateYesterday.get(Calendar.MONTH) && date.get(Calendar.YEAR) == dateYesterday.get(Calendar.YEAR) ? "yesterday" : date.get(Calendar.DAY_OF_MONTH) + " " + months[date.get(Calendar.MONTH)] + (dateNow.get(Calendar.YEAR) != date.get(Calendar.YEAR) ? " " + date.get(Calendar.YEAR) : "");
        }

        return day + ", " + String.format("%02d", date.get(Calendar.HOUR_OF_DAY)) + ":" + String.format("%02d", date.get(Calendar.MINUTE));
    }

    public static String niceLocation(Location location) {
        return location == null || !location.isWorldLoaded() ? "unknown" : "([" + location.getWorld().getName() + "]" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }

    public static String parseFlagChange(char[] flagsOriginal, String change) {
        String flagsRaw = new String(flagsOriginal);
        change = change.replace("*", "bcgeptsu");

        boolean isAdding = true;

        for (int i = 0; i < change.length(); i++) {
            char currentChar = change.charAt(i);

            // the first character must be either a + or a -
            if (i == 0 && currentChar != '+' && currentChar != '-') {
                return null;
            }

            if (currentChar == '+') {
                isAdding = true;
                continue;
            } else if (currentChar == '-') {
                isAdding = false;
                continue;
            }

            if (!isValidFlag(currentChar)) {
                return null;
            }

            flagsRaw = isAdding ? flagsRaw + currentChar : flagsRaw.replace(String.valueOf(currentChar), "");
        }

        StringBuilder flagsNew = new StringBuilder();

        for (char flag : flagsRaw.toCharArray()) {
            boolean exists = false;

            for (int j = 0; j < flagsNew.length(); j++) {
                if (flagsNew.charAt(j) == flag) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                flagsNew.append(flag);
            }
        }

        char[] flagsNewArray = flagsNew.toString().toCharArray();

        Arrays.sort(flagsOriginal);
        Arrays.sort(flagsNewArray);

        flagsNew = new StringBuilder(new String(flagsNewArray));

        // don't change if there's nothing to change
        if (flagsNew.toString().equals(new String(flagsOriginal))) {
            return null;
        }

        return flagsNew.toString();
    }

    private static boolean isValidFlag(char currentChar){
        if (currentChar != 'b' && currentChar != 'c' && currentChar != 'g' && currentChar != 'e' && currentChar != 'p' && currentChar != 't' && currentChar != 's' && currentChar != 'u') {
            return false;
        }
        return true;
    }

    public static Integer parseDurationToMillis(String input)
    {
        long duration = 0;
        String tmp = "";

        for (Character character : input.toCharArray())
        {
            if (character.toString().matches("[0-9]"))
            {
                tmp += character;
            }

            else
            {
                if (tmp.length() == 0) { return null; }

                if (character == 's') { duration += Integer.parseInt(tmp) * 1000; }
                else if (character == 'm') { duration += Integer.parseInt(tmp) * 60 * 1000; }
                else if (character == 'h') { duration += Integer.parseInt(tmp) * 3600 * 1000; }
                else if (character == 'd') { duration += Integer.parseInt(tmp) * 86400 * 1000; }
                else { return null; }

                tmp = "";
            }
        }

        // default to milliseconds
        if (tmp.length() != 0)
        {
            try { duration += Integer.parseInt(tmp); }
            catch (Exception exception) { return null; }
        }

        return Integer.valueOf(String.valueOf(duration));
    }

    public static String formatPermissions(char[] permissions) {
        if (permissions.length == 0) {
            return "(none)";
        }

        StringBuilder output = new StringBuilder();

        for (char permission : permissions) {
            if (permission == 0) {
                continue;
            }
            output.append(permission);
        }

        return output.toString();
    }

    public static ItemStack stringToItem(String string) {
        try {
            YamlConfiguration yamlConfig = new YamlConfiguration();
            yamlConfig.loadFromString(string);

            return yamlConfig.getItemStack("item");
        } catch (Exception exception) {
            TimingSystem.getPlugin().getLogger().warning("Failed to translate from String to ItemStack: " + string);
            return null;
        }
    }

    public static String itemToString(ItemStack item) {
        YamlConfiguration yamlConfig = new YamlConfiguration();
        yamlConfig.set("item", item);

        return yamlConfig.saveToString();
    }

    public static void sendActionBar(String msg, Player player) {
        player.sendActionBar(Component.text(msg));
    }

    public static void msgConsole(String msg) {
        TimingSystem.getPlugin().logger.info(msg);
    }

    public static String color(String uncolored) {
        return ChatColor.translateAlternateColorCodes('&', uncolored);
    }

    public static String formatAsTime(long time) {
        String toReturn;
        long timeInMillis = getRoundedToTick(time);
        long hours = TimeUnit.MILLISECONDS.toHours(timeInMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % TimeUnit.HOURS.toMinutes(1);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % TimeUnit.MINUTES.toSeconds(1);
        String milis = String.format("%03d", (timeInMillis % 1000));

        if (hours == 0 && minutes == 0) {
            toReturn = String.format("%02d", seconds) + "." + milis;
        } else if (hours == 0) {
            toReturn = String.format("%02d:%02d", minutes, seconds) + "." + milis;
        } else {
            toReturn = String.format("%d:%02d:%02d", hours, minutes, seconds) + "." + milis;
        }
        return toReturn;
    }

    public static String formatAsHeatTimeCountDown(long time) {
        String toReturn;
        long timeInMillis = getRoundedToTick(time);
        long hours = TimeUnit.MILLISECONDS.toHours(timeInMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % TimeUnit.HOURS.toMinutes(1);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % TimeUnit.MINUTES.toSeconds(1);

        if (hours == 0 && minutes == 0) {
            toReturn = String.format("%02d", seconds);
        } else if (hours == 0) {
            toReturn = String.format("%d:%02d", minutes, seconds);
        } else {
            toReturn = String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return toReturn;
    }

    // Used by scoreboard and bossbar
    public static String formatAsRacingGap(long time) {
        String toReturn;
        long timeInMillis = getRoundedToTick(time);
        long hours = TimeUnit.MILLISECONDS.toHours(timeInMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % TimeUnit.HOURS.toMinutes(1);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % TimeUnit.MINUTES.toSeconds(1);
        String milis = String.format("%02d", (timeInMillis % 1000) / 10);

        if (hours == 0 && minutes == 0) {
            toReturn = String.format("%02d", seconds) + "." + milis;
        } else if (hours == 0) {
            toReturn = String.format("%d:%02d", minutes, seconds) + "." + milis;
        } else {
            toReturn = String.format("%d:%02d:%02d", hours, minutes, seconds) + "." + milis;
        }
        return toReturn;
    }

    public static String formatAsQualyGap(long time) {
        String toReturn;
        long timeInMillis = getRoundedToTick(time);
        long hours = TimeUnit.MILLISECONDS.toHours(timeInMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % TimeUnit.HOURS.toMinutes(1);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % TimeUnit.MINUTES.toSeconds(1);
        String milis = String.format("%02d", (timeInMillis % 1000) / 10);

        if (hours == 0 && minutes == 0) {
            toReturn = String.format("%02d", seconds) + "." + milis;
        } else if (hours == 0) {
            toReturn = String.format("%d:%02d", minutes, seconds) + "." + milis;
        } else {
            toReturn = String.format("%d:%02d:%02d", hours, minutes, seconds) + "." + milis;
        }
        return toReturn;
    }
    public static Boat spawnBoat(Location location, Boat.Type type) {
        if (!location.isWorldLoaded()) {
            return null;
        }
        Boat boat = (Boat) location.getWorld().spawnEntity(location, EntityType.BOAT);
        boat.setMetadata("spawned", new FixedMetadataValue(TimingSystem.getPlugin(), null));
        Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> {
            boat.setBoatType(type);
        }, 2);

        return boat;
    }

    public static List<Location> getPositions(Player player) {
        BukkitPlayer bPlayer = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(bPlayer);
        Region selection;
        try {
            selection = session.getSelection(bPlayer.getWorld());
        } catch (IncompleteRegionException e) {
            plugin.sendMessage(player, "messages.error.missing.selection");
            return null;
        }

        if (selection instanceof CuboidRegion) {
            List<Location> locations = new ArrayList<>();
            BlockVector3 p1 = selection.getMinimumPoint();
            locations.add(new Location(player.getWorld(), p1.getBlockX(), p1.getBlockY(), p1.getBlockZ()));
            BlockVector3 p2 = selection.getMaximumPoint();
            locations.add(new Location(player.getWorld(), p2.getBlockX(), p2.getBlockY(), p2.getBlockZ()));
            return locations;
        }  else if (selection instanceof Polygonal2DRegion) {
            return null;
        } else {
            plugin.sendMessage(player, "messages.error.selectionException");
            return null;
        }
    }

    public static Optional<Region> getSelection(Player player) {
        BukkitPlayer bPlayer = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(bPlayer);
        try {
            return Optional.of(session.getSelection(bPlayer.getWorld()));
        } catch (IncompleteRegionException e) {
            return Optional.empty();
        }
    }

    public static Location getLocationFromBlockVector3(World world, BlockVector3 v) {
        return new Location(world, v.getBlockX(), v.getBlockY(), v.getBlockZ());
    }

    public static long getRoundedToTick(long mapTime){
        if ( mapTime % 50 == 0) {
            return mapTime;
        } else {
            long mapTime2 = mapTime + 1;
            if (mapTime2 % 50 == 0) {
                return mapTime2;
            }
            else {
                long rest = mapTime % 50;
                if (rest < 25) {
                    return mapTime - rest;
                } else {
                    mapTime = mapTime + (50-rest);
                }
            }
        }
        return mapTime;
    }

    public static boolean isRegionMatching(TrackRegion trackRegion, Region selection){
        if (trackRegion instanceof TrackCuboidRegion && selection instanceof CuboidRegion) {
            return true;
        } else if (trackRegion instanceof TrackPolyRegion && selection instanceof Polygonal2DRegion) {
            return true;
        }
        return false;
    }

    private static final List<String> rejectedWords = Arrays.asList("random", "r", "cancel", "c", "help", "verbose", "toggleSound");
    public static boolean checkTrackName(String name){
        for(String rejected : rejectedWords){
            if(name.equalsIgnoreCase(rejected)){
                return true;
            }
        }
        return false;
    }

    public static List<Material> getBoatMaterials(){
        return List.of(
                Material.BIRCH_BOAT,
                Material.ACACIA_BOAT,
                Material.DARK_OAK_BOAT,
                Material.JUNGLE_BOAT,
                Material.MANGROVE_BOAT,
                Material.OAK_BOAT,
                Material.SPRUCE_BOAT
        );
    }

    public static Boat.Type getBoatType(Material material){
        switch (material) {
            case ACACIA_BOAT -> {
                return Boat.Type.ACACIA;
            }
            case BIRCH_BOAT -> {
                return Boat.Type.BIRCH;
            }
            case DARK_OAK_BOAT -> {
                return Boat.Type.DARK_OAK;
            }
            case SPRUCE_BOAT -> {
                return Boat.Type.SPRUCE;
            }
            case JUNGLE_BOAT -> {
                return Boat.Type.JUNGLE;
            }
            case MANGROVE_BOAT -> {
                return Boat.Type.MANGROVE;
            }
            default -> {
                return Boat.Type.OAK;
            }
        }
    }
}