package me.makkuusen.timing.system.theme;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.contexts.ContextResolver;
import net.kyori.adventure.text.format.NamedTextColor;

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

    public String getKey() {
        return "color." + this.name().toLowerCase();
    }
}
