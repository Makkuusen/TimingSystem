package me.makkuusen.timing.system.text;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;

public enum TimingSystemColor {
    DARK,
    HIGHLIGHT,
    ERROR,
    WARNING,
    SUCCESS,
    BROADCAST,
    AWARD_HIGHLIGHT,
    AWARD_DARK;

    public static ContextResolver<TimingSystemColor, BukkitCommandExecutionContext> getColorContextResolver() {
        return (c) -> {
            String name = c.popFirstArg();
            try {
                return TimingSystemColor.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                //no matching boat types
                throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
        };
    }
}
