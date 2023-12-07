package me.makkuusen.timing.system.track;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import lombok.Getter;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;

import java.util.Arrays;
import java.util.NoSuchElementException;

@Getter
public enum TrackOption {

    NO_CREATIVE(0),
    NO_POTION_EFFECTS(1),
    NO_ELYTRA(2),
    FORCE_ELYTRA(3),
    NO_SOUL_SPEED(4),
    FORCE_BOAT(5),
    NO_RIPTIDE(6),
    RESET_TO_LATEST_CHECKPOINT(7);

    private final int id;
    TrackOption(int id) {
        this.id = id;
    }

    public static TrackOption fromID(int id) throws NoSuchElementException {
        return Arrays.stream(TrackOption.values()).filter(option -> option.getId() == id).findFirst().orElseThrow();
    }

    public static ContextResolver<TrackOption, BukkitCommandExecutionContext> getTrackTagContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return TrackOption.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }
}
