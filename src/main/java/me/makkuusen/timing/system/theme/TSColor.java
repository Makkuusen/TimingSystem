package me.makkuusen.timing.system.theme;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;

public enum TSColor {
    PRIMARY,
    SECONDARY,
    ERROR,
    WARNING,
    SUCCESS,
    BROADCAST,
    AWARD,
    TITLE,
    BUTTON,
    BUTTON_ADD,
    BUTTON_REMOVE,
    AWARD_SECONDARY;


    public static ContextResolver<TSColor, BukkitCommandExecutionContext> getColorContextResolver() {
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
}