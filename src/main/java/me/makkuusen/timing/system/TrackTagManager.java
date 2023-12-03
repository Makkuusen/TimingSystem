package me.makkuusen.timing.system;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.idb.DB;
import me.makkuusen.timing.system.database.Database;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackTag;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackTagManager {

    private static final Map<String, TrackTag> trackTags = new HashMap<>();

    public static boolean createTrackTag(String value) {
        TextColor color = NamedTextColor.WHITE;
        ItemStack item = new ItemBuilder(Material.ANVIL).build();
        int weight = 100;
        var tag = new TrackTag(value, color, item, weight);
        if (trackTags.containsKey(tag.getValue())) {
            return false;
        }

        DB.executeUpdateAsync("INSERT INTO `ts_tags` (`tag`, `color`, `item`) VALUES('" + tag.getValue() + "', '" + color.asHexString() + "', " + Database.sqlString(ApiUtilities.itemToString(item)) + ");");
        trackTags.put(tag.getValue(), tag);
        return true;
    }

    public static void addTag(TrackTag tag) {
        if (trackTags.containsKey(tag.getValue())) {
            return;
        }
        trackTags.put(tag.getValue(), tag);
    }

    public static boolean deleteTag(TrackTag tag) {
        if (trackTags.containsKey(tag)) {
            for (Track t : TrackDatabase.getTracks()) {
                if (t.hasTag(tag)) {
                    t.removeTag(tag);
                }
            }
            trackTags.remove(tag);
            DB.executeUpdateAsync("DELETE FROM `ts_tags` WHERE `tag` = '" + tag.getValue() + "';");
            DB.executeUpdateAsync("DELETE FROM `ts_tracks_tags` WHERE `tag` = '" + tag.getValue() + "';");
            return true;
        }
        return false;
    }

    public static boolean hasTag(TrackTag tag) {
        return trackTags.containsKey(tag);
    }

    public static Map<String, TrackTag> getTrackTags() {
        return trackTags;
    }

    public static TrackTag getTrackTag(String value) {
        return trackTags.get(value);
    }

    public static ContextResolver<TrackTag, BukkitCommandExecutionContext> getTrackTagContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return TrackTagManager.getTrackTag(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    public static List<TrackTag> getSortedTrackTags(boolean includeZeroWeight) {
        if (includeZeroWeight) {
            return trackTags.values().stream().sorted(Comparator.comparingInt(TrackTag::getWeight).reversed()).toList();
        }
        return trackTags.values().stream().filter(tag -> tag.getWeight() > 0).sorted(Comparator.comparingInt(TrackTag::getWeight).reversed()).toList();
    }
}
