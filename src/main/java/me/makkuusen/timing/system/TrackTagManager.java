package me.makkuusen.timing.system;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.idb.DB;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackDatabase;
import me.makkuusen.timing.system.track.TrackTag;

import java.util.ArrayList;
import java.util.List;

public class TrackTagManager {

    private static final List<TrackTag> trackTags = new ArrayList<>();

    public static boolean createTrackTag(String value) {

        var tag = new TrackTag(value);
        if (trackTags.contains(tag)) {
            return false;
        }

        DB.executeUpdateAsync("INSERT INTO `ts_tags` (`tag`) VALUES('" + tag.getValue() + "');");
        trackTags.add(tag);
        return true;
    }

    public static void addTag(TrackTag tag) {
        if (trackTags.contains(tag)) {
            return;
        }
        trackTags.add(tag);
    }

    public static boolean deleteTag(TrackTag tag) {
        if (trackTags.contains(tag)) {
            for (Track t : TrackDatabase.getTracks()) {
                if (t.hasTag(tag)) {
                    t.removeTag(tag);
                }
            }
            trackTags.remove(tag);
            DB.executeUpdateAsync("DELETE FROM `ts_tags` WHERE `tag` = " + tag.getValue() + ";");
            return true;
        }
        return false;
    }

    public static boolean hasTag(TrackTag tag) {
        return trackTags.contains(tag);
    }

    public static List<TrackTag> getTrackTags() {
        return trackTags.stream().toList();
    }

    public static ContextResolver<TrackTag, BukkitCommandExecutionContext> getTrackTagContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return new TrackTag(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    public static TrackTag getNext(TrackTag tag) {
        var tags = getTrackTags();
        if (tags.size() == 0) {
            return null;
        }
        if (tag == null) {
            return tags.get(0);
        }

        boolean match = false;
        for (TrackTag trackTag : tags) {
            if (match) {
                return trackTag;
            }
            if (trackTag.equals(tag)) {
                match = true;
            }
        }
        return null;
    }
}
