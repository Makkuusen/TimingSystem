package me.makkuusen.timing.system;

import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.tags.TrackTag;
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

        TimingSystem.getTrackDatabase().createTagAsync(tag, color, item);
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
            for (Track t : TrackDatabase.tracks) {
                if (t.getTrackTags().hasTag(tag)) {
                    t.getTrackTags().remove(tag);
                }
            }
            trackTags.remove(tag);
            TimingSystem.getTrackDatabase().deleteTagAsync(tag);
            return true;
        }
        return false;
    }

    public static boolean hasTag(TrackTag tag) {
        return trackTags.containsKey(tag.getValue());
    }

    public static Map<String, TrackTag> getTrackTags() {
        return trackTags;
    }

    public static TrackTag getTrackTag(String value) {
        return trackTags.get(value);
    }

    public static List<TrackTag> getSortedTrackTags(boolean includeZeroWeight) {
        if (includeZeroWeight) {
            return trackTags.values().stream().sorted(Comparator.comparingInt(TrackTag::getWeight).reversed()).toList();
        }
        return trackTags.values().stream().filter(tag -> tag.getWeight() > 0).sorted(Comparator.comparingInt(TrackTag::getWeight).reversed()).toList();
    }
}
