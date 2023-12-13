package me.makkuusen.timing.system;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.contexts.ContextResolver;
import me.makkuusen.timing.system.boatutils.BoatUtilsManager;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.database.EventDatabase;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.event.Event;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.round.Round;
import me.makkuusen.timing.system.round.RoundType;
import me.makkuusen.timing.system.theme.TSColor;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.options.TrackOption;
import me.makkuusen.timing.system.track.regions.TrackRegion;
import me.makkuusen.timing.system.track.tags.TrackTag;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Boat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContextResolvers {

    static void loadCommandContextsAndCompletions(PaperCommandManager manager) {

        manager.getCommandContexts().registerContext(
                Event.class, ContextResolvers.getEventContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("event", context ->
                EventDatabase.getEventsAsStrings()
        );
        manager.getCommandContexts().registerContext(
                Round.class, ContextResolvers.getRoundContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("round", context ->
                EventDatabase.getRoundsAsStrings(context.getPlayer().getUniqueId())
        );
        manager.getCommandContexts().registerContext(
                Heat.class, ContextResolvers.getHeatContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("heat", context ->
                EventDatabase.getHeatsAsStrings(context.getPlayer().getUniqueId())
        );
        manager.getCommandContexts().registerContext(
                Track.class, ContextResolvers.getTrackContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("track", (context -> TrackDatabase.getTracksAsStrings(context.getPlayer())));

        manager.getCommandContexts().registerContext(
                TrackRegion.class, ContextResolvers.getRegionContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("region", TrackDatabase::getRegionsAsStrings
        );
        manager.getCommandContexts().registerContext(
                Track.TrackType.class, ContextResolvers.getTrackTypeContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("trackType", context -> {
            List<String> res = new ArrayList<>();
            for (Track.TrackType type : Track.TrackType.values()) {
                res.add(type.name().toLowerCase());
            }
            return res;
        });
        manager.getCommandContexts().registerContext(
                TrackTag.class, ContextResolvers.getTrackTagContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("trackTag", context -> {
            List<String> res = new ArrayList<>();
            for (String tag : TrackTagManager.getTrackTags().keySet()) {
                res.add(tag.toLowerCase());
            }
            return res;
        });
        manager.getCommandContexts().registerContext(
                TrackOption.class, ContextResolvers.getTrackOptionContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("trackOption", context -> {
            List<String> res = new ArrayList<>();
            for (TrackOption option : TrackOption.values()) {
                res.add(option.toString().toLowerCase());
            }
            return res;
        });
        manager.getCommandContexts().registerContext(
                RoundType.class, ContextResolvers.getRoundTypeContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("roundType", context -> {
            List<String> res = new ArrayList<>();
            for (RoundType type : RoundType.values()) {
                res.add(type.name().toLowerCase());
            }
            return res;
        });
        manager.getCommandContexts().registerContext(
                Track.TrackMode.class, ContextResolvers.getTrackModeContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("trackMode", context -> {
            List<String> res = new ArrayList<>();
            for (Track.TrackMode mode : Track.TrackMode.values()) {
                res.add(mode.name().toLowerCase());
            }
            return res;
        });
        manager.getCommandContexts().registerContext(
                Boat.Type.class, ContextResolvers.getBoatContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("boat", context -> {
            List<String> res = new ArrayList<>();
            for (Boat.Type tree : Boat.Type.values()) {
                res.add(tree.name().toLowerCase());
            }
            return res;
        });
        manager.getCommandContexts().registerContext(
                TSColor.class, ContextResolvers.getTSColorContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("tsColor", context -> {
            List<String> res = new ArrayList<>();
            for (TSColor color : TSColor.values()) {
                res.add(color.name().toLowerCase());
            }
            return res;
        });
        manager.getCommandContexts().registerContext(
                NamedTextColor.class, ContextResolvers.getNamedColorContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("namedColor", context -> {
            List<String> res = new ArrayList<>();
            for (NamedTextColor color : NamedTextColor.NAMES.values()) {
                res.add(color.toString());
            }
            return res;
        });

        manager.getCommandContexts().registerContext(BoatUtilsMode.class, ContextResolvers.getBoatUtilsModeContextResolver());
        manager.getCommandCompletions().registerAsyncCompletion("allBoatUtilsMode", context -> {
            List<String> res = new ArrayList<>();
            Arrays.stream(BoatUtilsMode.values()).forEach(mode -> res.add(mode.name().toLowerCase()));
            return res;
        });
        manager.getCommandCompletions().registerAsyncCompletion("boatUtilsMode", context -> {
            TPlayer tPlayer = TSDatabase.getPlayer(context.getPlayer().getUniqueId());
            List<String> res = new ArrayList<>();

            if(tPlayer.hasBoatUtils()) {
                List<BoatUtilsMode> availableModes = BoatUtilsManager.getAvailableModes(tPlayer.getBoatUtilsVersion());
                availableModes.forEach(mode -> res.add(mode.name().toLowerCase()));
            } else {
                res.add(BoatUtilsMode.BROKEN_SLIME_BA_NOFD.name().toLowerCase());
                res.add(BoatUtilsMode.BROKEN_SLIME_RALLY.name().toLowerCase());
            }
            return res;
        });
    }
    public static ContextResolver<Track.TrackType, BukkitCommandExecutionContext> getTrackTypeContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return Track.TrackType.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    public static ContextResolver<Track.TrackMode, BukkitCommandExecutionContext> getTrackModeContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return Track.TrackMode.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    public static ContextResolver<Track, BukkitCommandExecutionContext> getTrackContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            var maybeTrack = TrackDatabase.getTrack(name);
            if (maybeTrack.isPresent()) {
                return maybeTrack.get();
            } else {
                // User didn't type an Event, show error!
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    public static ContextResolver<TrackRegion, BukkitCommandExecutionContext> getRegionContextResolver() {
        return (c) -> {
            String region = c.popFirstArg();
            var maybeTrack = TimingSystem.playerEditingSession.get(c.getPlayer().getUniqueId());
            if (maybeTrack != null) {
                try {
                    String[] regionName = region.split("-");
                    int index = Integer.parseInt(regionName[1]);
                    String regionType = regionName[0];
                    var maybeRegion = maybeTrack.getTrackRegions().getRegion(TrackRegion.RegionType.valueOf(regionType.toUpperCase()), index);
                    if (maybeRegion.isPresent()) {
                        return maybeRegion.get();
                    }
                } catch (Exception ignored) {

                }
            }
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        };
    }

    public static ContextResolver<Event, BukkitCommandExecutionContext> getEventContextResolver() {
        return (c) -> {
            String first = c.popFirstArg();
            var maybeEvent = EventDatabase.getEvent(first);
            if (maybeEvent.isPresent()) {
                return maybeEvent.get();
            } else {
                // User didn't type an Event, show error!
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    public static ContextResolver<Round, BukkitCommandExecutionContext> getRoundContextResolver() {
        return (c) -> {
            String roundName = c.popFirstArg();
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(c.getPlayer().getUniqueId());
            if (maybeEvent.isPresent()) {
                Event event = maybeEvent.get();
                var maybeRound = event.getEventSchedule().getRound(roundName);
                if (maybeRound.isPresent()) {
                    return maybeRound.get();
                }
            }
            // User didn't type a heat, show error!
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        };
    }

    public static ContextResolver<Heat, BukkitCommandExecutionContext> getHeatContextResolver() {
        return (c) -> {
            String heatName = c.popFirstArg();
            var maybeEvent = EventDatabase.getPlayerSelectedEvent(c.getPlayer().getUniqueId());
            if (maybeEvent.isPresent()) {
                Event event = maybeEvent.get();
                var maybeHeat = event.getEventSchedule().getHeat(heatName);
                if (maybeHeat.isPresent()) {
                    return maybeHeat.get();
                }
            }
            // User didn't type a heat, show error!
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        };
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

    public static ContextResolver<TrackOption, BukkitCommandExecutionContext> getTrackOptionContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return TrackOption.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    public static ContextResolver<RoundType, BukkitCommandExecutionContext> getRoundTypeContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return RoundType.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    public static ContextResolver<Boat.Type, BukkitCommandExecutionContext> getBoatContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return Boat.Type.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                //no matching boat types
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    public static ContextResolver<TSColor, BukkitCommandExecutionContext> getTSColorContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return TSColor.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                //no matching boat types
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    public static ContextResolver<NamedTextColor, BukkitCommandExecutionContext> getNamedColorContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return NamedTextColor.NAMES.value(name);
            } catch (IllegalArgumentException e) {
                //no matching boat types
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }

    public static ContextResolver<BoatUtilsMode, BukkitCommandExecutionContext> getBoatUtilsModeContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return BoatUtilsMode.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                //no matching boat types
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }
}
